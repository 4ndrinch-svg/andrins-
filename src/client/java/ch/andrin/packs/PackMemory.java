package ch.andrin.packs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Merkt sich, welche Andrin-Packs aktiv sind (config/andrinspacks.txt),
 * und stellt sie beim Spielstart wieder her – auch wenn Minecraft die Auswahl zurückgesetzt hat.
 */
public final class PackMemory {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("andrinspacks.txt");

	private PackMemory() {}

	static String findId(PackRepository repo, String folder) {
		String suffix = AndrinsPacksClient.MOD_ID + ":" + folder;
		for (Pack pack : repo.getAvailablePacks()) {
			String id = pack.getId();
			if (id.equals(suffix) || id.endsWith(suffix) || id.endsWith("/" + folder)) return id;
		}
		return null;
	}

	/** Speichert die aktuell aktiven Andrin-Packs (Ordnernamen, Reihenfolge wie im Spiel). */
	public static void save(Minecraft mc) {
		PackRepository repo = mc.getResourcePackRepository();
		List<String> lines = new ArrayList<>();
		for (String id : repo.getSelectedIds()) {
			for (String[] p : AndrinsPacksClient.PACKS) {
				if (id.equals(findId(repo, p[0]))) lines.add(p[0]);
			}
		}
		try {
			Files.createDirectories(FILE.getParent());
			Files.write(FILE, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("[AndrinsPacks] Konnte Auswahl nicht speichern: " + e);
		}
	}

	/** Stellt die gespeicherten Packs wieder her; lädt nur neu, wenn etwas fehlt. */
	public static void restore(Minecraft mc) {
		if (!Files.exists(FILE)) {
			save(mc);
			return;
		}
		List<String> saved;
		try {
			saved = Files.readAllLines(FILE, StandardCharsets.UTF_8);
		} catch (IOException e) {
			return;
		}
		PackRepository repo = mc.getResourcePackRepository();
		Set<String> selected = new LinkedHashSet<>(repo.getSelectedIds());
		Set<String> ours = new LinkedHashSet<>();
		for (String[] p : AndrinsPacksClient.PACKS) {
			String id = findId(repo, p[0]);
			if (id != null) ours.add(id);
		}
		List<String> wanted = new ArrayList<>();
		for (String id : selected) if (!ours.contains(id)) wanted.add(id);   // fremde Packs behalten
		for (String folder : saved) {
			String id = findId(repo, folder.trim());
			if (id != null && !wanted.contains(id)) wanted.add(id);
		}
		if (!new LinkedHashSet<>(wanted).equals(selected)) {
			repo.setSelected(wanted);
			mc.options.updateResourcePacks(repo);
		}
	}
}
