package com.foxyclient.mixin;

import net.minecraft.client.MinecraftClient;
import com.foxyclient.mixin_interface.IEntityRenderState;
import com.foxyclient.FoxyClient;
import com.foxyclient.module.Module;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.entity.EntityType;
import com.foxyclient.module.render.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void onUpdateRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        ((IEntityRenderState) state).setEntityId(entity.getId());
        
        // Force local player to be visible during Freecam
        com.foxyclient.module.render.Freecam freecam = com.foxyclient.module.render.Freecam.get();
        if (freecam != null && freecam.isEnabled() && entity == MinecraftClient.getInstance().player) {
            state.invisible = false;
        }
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(T entity, net.minecraft.client.render.Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        com.foxyclient.module.render.Freecam freecam = com.foxyclient.module.render.Freecam.get();
        if (freecam != null && freecam.isEnabled() && !freecam.shouldHidePlayer()) {
            if (MinecraftClient.getInstance().player != null && entity == MinecraftClient.getInstance().player) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void onHasLabel(T entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        if (FoxyClient.INSTANCE != null) {
            Module nametags = FoxyClient.INSTANCE.getModuleManager().getModule("Nametags");
            if (nametags != null && nametags.isEnabled() && entity.getType() == EntityType.PLAYER) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        // Handle NoRender features first
        NoRender noRender = FoxyClient.INSTANCE.getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isEnabled()) {
            if (noRender.noItems() && state.entityType == EntityType.ITEM) {
                ci.cancel();
                return;
            }
        }

        // Handle Freecam hiding player
        com.foxyclient.module.render.Freecam freecam = com.foxyclient.module.render.Freecam.get();
        if (freecam != null && freecam.isEnabled() && freecam.shouldHidePlayer()) {
            int entityId = ((IEntityRenderState) state).getEntityId();
            if (MinecraftClient.getInstance().player != null && entityId == MinecraftClient.getInstance().player.getId()) {
                ci.cancel();
                return;
            }
        }
        
        // Handle XRay features
        com.foxyclient.module.render.XRay xray = FoxyClient.INSTANCE.getModuleManager().getModule(com.foxyclient.module.render.XRay.class);
        if (xray != null && xray.isEnabled()) {
            if (!xray.showMobs.get() && state instanceof LivingEntityRenderState && state.entityType != EntityType.PLAYER) {
                ci.cancel();
                return;
            }
        }
    }
}
