package com.foxyclient.event.events;

import com.foxyclient.event.Event;
import net.minecraft.entity.Entity;

public class EntityDeathEvent extends Event {
    private final Entity entity;

    public EntityDeathEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
