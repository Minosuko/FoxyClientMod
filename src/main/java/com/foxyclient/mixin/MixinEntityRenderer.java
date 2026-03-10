package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Module;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void onHasLabel(T entity, double tickDelta, CallbackInfoReturnable<Boolean> cir) {
        if (FoxyClient.INSTANCE != null) {
            Module nametags = FoxyClient.INSTANCE.getModuleManager().getModule("Nametags");
            if (nametags != null && nametags.isEnabled() && entity instanceof PlayerEntity) {
                cir.setReturnValue(false);
            }
        }
    }
}
