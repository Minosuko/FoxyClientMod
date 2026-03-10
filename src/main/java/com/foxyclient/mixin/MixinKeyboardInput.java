package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.KeyEvent;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboardInput {

    /*
    * Fix for Minecraft 1.21.11 signature change.
    * The onKey method signature is: onKey(long window, int key, class_11908 actionObj)
    * Since class_11908 is an intermediary name and its Yarn name is missing in the compiler's view,
    * we use Object with @Coerce to satisfy both the compiler and the Mixin applicator.
    */
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, @Coerce Object actionObj, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        
        int action = -1;
        try {
            // class_11908 likely contains the key action (Press/Release/Repeat).
            // We use reflection to extract it.
            java.lang.reflect.Method m = actionObj.getClass().getMethod("action");
            action = (int) m.invoke(actionObj);
        } catch (Exception e) {
            try {
                // Try a fallback to toString() or other common methods
                String s = actionObj.toString().toLowerCase();
                if (s.contains("press")) action = 1;
                else if (s.contains("release")) action = 0;
                else if (s.contains("repeat")) action = 2;
                else action = 1; // Default
            } catch (Exception ignored) {
                action = 1;
            }
        }

        // Action: 1 = Press, 0 = Release, 2 = Repeat
        FoxyClient.INSTANCE.getEventBus().post(new KeyEvent(key, action));
        if (action == 1) { // Key press
            FoxyClient.INSTANCE.getModuleManager().onKey(key, action);
        }
    }
}
