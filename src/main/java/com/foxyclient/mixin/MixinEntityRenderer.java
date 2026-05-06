package com.foxyclient.mixin;

import com.foxyclient.mixin_interface.IEntityRenderState;
import com.foxyclient.FoxyClient;
import com.foxyclient.module.Module;
import com.foxyclient.module.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
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

        // FoxyMoonlightClient Nametags formatting
        if (entity instanceof net.minecraft.entity.player.PlayerEntity) {
            MinecraftClient client = MinecraftClient.getInstance();
            boolean isSelf = client.player != null && entity.getUuid().equals(client.player.getUuid());
            ((IEntityRenderState) state).foxyclient$setSelf(isSelf);

            com.foxyclient.module.ui.Nametags module = com.foxyclient.module.ui.Nametags.INSTANCE;

            if (module != null && module.isEnabled() && state.displayName != null) {
                net.minecraft.text.Text original = state.displayName;
                net.minecraft.text.Text modified = original;

                if (!isSelf) {
                    if (module.healthCount.get()) {
                        float health = ((net.minecraft.entity.player.PlayerEntity) entity).getHealth();
                        int color = health > 15 ? 0x55FF55 : (health > 7 ? 0xFFFF55 : 0xFF5555);
                        String formattedHealth = String.format(" §c%d\u2764", (int) health); // red heart
                        modified = modified.copy().append(net.minecraft.text.Text.literal(formattedHealth));
                    }
                    if (module.distance.get() && client.player != null) {
                        int distance = (int) client.player.distanceTo(entity);
                        modified = modified.copy().append(net.minecraft.text.Text.literal(String.format(" §7[%dm]", distance)));
                    }
                }

                if (isSelf && module.showLogo.get()) {
                    // Pad display name with spaces on the left for the badge
                    modified = net.minecraft.text.Text.empty().append("    ").append(modified);
                }

                state.displayName = modified;
            }
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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;

        // Handle NoRender features
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
