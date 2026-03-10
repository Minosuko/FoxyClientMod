package com.foxyclient.seedcracker.finder;

import com.foxyclient.seedcracker.finder.decorator.EndPillarsFinder;
import com.foxyclient.seedcracker.finder.decorator.ore.EmeraldOreFinder;
import com.foxyclient.seedcracker.finder.structure.*;
import com.foxyclient.seedcracker.util.HeightContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class ReloadFinders {

    public static void reloadHeight(int minY, int maxY) {
        Finder.CHUNK_POSITIONS.clear();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Finder.CHUNK_POSITIONS.add(new BlockPos(x, y, z));
                }
            }
        }
        Finder.heightContext = new HeightContext(minY, maxY);

        EmeraldOreFinder.reloadSearchPositions();
        EndPillarsFinder.BedrockMarkerFinder.reloadSearchPositions();
        AbstractTempleFinder.reloadSearchPositions();
        BuriedTreasureFinder.reloadSearchPositions();
        EndCityFinder.reloadSearchPositions();
        MonumentFinder.reloadSearchPositions();
        OutpostFinder.reloadSearchPositions();
        IglooFinder.reloadSearchPositions();
        TrialChambersFinder.reloadSearchPositions();
    }

    public static void reload(int range) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;

        BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                FinderQueue.get().onChunkData(world, new ChunkPos(chunkX + x, chunkZ + z));
            }
        }
    }
}
