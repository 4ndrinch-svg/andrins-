package ch.andrin.packs;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Spieler durch Wände sehen (nur für private Welten gedacht). Standard: aus. */
public final class TrollVision {
	public static boolean enabled = false;

	private TrollVision() {}

	public static boolean shouldGlow(Entity entity) {
		if (!enabled || !(entity instanceof Player)) return false;
		Minecraft mc = Minecraft.getInstance();
		return entity != mc.player;
	}
}
