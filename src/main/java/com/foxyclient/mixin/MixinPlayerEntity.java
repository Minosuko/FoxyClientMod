package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Module;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void onGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (FoxyClient.INSTANCE == null) return;
        Module reach = FoxyClient.INSTANCE.getModuleManager().getModule("Reach");
        if (reach != null && reach.isEnabled()) {
            cir.setReturnValue(((com.foxyclient.module.player.Reach) reach).getReach());
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void onGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (FoxyClient.INSTANCE == null) return;
        Module reach = FoxyClient.INSTANCE.getModuleManager().getModule("Reach");
        if (reach != null && reach.isEnabled()) {
            cir.setReturnValue(((com.foxyclient.module.player.Reach) reach).getReach());
        }
    }
}
