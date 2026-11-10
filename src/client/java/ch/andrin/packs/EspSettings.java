package ch.andrin.packs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;

/** Was die G-Sicht anzeigt (config/andrinspacks_sicht.txt). */
public final class EspSettings {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("andrinspacks_sicht.txt");
	/** Schlüssel -> {Anzeigename, an/aus} */
	public static final Map<String, Object[]> OPTIONS = new LinkedHashMap<>();

	static {
		put("players", "Spieler", true);
		put("chests", "Kisten", true);
		put("trapped", "Fallenkisten", true);
		put("ender", "Endertruhen", true);
		put("shulker", "Shulkerkisten", true);
		put("barrels", "Fässer", true);
		put("hoppers", "Trichter", true);
		put("dispensers", "Werfer & Spender", true);
		put("pistons", "Kolben", true);
		put("bases", "Base-Finder", true);
		put("fill", "Gefüllte Boxen", true);
		put("tracers", "Linien zum Ziel", true);
		load();
	}

	private EspSettings() {}

	private static void put(String key, String name, boolean def) {
		OPTIONS.put(key, new Object[] {name, def});
	}

	public static boolean on(String key) {
		Object[] o = OPTIONS.get(key);
		return o != null && (Boolean) o[1];
	}

	public static void toggle(String key) {
		Object[] o = OPTIONS.get(key);
		if (o != null) o[1] = !(Boolean) o[1];
		save();
	}

	private static void load() {
		if (!Files.exists(FILE)) return;
		try {
			for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
				String[] kv = line.split("=", 2);
				if (kv.length == 2 && OPTIONS.containsKey(kv[0].trim())) OPTIONS.get(kv[0].trim())[1] = Boolean.parseBoolean(kv[1].trim());
			}
		} catch (IOException ignored) {
		}
	}

	private static void save() {
		List<String> lines = new ArrayList<>();
		OPTIONS.forEach((k, v) -> lines.add(k + "=" + v[1]));
		try {
			Files.createDirectories(FILE.getParent());
			Files.write(FILE, lines, StandardCharsets.UTF_8);
		} catch (IOException ignored) {
		}
	}
}
