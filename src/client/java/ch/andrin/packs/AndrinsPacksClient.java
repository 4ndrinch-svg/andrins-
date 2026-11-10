package ch.andrin.packs;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.world.entity.EntityTypes;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Registriert alle Packs als einzeln schaltbare eingebaute Ressourcenpakete
 * und öffnet mit Taste K das Andrin-Pack-Menü.
 */
public class AndrinsPacksClient implements ClientModInitializer {
	public static final String MOD_ID = "andrinspacks";

	/** {ordner, anzeigename, kategorie} */
	public static final String[][] PACKS = {
			{"deepslate_to_netherite", "Deepslate to Netherite", "Streamer"},
			{"low_mace", "Low Mace", "Kampf"},
			{"blue_netherite", "Blue Netherite", "Kampf"},
			{"dramatic_skys", "Dramatic Skys", "Welt"},
			{"3d_redstone_dot_to_paw", "3D Redstone (Dot-to-Paw)", "Redstone"},
			{"cubic_sun_moon", "Cubic Sun & Moon", "Welt"},
			{"black_steel_shield", "Black Steel Shield", "Kampf"},
			{"cobweb_outline", "Cobweb Outline", "Welt"},
			{"clear_water", "Clear Water", "Welt"},
			{"clear_lava_water_claw", "Clear Lava & Water (CLaW)", "Welt"},
			{"brighter_dark_blocks", "Brighter Dark Blocks", "Welt"},
			{"animated_purple_xp_bar", "Animated Purple XP Bar", "Lila"},
			{"crystalcore_totem", "CrystalCore Totem", "Kampf"},
			{"brighter_nether_bricks", "Brighter Nether Bricks", "Welt"},
			{"3d_redstone_dust", "3D Redstone Dust", "Redstone"},
			{"brewing_guide", "Brewing Guide", "HUD"},
			{"enchantment_outlines", "Enchantment Outlines", "Kampf"},
			{"fullbright", "Fullbright", "Welt"},
			{"low_fire_extreme", "Low Fire EXTREME", "Kampf"},
			{"low_shield", "Low Shield", "Kampf"},
			{"anti_leak_netherite", "Anti-Leak Zufall (Stein, Erze, Bedrock = zufällige Blöcke)", "Streamer"},
			{"new_glowing_ores", "New Glowing Ores", "Welt"},
			{"no_totem_pop_particles", "No Totem Pop Particles", "Kampf"},
			{"purplepearls", "PurplePearls", "Lila"},
			{"purple_block_selection_outline", "Purple Block Selection Outline", "Lila"},
			{"purple_hearts", "Purple Hearts", "Lila"},
			{"purple_netherite", "Purple Netherite", "Lila"},
			{"realistic_animals", "Realistic Animals", "Welt"},
			{"realistic_mace_3d", "Realistic Mace 3D", "Kampf"},
			{"redstone_tweaks", "Redstone Tweaks", "Redstone"},
			{"ukuarmorhud", "UkuArmorHUD", "HUD"},
			{"decompression", "Decompression", "HUD"},
			{"purple_sweep_attack", "purple sweep attack", "Lila"},
			{"tiny_trident", "tiny trident", "Kampf"},
			{"spatula_trident", "Spatula (Dreizack als Pfannenwender)", "Spass"},
			{"waystones", "Waystones", "Welt"},
	};

	public static final String[] CATEGORIES = {"Alle", "Kampf", "Welt", "Lila", "Redstone", "HUD", "Streamer", "Spass"};

	public static KeyMapping openMenuKey;
	public static KeyMapping trollKey;

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

		// Speicher: beim Start wiederherstellen (gespeichert wird beim Schliessen des Menüs)
		ClientLifecycleEvents.CLIENT_STARTED.register(PackMemory::restore);

		SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(MOD_ID, "spatula"), SpatulaSpecialRenderer.Unbaked.MAP_CODEC);
		LevelRenderEvents.END_MAIN.register(ChestEsp::render);
		EntityRendererRegistry.register(EntityTypes.TRIDENT, SpatulaTridentRenderer::new);

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openMenuKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.andrinspacks.open_menu", InputConstants.KEY_K, category));

		trollKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.andrinspacks.troll_vision", InputConstants.KEY_G, category));

		ClientTickEvents.END_CLIENT_TICK.register(ChestEsp::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (trollKey.consumeClick()) {
				TrollVision.enabled = !TrollVision.enabled;
				if (client.player != null) {
					client.player.sendOverlayMessage(Component.literal(TrollVision.enabled ? "§dSpieler, Kisten, Trichter & Kolben §aAN" : "§dSpieler, Kisten, Trichter & Kolben §cAUS"));
				}
			}
			while (openMenuKey.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new PackToggleScreen(null));
				}
			}
		});
	}
}
