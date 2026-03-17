package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboardInput {

    /*
     * Signature for 1.21.11: onKey(long window, int key, KeyInput input)
     * class_11908 is the intermediary name for KeyInput record.
     */
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        
        int key = input.key();
        
        // Filter out invalid keys to prevent Unicode-related crashes in listeners
        if (key < -1 || key > 1024) return;

        FoxyClient.INSTANCE.getEventBus().post(new KeyEvent(key, action));
        FoxyClient.INSTANCE.getModuleManager().onKey(key, action);
    }
}
