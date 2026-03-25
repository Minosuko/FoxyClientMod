package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;

import java.util.UUID;

public class EntityOwner extends Module {
    
    public EntityOwner() {
        super("EntityOwner", "Shows owner names of tamed mobs", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (Entity entity : mc.world.getEntities()) {
            UUID ownerId = null;
            if (entity instanceof TameableEntity tameable) {
                if (tameable.isTamed()) ownerId = tameable.getOwnerReference().getUuid();
            } else if (entity instanceof AbstractHorseEntity horse) {
                if (horse.isTame()) ownerId = horse.getOwnerReference().getUuid();
            }

            if (ownerId != null) {
                // In a real scenario we'd query the Mojang API for the UUID,
                // but for real-time rendering, displaying the UUID or checking if it matches the player is safe.
                String displayName = ownerId.equals(mc.player.getUuid()) ? "§a" + mc.player.getName().getString() : "§7" + ownerId.toString().substring(0, 8);
                
                // Draw name tag above the mob
                RenderUtil.drawNametag(
                    event.getMatrices(), 
                    event.getVertexConsumers(), 
                    "Owner: " + displayName, 
                    entity.getX(), 
                    entity.getY() + entity.getHeight() + 0.5, 
                    entity.getZ(), 
                    0.025f
                );
            }
        }
    }
}
