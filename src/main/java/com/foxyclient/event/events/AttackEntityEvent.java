package com.foxyclient.event.events;

import com.foxyclient.event.Event;
import net.minecraft.entity.Entity;

public class AttackEntityEvent extends Event {
    private final Entity entity;

    public AttackEntityEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
