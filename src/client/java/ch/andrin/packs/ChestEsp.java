package ch.andrin.packs;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
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
	private static final RenderPipeline FILL_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath(AndrinsPacksClient.MOD_ID, "pipeline/esp_fill"))
					.withDepthStencilState(Optional.empty())
					.build());
	private static RenderType type;
	private static RenderType fillType;

	/** Kolben-Positionen aus dem letzten Scan, mit Farbe. */
	private static final List<long[]> pistons = new ArrayList<>();
	private static int scanCooldown = 0;

	private ChestEsp() {}

	private static RenderType type() {
		if (type == null) type = RenderType.create("andrinspacks_esp", RenderSetup.builder(PIPELINE).createRenderSetup());
		return type;
	}

	private static RenderType fillType() {
		if (fillType == null) fillType = RenderType.create("andrinspacks_esp_fill", RenderSetup.builder(FILL_PIPELINE).sortOnUpload().createRenderSetup());
		return fillType;
	}

	/** Sucht alle ~2 Sekunden Kolben im Umkreis (Kolben haben keine Block-Entity). */
	public static void tick(Minecraft mc) {
		if (!TrollVision.enabled || mc.level == null || mc.player == null) return;
		if (scanCooldown-- > 0) return;
		scanCooldown = 40;
		List<long[]> found = new ArrayList<>();
		int r = Math.min(mc.options.getEffectiveRenderDistance(), 6);
		int pcx = mc.player.blockPosition().getX() >> 4, pcz = mc.player.blockPosition().getZ() >> 4;
		for (int cx = pcx - r; cx <= pcx + r; cx++) {
			for (int cz = pcz - r; cz <= pcz + r; cz++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				LevelChunkSection[] sections = chunk.getSections();
				for (int si = 0; si < sections.length; si++) {
					LevelChunkSection sec = sections[si];
					if (sec == null || sec.hasOnlyAir()) continue;
					if (!sec.getStates().maybeHas(st -> (st.is(Blocks.PISTON) || st.is(Blocks.STICKY_PISTON)))) continue;
					int baseY = chunk.getMinY() + si * 16;
					for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
						BlockState st = sec.getBlockState(x, y, z);
						if ((st.is(Blocks.PISTON) || st.is(Blocks.STICKY_PISTON))) {
							int col = st.is(Blocks.STICKY_PISTON) ? 0xFF80FF20 : 0xFF20FF60;
							found.add(new long[] {BlockPos.asLong(cx * 16 + x, baseY + y, cz * 16 + z), col});
						}
					}
				}
			}
		}
		synchronized (pistons) {
			pistons.clear();
			pistons.addAll(found);
		}
	}

	/** Farbe pro Kistenart (ARGB), 0 = ignorieren. */
	private static int color(BlockEntity be) {
		if (be instanceof TrappedChestBlockEntity) return EspSettings.on("trapped") ? 0xFFFF3030 : 0;
		if (be instanceof ChestBlockEntity) return EspSettings.on("chests") ? 0xFFFFB000 : 0;
		if (be instanceof EnderChestBlockEntity) return EspSettings.on("ender") ? 0xFFB040FF : 0;
		if (be instanceof ShulkerBoxBlockEntity) return EspSettings.on("shulker") ? 0xFFFF50D0 : 0;
		if (be instanceof BarrelBlockEntity) return EspSettings.on("barrels") ? 0xFF40E0FF : 0;
		if (be instanceof HopperBlockEntity) return EspSettings.on("hoppers") ? 0xFFFFFFFF : 0;
		if (be instanceof DispenserBlockEntity) return EspSettings.on("dispensers") ? 0xFFA0A0A0 : 0;
		return 0;
	}

	public static void render(LevelRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		if (InfoHud.deathPos != null && mc.level.dimension().toString().equals(InfoHud.deathDim)) {
			Vec3 c0 = ctx.levelState().cameraRenderState.pos;
			Vec3 lk = mc.player.getViewVector(1.0F);
			drawMarker(InfoHud.deathPos, 0xFFFF0000, c0, ctx.poseStack(), ctx.submitNodeCollector(), type(),
					(float) (lk.x * 0.6), (float) (lk.y * 0.6), (float) (lk.z * 0.6));
		}
		if (!TrollVision.enabled) return;

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
					drawMarker(be.getBlockPos(), c, cam, poseStack, collector, rt, sx, sy, sz);
				}
			}
		}
		if (EspSettings.on("pistons")) synchronized (pistons) {
			for (long[] p : pistons) drawMarker(BlockPos.of(p[0]), (int) p[1], cam, poseStack, collector, rt, sx, sy, sz);
		}
		if (EspSettings.on("bases")) synchronized (BaseFinder.bases) {
			for (int[] b : BaseFinder.bases) drawBase(b, cam, poseStack, collector, rt, sx, sy, sz);
		}
	}

	private static void drawBase(int[] b, Vec3 cam, PoseStack poseStack, SubmitNodeCollector collector, RenderType rt,
			float sx, float sy, float sz) {
		final int c = 0xFFFF2020;
		float x = (float) (b[0] * 16 - cam.x), y = (float) (b[3] - 6 - cam.y), z = (float) (b[1] * 16 - cam.z);
		poseStack.pushPose();
		poseStack.translate(x, y, z);
		collector.submitShapeOutline(poseStack, Shapes.create(new AABB(0, 0, 0, 16, 12, 16)), rt, c, 8.0F, false);
		poseStack.popPose();
		final float x0 = x, y0 = y, z0 = z, x1 = x + 16, y1 = y + 12, z1 = z + 16;
		final int fill = 0x30FF2020;
		if (EspSettings.on("fill")) collector.submitCustomGeometry(poseStack, fillType(), (pose, buf) -> {
			float[][] q = {
					{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},
					{x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
					{x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
			for (float[] f : q) for (int i = 0; i < 12; i += 3) buf.addVertex(pose, f[i], f[i + 1], f[i + 2]).setColor(fill);
		});
		final float tx = x + 8, ty = y + 6, tz = z + 8;
		final float len = (float) Math.sqrt((tx - sx) * (tx - sx) + (ty - sy) * (ty - sy) + (tz - sz) * (tz - sz));
		final float nx = (tx - sx) / len, ny = (ty - sy) / len, nz = (tz - sz) / len;
		if (EspSettings.on("tracers")) collector.submitCustomGeometry(poseStack, rt, (pose, buf) -> {
			buf.addVertex(pose, sx, sy, sz).setColor(c).setNormal(pose, nx, ny, nz).setLineWidth(6.0F);
			buf.addVertex(pose, tx, ty, tz).setColor(c).setNormal(pose, nx, ny, nz).setLineWidth(6.0F);
		});
	}

	private static void drawMarker(BlockPos p, int c, Vec3 cam, PoseStack poseStack, SubmitNodeCollector collector, RenderType rt,
			float sx, float sy, float sz) {
					float x = (float) (p.getX() - cam.x), y = (float) (p.getY() - cam.y), z = (float) (p.getZ() - cam.z);

					// gefüllte, halbtransparente Box
					final int fill = (c & 0x00FFFFFF) | 0x55000000;
					final float x0 = x - 0.02F, y0 = y - 0.02F, z0 = z - 0.02F, x1 = x + 1.02F, y1 = y + 1.02F, z1 = z + 1.02F;
					if (EspSettings.on("fill")) collector.submitCustomGeometry(poseStack, fillType(), (pose, buf) -> {
						float[][] q = {
								{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},
								{x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
								{x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
						for (float[] f : q) for (int i = 0; i < 12; i += 3) buf.addVertex(pose, f[i], f[i + 1], f[i + 2]).setColor(fill);
					});

					// Umriss
					poseStack.pushPose();
					poseStack.translate(x, y, z);
					collector.submitShapeOutline(poseStack, Shapes.block(), rt, c, 6.0F, false);
					poseStack.popPose();

					// Linie zum Kistenmittelpunkt
					final float tx = x + 0.5F, ty = y + 0.5F, tz = z + 0.5F;
					final float len = (float) Math.sqrt((tx - sx) * (tx - sx) + (ty - sy) * (ty - sy) + (tz - sz) * (tz - sz));
					final float nx = (tx - sx) / len, ny = (ty - sy) / len, nz = (tz - sz) / len;
					if (EspSettings.on("tracers")) collector.submitCustomGeometry(poseStack, rt, (pose, buf) -> {
						buf.addVertex(pose, sx, sy, sz).setColor(c).setNormal(pose, nx, ny, nz).setLineWidth(4.0F);
						buf.addVertex(pose, tx, ty, tz).setColor(c).setNormal(pose, nx, ny, nz).setLineWidth(4.0F);
					});
	}
}
