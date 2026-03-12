package com.foxyclient.mixin;

import com.foxyclient.mixin_interface.IEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinEntityRenderState implements IEntityRenderState {
    @Unique
    private int entityId;

    @Override
    public void setEntityId(int id) {
        this.entityId = id;
    }

    @Override
    public int getEntityId() {
        return entityId;
    }
}
