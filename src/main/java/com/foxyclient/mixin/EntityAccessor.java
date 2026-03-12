package com.foxyclient.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("lastRenderX")
    void setLastRenderX(double x);

    @Accessor("lastRenderY")
    void setLastRenderY(double y);

    @Accessor("lastRenderZ")
    void setLastRenderZ(double z);
}
