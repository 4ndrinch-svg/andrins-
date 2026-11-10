package ch.andrin.packs;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Sucht Chunks mit vielen typischen Base-Blöcken (nur für private Welten).
 * Läuft mit Taste G zusammen mit der Spieler-/Kisten-Sicht.
 */
public final class BaseFinder {
	/** {chunkX, chunkZ, score, avgY} */
	static final List<int[]> bases = new ArrayList<>();
	private static int cooldown = 0;
	private static final int MIN_SCORE = 8;

	private BaseFinder() {}

	/** Punkte pro Block-Entity-Typ (Name der Typ-ID). Natürliche Strukturen haben kaum Betten/Öfen/Schilder. */
	private static int points(BlockEntity be) {
		String id = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
		if (id.contains("bed") || id.contains("beacon") || id.contains("enchanting") || id.contains("shulker")) return 4;
		if (id.contains("furnace") || id.contains("smoker") || id.contains("brewing") || id.contains("ender_chest")
				|| id.contains("sign") || id.contains("banner") || id.contains("lectern") || id.contains("jukebox")
				|| id.contains("crafter") || id.contains("campfire")) return 2;
		if (id.contains("chest") || id.contains("barrel") || id.contains("hopper") || id.contains("dropper")
				|| id.contains("dispenser") || id.contains("decorated_pot") || id.contains("chiseled_bookshelf")) return 1;
		return 0;   // Spawner, Bienen usw. zählen nicht
	}

	public static void tick(Minecraft mc) {
		if (!TrollVision.enabled || mc.level == null || mc.player == null) return;
		if (cooldown-- > 0) return;
		cooldown = 60;
		List<int[]> found = new ArrayList<>();
		int r = mc.options.getEffectiveRenderDistance() + 1;
		int pcx = mc.player.blockPosition().getX() >> 4, pcz = mc.player.blockPosition().getZ() >> 4;
		for (int cx = pcx - r; cx <= pcx + r; cx++) {
			for (int cz = pcz - r; cz <= pcz + r; cz++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				int score = 0, sumY = 0, n = 0;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					int p = points(be);
					if (p > 0) {
						score += p;
						sumY += be.getBlockPos().getY();
						n++;
					}
				}
				if (score >= MIN_SCORE) found.add(new int[] {cx, cz, score, sumY / n});
			}
		}
		int[] nearest = null;
		double best = Double.MAX_VALUE;
		for (int[] b : found) {
			double dx = b[0] * 16 + 8 - mc.player.getX(), dz = b[1] * 16 + 8 - mc.player.getZ();
			double d = dx * dx + dz * dz;
			if (d > 32 * 32 && d < best) { best = d; nearest = b; }
		}
		boolean isNew;
		synchronized (bases) {
			isNew = found.size() > bases.size();
			bases.clear();
			bases.addAll(found);
		}
		if (nearest != null && isNew) {
			mc.player.sendOverlayMessage(Component.literal("§c⚑ Base gefunden §7– ca. §f" + (int) Math.sqrt(best)
					+ " §7Blöcke, X §f" + (nearest[0] * 16 + 8) + " §7Z §f" + (nearest[1] * 16 + 8) + " §7(Stärke " + nearest[2] + ")"));
		}
	}
}
