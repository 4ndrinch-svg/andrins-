package ch.andrin.packs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Info-HUD, Rüstungs-Warnung und Todespunkt. */
public final class InfoHud {
	public static boolean hudEnabled = true;

	/** Todespunkt: {x, y, z} + Dimension */
	public static BlockPos deathPos;
	public static String deathDim;
	private static boolean wasDead;
	private static final boolean[] warned = new boolean[6];

	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
	private static final String[] SLOT_NAMES = {"Helm", "Brust", "Hose", "Schuhe", "Hand", "Zweithand"};

	private InfoHud() {}

	private static int percent(ItemStack s) {
		if (s.isEmpty() || !s.isDamageableItem() || s.getMaxDamage() <= 0) return -1;
		return 100 - (s.getDamageValue() * 100 / s.getMaxDamage());
	}

	public static void tick(Minecraft mc) {
		Player p = mc.player;
		if (p == null || mc.level == null) return;

		// Todespunkt merken
		boolean dead = p.isDeadOrDying();
		if (dead && !wasDead) {
			deathPos = p.blockPosition();
			deathDim = mc.level.dimension().toString();
		}
		wasDead = dead;
		if (deathPos != null && !dead && mc.level.dimension().toString().equals(deathDim)
				&& p.blockPosition().distSqr(deathPos) < 9) {
			deathPos = null;   // angekommen -> Markierung weg
		}

		// Rüstungs-Warnung: einmal pro Teil, wenn unter 10 %
		for (int i = 0; i < SLOTS.length; i++) {
			int pc = percent(p.getItemBySlot(SLOTS[i]));
			if (pc >= 0 && pc <= 10) {
				if (!warned[i]) {
					warned[i] = true;
					mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.0F));
					p.sendOverlayMessage(Component.literal("§c⚠ " + SLOT_NAMES[i] + " fast kaputt! §7(" + pc + " %)"));
				}
			} else {
				warned[i] = false;
			}
		}
	}

	/** Wird als HUD-Element gezeichnet. */
	public static void draw(net.minecraft.client.gui.GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		Player p = mc.player;
		if (!hudEnabled || p == null || mc.level == null) return;
		var font = mc.font;
		int y = 4;
		String[] lines = {
				"§dXYZ §f" + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ(),
				"§dRichtung §f" + richtung(p.getDirection().getName()),
				"§dFPS §f" + mc.getFps(),
				"§dBiom §f" + mc.level.getBiome(p.blockPosition()).getRegisteredName().replace("minecraft:", "")
		};
		for (String l : lines) {
			g.fill(2, y - 2, 6 + font.width(l), y + 9, 0x80000000);
			g.text(font, l, 4, y, 0xFFFFFFFF, true);
			y += 11;
		}
		if (deathPos != null) {
			String l = "§c☠ Tod §f" + deathPos.getX() + " " + deathPos.getY() + " " + deathPos.getZ()
					+ (mc.level.dimension().toString().equals(deathDim) ? " §7(" + (int) Math.sqrt(p.blockPosition().distSqr(deathPos)) + " m)" : " §7(andere Dimension)");
			g.fill(2, y - 2, 6 + font.width(l), y + 9, 0x80000000);
			g.text(font, l, 4, y, 0xFFFFFFFF, true);
			y += 11;
		}
		// Haltbarkeit
		y += 3;
		for (int i = 0; i < SLOTS.length; i++) {
			int pc = percent(p.getItemBySlot(SLOTS[i]));
			if (pc < 0) continue;
			String col = pc <= 10 ? "§c" : pc <= 30 ? "§6" : "§a";
			String l = "§7" + SLOT_NAMES[i] + " " + col + pc + " %";
			g.fill(2, y - 2, 6 + font.width(l), y + 9, 0x80000000);
			g.text(font, l, 4, y, 0xFFFFFFFF, true);
			y += 11;
		}
	}

	private static String richtung(String d) {
		return switch (d) {
			case "north" -> "Norden (-Z)";
			case "south" -> "Süden (+Z)";
			case "east" -> "Osten (+X)";
			case "west" -> "Westen (-X)";
			default -> d;
		};
	}
}
