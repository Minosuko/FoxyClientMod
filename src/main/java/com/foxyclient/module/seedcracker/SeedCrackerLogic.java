package com.foxyclient.module.seedcracker;

import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.finder.FinderQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;

public class SeedCrackerLogic {

    public static void onChunkData(ClientWorld world, ChunkPos pos) {
        if (FoxySeedCracker.get() == null || !com.foxyclient.seedcracker.config.Config.get().active) return;
        if (world == null) world = MinecraftClient.getInstance().world;
        if (world == null) return;
        FinderQueue.get().onChunkData(world, pos);
    }

    public static void onJoinWorld(ClientWorld world) {
        if (FoxySeedCracker.get() == null) return;
        FoxySeedCracker.get().reset();
    }
}
