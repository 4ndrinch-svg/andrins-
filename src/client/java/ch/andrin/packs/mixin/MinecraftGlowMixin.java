package ch.andrin.packs.mixin;

import ch.andrin.packs.TrollVision;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lässt andere Spieler leuchten (Umriss durch Wände), wenn Troll-Sicht aktiv ist. */
@Mixin(Minecraft.class)
public class MinecraftGlowMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void andrinspacks$glowPlayers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (TrollVision.shouldGlow(entity)) cir.setReturnValue(true);
	}
}
