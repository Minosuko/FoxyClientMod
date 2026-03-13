package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.MouseScrollEvent;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        
        MouseScrollEvent event = new MouseScrollEvent(horizontal, vertical);
        FoxyClient.INSTANCE.getEventBus().post(event);
        
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
