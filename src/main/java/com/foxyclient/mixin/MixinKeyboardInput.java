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
            // In 1.21.11, actionObj is a KeyInput record (class_11908).
            // Its fields are (int key, int scancode, KeyAction action, int modifiers).
            // We find the accessor method for `action` by checking which method returns an Enum.
            for (java.lang.reflect.Method m : actionObj.getClass().getDeclaredMethods()) {
                if (m.getReturnType().isEnum()) {
                    Object enumVal = m.invoke(actionObj);
                    // The ordinals in KeyAction map to GLFW actions: 0=PRESS, 1=RELEASE, 2=REPEAT
                    // Wait, we need to map the Enum name to exactly 1 (press), 0 (release), 2 (repeat)
                    String enumName = ((Enum<?>) enumVal).name().toLowerCase();
                    if (enumName.contains("press")) action = 1;
                    else if (enumName.contains("release")) action = 0;
                    else if (enumName.contains("repeat")) action = 2;
                    else {
                        // Fallback using ordinal. Often PRESS is first, then RELEASE, then REPEAT, or similar.
                        // We will just use ordinal fallback if names don't match, or log it.
                        action = ((Enum<?>) enumVal).ordinal();
                        // GLFW action values: RELEASE = 0, PRESS = 1, REPEAT = 2.
                    }
                    break; // found the action Enum
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            action = 1; // Default
        }

        // Action: 1 = Press, 0 = Release, 2 = Repeat
        FoxyClient.INSTANCE.getEventBus().post(new KeyEvent(key, action));
        FoxyClient.INSTANCE.getModuleManager().onKey(key, action);
    }
}
