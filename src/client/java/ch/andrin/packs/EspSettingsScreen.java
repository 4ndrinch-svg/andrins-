package ch.andrin.packs;

import java.util.Map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Liste: was die G-Sicht anzeigen soll. */
public class EspSettingsScreen extends Screen {
	private final Screen parent;

	public EspSettingsScreen(Screen parent) {
		super(Component.literal("Sicht-Einstellungen"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int i = 0;
		for (Map.Entry<String, Object[]> e : EspSettings.OPTIONS.entrySet()) {
			String key = e.getKey();
			int col = i % 2, row = i / 2;
			this.addRenderableWidget(Button.builder(label(key), b -> {
				EspSettings.toggle(key);
				b.setMessage(label(key));
			}).bounds(cx - 155 + col * 160, 34 + row * 24, 150, 20).build());
			i++;
		}
		this.addRenderableWidget(Button.builder(Component.literal("§lFertig"), b -> this.onClose())
				.bounds(cx - 100, this.height - 28, 200, 20).build());
	}

	private static Component label(String key) {
		Object[] o = EspSettings.OPTIONS.get(key);
		return Component.literal(((Boolean) o[1] ? "§a● " : "§c○ §7") + o[0]);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		g.centeredText(this.font, Component.literal("§d§lSicht-Einstellungen §7(Taste G)"), this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(parent);
	}
}
