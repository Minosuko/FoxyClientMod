package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinInputTick {
    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickReturn(CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        var pf = FoxyClient.INSTANCE.getPathFinder();
        if (pf != null && pf.isActive() && !pf.isPaused()) {
            // Re-apply pathfinder movement after KeyboardInput.tick() overwrites it
            pf.tick(); 
        }
    }
}
