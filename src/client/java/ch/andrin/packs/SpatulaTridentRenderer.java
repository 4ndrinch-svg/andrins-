package ch.andrin.packs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Geworfener / im Boden steckender Dreizack: zeigt die Spatula, wenn das Spatula-Pack aktiv ist,
 * sonst den normalen Dreizack.
 */
public class SpatulaTridentRenderer extends EntityRenderer<ThrownTrident, SpatulaTridentRenderer.State> {
	public static class State extends ThrownTridentRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public boolean spatula;
	}

	private final ThrownTridentRenderer vanilla;
	private final ItemModelResolver itemModelResolver;
	private ItemStack stack; // erst beim Rendern erzeugen (beim Start sind Items noch nicht bereit)

	public SpatulaTridentRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.vanilla = new ThrownTridentRenderer(context);
		this.itemModelResolver = context.getItemModelResolver();
	}

	private static boolean spatulaActive() {
		for (String id : Minecraft.getInstance().getResourcePackRepository().getSelectedIds()) {
			if (id.endsWith("spatula_trident")) return true;
		}
		return false;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ThrownTrident entity, State state, float partialTicks) {
		vanilla.extractRenderState(entity, state, partialTicks);
		state.spatula = spatulaActive();
		if (state.spatula) {
			if (stack == null) stack = new ItemStack(Items.TRIDENT);
			this.itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.NONE, entity);
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.spatula) {
			vanilla.submit(state, poseStack, collector, camera);
			return;
		}
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
		poseStack.rotateDegrees(Axis.ZP, state.xRot - 90.0F);   // Blatt zeigt in Flugrichtung
		poseStack.scale(1.6F, 1.6F, 1.6F);
		poseStack.translate(0.0F, 0.25F, 0.0F);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	protected AABB getBoundingBoxForCulling(ThrownTrident entity, float partialTicks) {
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.5);
	}
}
