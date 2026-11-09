package ch.andrin.packs;

import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Kisten durch Wände: farbige Umrisse + Linien vom Fadenkreuz (nur für private Welten).
 * Läuft zusammen mit TrollVision (Taste G).
 */
public final class ChestEsp {
	// Linien ohne Tiefentest -> durch Blöcke sichtbar
	private static final RenderPipeline PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath(AndrinsPacksClient.MOD_ID, "pipeline/esp_lines"))
					.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
					.withDepthStencilState(Optional.empty())
					.build());
	private static RenderType type;

	private ChestEsp() {}

	private static RenderType type() {
		if (type == null) type = RenderType.create("andrinspacks_esp", RenderSetup.builder(PIPELINE).createRenderSetup());
		return type;
	}

	/** Farbe pro Kistenart (ARGB), 0 = ignorieren. */
	private static int color(BlockEntity be) {
		if (be instanceof TrappedChestBlockEntity) return 0xFFFF3030;   // Fallenkiste: rot
		if (be instanceof ChestBlockEntity) return 0xFFFFB000;          // Kiste: gold
		if (be instanceof EnderChestBlockEntity) return 0xFFB040FF;     // Endertruhe: lila
		if (be instanceof ShulkerBoxBlockEntity) return 0xFFFF50D0;     // Shulker: pink
		if (be instanceof BarrelBlockEntity) return 0xFF40E0FF;         // Fass: türkis
		return 0;
	}

	public static void render(LevelRenderContext ctx) {
		if (!TrollVision.enabled) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		Vec3 cam = ctx.levelState().cameraRenderState.pos;
		PoseStack poseStack = ctx.poseStack();
		SubmitNodeCollector collector = ctx.submitNodeCollector();
		RenderType rt = type();

		// Startpunkt der Linien: kurz vor dem Fadenkreuz
		Vec3 look = mc.player.getViewVector(1.0F);
		final float sx = (float) (look.x * 0.6), sy = (float) (look.y * 0.6), sz = (float) (look.z * 0.6);

		int radius = mc.options.getEffectiveRenderDistance() + 1;
		int pcx = mc.player.blockPosition().getX() >> 4, pcz = mc.player.blockPosition().getZ() >> 4;

		for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
			for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					int c = color(be);
					if (c == 0) continue;
					BlockPos p = be.getBlockPos();
					float x = (float) (p.getX() - cam.x), y = (float) (p.getY() - cam.y), z = (float) (p.getZ() - cam.z);

					// Umriss
					poseStack.pushPose();
					poseStack.translate(x, y, z);
					collector.submitShapeOutline(poseStack, Shapes.block(), rt, c, 3.0F, false);
					poseStack.popPose();

					// Linie zum Kistenmittelpunkt
					final float tx = x + 0.5F, ty = y + 0.5F, tz = z + 0.5F;
					final float len = (float) Math.sqrt((tx - sx) * (tx - sx) + (ty - sy) * (ty - sy) + (tz - sz) * (tz - sz));
					final float nx = (tx - sx) / len, ny = (ty - sy) / len, nz = (tz - sz) / len;
					collector.submitCustomGeometry(poseStack, rt, (pose, buf) -> {
						buf.addVertex(pose, sx, sy, sz).setColor(c).setNormal(pose, nx, ny, nz).setLineWidth(2.0F);
						buf.addVertex(pose, tx, ty, tz).setColor(c).setNormal(pose, nx, ny, nz).setLineWidth(2.0F);
					});
				}
			}
		}
	}
}
