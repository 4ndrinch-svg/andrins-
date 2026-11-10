package ch.andrin.packs;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

/** Schaltet das Fullbright-Pack per Taste an/aus. */
public final class Fullbright {
	private Fullbright() {}

	public static void toggle(Minecraft mc) {
		PackRepository repo = mc.getResourcePackRepository();
		String id = PackMemory.findId(repo, "fullbright");
		if (id == null) return;
		List<String> sel = new ArrayList<>(repo.getSelectedIds());
		boolean on = !sel.contains(id);
		if (on) sel.add(id); else sel.remove(id);
		repo.setSelected(sel);
		mc.options.updateResourcePacks(repo);
		PackMemory.save(mc);
		if (mc.player != null) mc.player.sendOverlayMessage(Component.literal(on ? "§eFullbright §aAN" : "§eFullbright §cAUS"));
	}
}
