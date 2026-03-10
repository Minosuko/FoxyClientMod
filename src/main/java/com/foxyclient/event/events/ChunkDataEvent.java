package com.foxyclient.event.events;

import net.minecraft.world.chunk.WorldChunk;
import com.foxyclient.event.Event;

public class ChunkDataEvent extends Event {
    private final WorldChunk chunk;

    public ChunkDataEvent(WorldChunk chunk) {
        this.chunk = chunk;
    }

    public WorldChunk getChunk() {
        return chunk;
    }
}
