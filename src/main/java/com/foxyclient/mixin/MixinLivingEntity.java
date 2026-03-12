package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Module;
import com.foxyclient.module.render.Fullbright;
import com.foxyclient.util.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Shadow public int jumpingCooldown;

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void onKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        // Check if this entity is the player
        if ((Object) this == MinecraftClient.getInstance().player) {
            Module velocityModule = FoxyClient.INSTANCE.getModuleManager().getModule("Velocity");
            if (velocityModule != null && velocityModule.isEnabled() && velocityModule instanceof com.foxyclient.module.combat.Velocity velocity) {
                String mode = velocity.getMode();
                if (mode.equals("Cancel")) {
                    ci.cancel();
                } else if (mode.equals("Reduce")) {
                    // Knockback is usually handled by velocity. We can scale it down if it's applied here.
                    // But actually, Minecraft applies knockback via taking the strength and adding it to velocity.
                    // If we want to truly reduce it via modify variable, we should inject into the parameter or just cancel and apply our own.
                    // Since the user spec says "reduce `strength` via `ModifyVariable` if Reduce", we can use a ModifyVariable.
                    // But an easier way if we use At("HEAD") is we can't change strength without @ModifyVariable.
                    // The instruction said "reduce `strength` via `ModifyVariable` if "Reduce", and ignore if "Packet"."
                }
            }
        }
    }

    @ModifyVariable(method = "takeKnockback", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double modifyKnockbackStrength(double strength) {
        if (FoxyClient.INSTANCE == null) return strength;
        if ((Object) this == MinecraftClient.getInstance().player) {
            Module velocityModule = FoxyClient.INSTANCE.getModuleManager().getModule("Velocity");
            if (velocityModule != null && velocityModule.isEnabled() && velocityModule instanceof com.foxyclient.module.combat.Velocity velocity) {
                if (velocity.getMode().equals("Reduce")) {
                    // Horizontal multiplier applies to X/Z knockback
                    // Since takeKnockback's 'strength' dictates the magnitude, we scale strength by Horizontal.
                    // Vertical is done separately by modifying velocity y in tick or applying it...
                    // For simplicity and matching the standard way, horizontal controls strength here.
                    return strength * velocity.getHorizontalMultiplier();
                }
            }
        }
        return strength;
    }

    @Inject(method = "getStepHeight", at = @At("HEAD"), cancellable = true)
    private void onGetStepHeight(CallbackInfoReturnable<Float> cir) {
        if (FoxyClient.INSTANCE == null) return;
        if ((Object) this == MinecraftClient.getInstance().player) {
            Module step = FoxyClient.INSTANCE.getModuleManager().getModule("Step");
            if (step != null && step.isEnabled()) {
                cir.setReturnValue(1.0f);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        if ((Object) this == MinecraftClient.getInstance().player) {
            // NoJumpDelay
            Module noJumpDelay = FoxyClient.INSTANCE.getModuleManager().getModule("NoJumpDelay");
            if (noJumpDelay != null && noJumpDelay.isEnabled()) {
                this.jumpingCooldown = 0;
            }

            // Freelook / Freecam rotation suppression
            com.foxyclient.module.render.Freelook freelook = com.foxyclient.module.render.Freelook.get();
            com.foxyclient.module.render.Freecam freecam = com.foxyclient.module.render.Freecam.get();
            
            if ((freelook != null && freelook.isEnabled()) || (freecam != null && freecam.isEnabled())) {
                LivingEntityAccessor accessor = (LivingEntityAccessor) this;
                // If freelook is on, we want the body to stay still. 
                // We don't use RotationManager here because that's for server-side rotations/spoofing.
                // We just want to keep the render model still.
                float yaw = ((LivingEntity)(Object)this).getYaw();
                float pitch = ((LivingEntity)(Object)this).getPitch();
                
                accessor.setBodyYaw(yaw);
                accessor.setLastBodyYaw(yaw);
                accessor.setHeadYaw(yaw);
                accessor.setLastHeadYaw(yaw);
            }
            // Spinbot/Rotation rendering spoofing
            else if (RotationManager.isActive()) {
                boolean isFirstPerson = MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
                if (!RotationManager.shouldNormalize() || !isFirstPerson) {
                    LivingEntityAccessor accessor = (LivingEntityAccessor) this;
                    float yaw = RotationManager.getServerYaw();
                    float prevYaw = RotationManager.getPrevServerYaw();
                    
                    accessor.setBodyYaw(yaw);
                    accessor.setLastBodyYaw(prevYaw);
                    accessor.setHeadYaw(yaw);
                    accessor.setLastHeadYaw(prevYaw);
                }
            }
        }
    }

}
