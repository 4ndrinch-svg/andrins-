package ch.andrin.packs;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Item-Renderer "andrinspacks:spatula" für items/trident.json im Spatula-Pack. */
public class SpatulaSpecialRenderer implements NoDataSpecialModelRenderer {
	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
		SpatulaMesh.submit(poseStack, collector, light, overlay);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0.27F, -0.38F, 0.31F));
		output.accept(new Vector3f(0.73F, 1.25F, 0.69F));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpatulaSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new SpatulaSpecialRenderer();
		}
	}
}
