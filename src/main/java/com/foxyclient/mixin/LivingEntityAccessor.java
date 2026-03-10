package com.foxyclient.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("bodyYaw")
    void setBodyYaw(float yaw);

    @Accessor("bodyYaw")
    float getBodyYaw();

    @Accessor("lastBodyYaw")
    void setLastBodyYaw(float yaw);

    @Accessor("lastBodyYaw")
    float getLastBodyYaw();

    @Accessor("headYaw")
    void setHeadYaw(float yaw);

    @Accessor("headYaw")
    float getHeadYaw();

    @Accessor("lastHeadYaw")
    void setLastHeadYaw(float yaw);

    @Accessor("lastHeadYaw")
    float getLastHeadYaw();
}
