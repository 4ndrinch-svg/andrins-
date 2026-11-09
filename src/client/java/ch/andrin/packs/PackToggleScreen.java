package ch.andrin.packs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Menü mit einem An/Aus-Schalter pro Pack. Änderungen werden beim Schliessen
 * (Fertig oder ESC) übernommen und die Ressourcen einmal neu geladen.
 */
public class PackToggleScreen extends Screen {
	private static final int PER_PAGE = 20; // 2 Spalten × 10 Zeilen
	private static final int BTN_W = 190;
	private static final int BTN_H = 20;

	private final Screen parent;
	private final Set<String> wanted = new LinkedHashSet<>();
	private Set<String> original;
	private int page = 0;

	public PackToggleScreen(Screen parent) {
		super(Component.literal("Andrins Packs"));
		this.parent = parent;
	}

	/** Findet die echte Pack-ID im Repository (Fabric hängt evtl. Präfixe an). */
	private String findId(PackRepository repo, String folder) {
		for (Pack pack : repo.getAvailablePacks()) {
			String id = pack.getId();
			if (id.equals(PackIds.fabric(folder)) || id.endsWith(AndrinsPacksClient.MOD_ID + ":" + folder) || id.endsWith("/" + folder)) {
				return id;
			}
		}
		return null;
	}

	@Override
	protected void init() {
		PackRepository repo = this.minecraft.getResourcePackRepository();
		if (original == null) {
			original = new LinkedHashSet<>(repo.getSelectedIds());
			wanted.addAll(original);
		}

		int pages = (AndrinsPacksClient.PACKS.length + PER_PAGE - 1) / PER_PAGE;
		page = Math.max(0, Math.min(page, pages - 1));
		int left = this.width / 2 - BTN_W - 5;
		int top = 32;

		// Titelzeile (deaktivierter Button als Beschriftung)
		Button title = Button.builder(Component.literal("§d§lAndrins Packs §7– Seite " + (page + 1) + "/" + pages), b -> {})
				.bounds(this.width / 2 - 100, 6, 200, BTN_H).build();
		title.active = false;
		this.addRenderableWidget(title);

		// Taste ändern: öffnet die Minecraft-Tastenbelegung
		this.addRenderableWidget(Button.builder(
				Component.literal("§eTaste: §f").append(AndrinsPacksClient.openMenuKey.getTranslatedKeyMessage()).append(Component.literal(" §7(ändern)")),
				b -> this.minecraft.gui.setScreen(new KeyBindsScreen(this, this.minecraft.options)))
				.bounds(this.width - 150, 6, 145, BTN_H).build());

		int start = page * PER_PAGE;
		for (int i = start; i < Math.min(start + PER_PAGE, AndrinsPacksClient.PACKS.length); i++) {
			String[] p = AndrinsPacksClient.PACKS[i];
			String id = findId(repo, p[0]);
			int slot = i - start;
			int x = left + (slot / 10) * (BTN_W + 10);
			int y = top + (slot % 10) * (BTN_H + 2);
			Button btn = Button.builder(label(p[1], id), b -> {
				if (id == null) return;
				if (wanted.contains(id)) wanted.remove(id); else wanted.add(id);
				b.setMessage(label(p[1], id));
			}).bounds(x, y, BTN_W, BTN_H).build();
			if (id == null) btn.active = false;
			this.addRenderableWidget(btn);
		}

		int bottom = this.height - 28;
		int cx = this.width / 2;
		this.addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
				.bounds(cx - 205, bottom, 30, BTN_H).build()).active = page > 0;
		this.addRenderableWidget(Button.builder(Component.literal("§aAlle an"), b -> setAll(repo, true))
				.bounds(cx - 170, bottom, 80, BTN_H).build());
		this.addRenderableWidget(Button.builder(Component.literal("§cAlle aus"), b -> setAll(repo, false))
				.bounds(cx - 85, bottom, 80, BTN_H).build());
		this.addRenderableWidget(Button.builder(Component.literal("§lFertig"), b -> this.onClose())
				.bounds(cx + 0, bottom, 170, BTN_H).build());
		this.addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
				.bounds(cx + 175, bottom, 30, BTN_H).build()).active = page < pages - 1;
	}

	private Component label(String name, String id) {
		if (id == null) return Component.literal("§8" + name + " (fehlt)");
		return Component.literal((wanted.contains(id) ? "§a■ AN  §f" : "§c□ AUS §7") + name);
	}

	private void setAll(PackRepository repo, boolean on) {
		for (String[] p : AndrinsPacksClient.PACKS) {
			String id = findId(repo, p[0]);
			if (id == null) continue;
			if (on) wanted.add(id); else wanted.remove(id);
		}
		rebuildWidgets();
	}

	@Override
	public void onClose() {
		if (original != null && !wanted.equals(original)) {
			PackRepository repo = this.minecraft.getResourcePackRepository();
			List<String> order = new ArrayList<>();
			for (String id : original) if (wanted.contains(id)) order.add(id);   // alte Reihenfolge behalten
			for (String id : wanted) if (!order.contains(id)) order.add(id);     // neue oben drauf
			repo.setSelected(order);
			this.minecraft.options.updateResourcePacks(repo);                     // speichert + lädt neu
		}
		this.minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
