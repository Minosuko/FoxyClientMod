package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.render.Freecam;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinInputTick {
    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickReturn(CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;

        // Freecam: zero all movement so the player body stays perfectly still
        // while the camera keys (WASD/Space/Shift) are used for flying
        Freecam freecam = Freecam.get();
        if (freecam != null && freecam.isEnabled()) {
            Input input = (Input) (Object) this;
            input.playerInput = PlayerInput.DEFAULT;
            ((InputAccessor) input).setMovementVector(Vec2f.ZERO);
            return;
        }

        // Pathfinder movement override
        var pf = FoxyClient.INSTANCE.getPathFinder();
        if (pf != null && pf.isActive() && !pf.isPaused()) {
            pf.tick();
        }
    }
}
