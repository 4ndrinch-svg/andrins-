package ch.andrin.packs;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Original-Dreiecksnetz der Spatula (aus dem Meshy-Modell), unverändert gezeichnet. */
public final class SpatulaMesh {
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AndrinsPacksClient.MOD_ID, "textures/entity/spatula_mesh.png");
	private static float[] verts;   // pro Vertex: x y z u v nx ny nz
	private static int[] faces;
	private static boolean failed;

	private SpatulaMesh() {}

	private static synchronized boolean load() {
		if (verts != null) return true;
		if (failed) return false;
		try (InputStream in = SpatulaMesh.class.getResourceAsStream("/assets/andrinspacks/mesh/spatula.mesh")) {
			byte[] all = new DataInputStream(in).readAllBytes();
			ByteBuffer b = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
			int nv = b.getInt(), nf = b.getInt();
			float[] v = new float[nv * 8];
			for (int i = 0; i < v.length; i++) v[i] = b.getFloat();
			int[] f = new int[nf * 3];
			for (int i = 0; i < f.length; i++) f[i] = b.getInt();
			verts = v;
			faces = f;
			return true;
		} catch (Exception e) {
			failed = true;
			System.err.println("[AndrinsPacks] Spatula-Modell konnte nicht geladen werden: " + e);
			return false;
		}
	}

	/** Zeichnet das Modell im Item-Raum (0..1 Block, wie ein Item-Modell). */
	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
		if (!load()) return;
		final float[] v = verts;
		final int[] f = faces;
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buf) -> {
			for (int t = 0; t < f.length; t += 3) {
				// Dreieck als Quad (letzter Punkt doppelt)
				for (int k = 0; k < 4; k++) {
					int o = f[t + Math.min(k, 2)] * 8;
					buf.addVertex(pose, v[o], v[o + 1], v[o + 2])
							.setColor(-1)
							.setUv(v[o + 3], v[o + 4])
							.setOverlay(overlay)
							.setLight(light)
							.setNormal(pose, v[o + 5], v[o + 6], v[o + 7]);
				}
			}
		});
	}
}
