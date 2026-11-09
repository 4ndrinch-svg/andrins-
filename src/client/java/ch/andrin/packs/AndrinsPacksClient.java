package ch.andrin.packs;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Registriert alle Packs als einzeln schaltbare eingebaute Ressourcenpakete
 * und öffnet mit Taste K das Andrin-Pack-Menü.
 */
public class AndrinsPacksClient implements ClientModInitializer {
	public static final String MOD_ID = "andrinspacks";

	/** {ordner, anzeigename} */
	public static final String[][] PACKS = {
			{"deepslate_to_netherite", "Deepslate to Netherite"},
			{"low_mace", "Low Mace"},
			{"blue_netherite", "Blue Netherite"},
			{"dramatic_skys", "Dramatic Skys"},
			{"3d_redstone_dot_to_paw", "3D Redstone (Dot-to-Paw)"},
			{"cubic_sun_moon", "Cubic Sun & Moon"},
			{"black_steel_shield", "Black Steel Shield"},
			{"cobweb_outline", "Cobweb Outline"},
			{"clear_water", "Clear Water"},
			{"clear_lava_water_claw", "Clear Lava & Water (CLaW)"},
			{"brighter_dark_blocks", "Brighter Dark Blocks"},
			{"animated_purple_xp_bar", "Animated Purple XP Bar"},
			{"crystalcore_totem", "CrystalCore Totem"},
			{"brighter_nether_bricks", "Brighter Nether Bricks"},
			{"3d_redstone_dust", "3D Redstone Dust"},
			{"brewing_guide", "Brewing Guide"},
			{"enchantment_outlines", "Enchantment Outlines"},
			{"fullbright", "Fullbright"},
			{"low_fire_extreme", "Low Fire EXTREME"},
			{"low_shield", "Low Shield"},
			{"anti_leak_netherite", "Anti-Leak Netherite (Stein, Deepslate, Bedrock, Erze)"},
			{"new_glowing_ores", "New Glowing Ores"},
			{"no_totem_pop_particles", "No Totem Pop Particles"},
			{"purplepearls", "PurplePearls"},
			{"purple_block_selection_outline", "Purple Block Selection Outline"},
			{"purple_hearts", "Purple Hearts"},
			{"purple_netherite", "Purple Netherite"},
			{"realistic_animals", "Realistic Animals"},
			{"realistic_mace_3d", "Realistic Mace 3D"},
			{"redstone_tweaks", "Redstone Tweaks"},
			{"ukuarmorhud", "UkuArmorHUD"},
			{"decompression", "Decompression"},
			{"purple_sweep_attack", "purple sweep attack"},
			{"tiny_trident", "tiny trident"},
			{"spatula_trident", "Spatula (Dreizack als Pfannenwender)"},
			{"waystones", "Waystones"},
	};

	public static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		ModContainer mod = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
		for (String[] p : PACKS) {
			ResourceLoader.registerBuiltinPack(
					Identifier.fromNamespaceAndPath(MOD_ID, p[0]),
					mod,
					Component.literal("§d[Andrin] §f" + p[1]),
					PackActivationType.NORMAL);
		}

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openMenuKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.andrinspacks.open_menu", InputConstants.KEY_K, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new PackToggleScreen(null));
				}
			}
		});
	}
}
