package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.MouseScrollEvent;
import com.foxyclient.util.CPSTracker;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {
    @org.spongepowered.asm.mixin.Shadow
    private double cursorDeltaX;
    @org.spongepowered.asm.mixin.Shadow
    private double cursorDeltaY;

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        
        // FoxyMoonlightClient Zoom interception
        com.foxyclient.module.render.Zoom zoom = com.foxyclient.module.render.Zoom.get();
        if (zoom != null && zoom.isZooming()) {
            zoom.onScroll(vertical);
            ci.cancel();
            return;
        }

        MouseScrollEvent event = new MouseScrollEvent(horizontal, vertical);
        FoxyClient.INSTANCE.getEventBus().post(event);
        
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void onUpdateMouse(CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        com.foxyclient.module.render.Freelook freelook = com.foxyclient.module.render.Freelook.get();
        if (freelook != null && freelook.isActive()) {
            freelook.onMouseUpdate(this.cursorDeltaX, this.cursorDeltaY);
            this.cursorDeltaX = 0;
            this.cursorDeltaY = 0;
        }
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(CallbackInfo ci) {
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, 0) == 1) CPSTracker.INSTANCE.recordLeft();
        if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, 1) == 1) CPSTracker.INSTANCE.recordRight();
    }
}
