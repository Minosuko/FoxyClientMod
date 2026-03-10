package com.foxyclient.seedcracker.finder;

import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.config.Config;
import com.foxyclient.seedcracker.cracker.BiomeData;
import com.foxyclient.seedcracker.cracker.DataAddedEvent;
import com.foxyclient.seedcracker.util.BiomeFixer;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

public class BiomeFinder extends Finder {

    protected static List<BlockPos> SEARCH_POSITIONS;

    public BiomeFinder(World world, ChunkPos chunkPos) {
        super(world, chunkPos);
        this.searchPositions = SEARCH_POSITIONS;
    }

    protected List<BlockPos> searchPositions;

    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> {
            if (pos.getX() != 8 || pos.getZ() != 8) return true;
            return pos.getY() != 64;
        });
    }

    public static List<Finder> create(World world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new BiomeFinder(world, chunkPos));
        return finders;
    }

    @Override
    public List<BlockPos> findInChunk() {
        if (!Config.get().biome.get()) return new ArrayList<>();

        RegistryEntry<Biome> biomeEntry = this.world.getBiome(this.chunkPos.getBlockPos(8, 64, 8));
        com.seedfinding.mcbiome.biome.Biome biome = BiomeFixer.swap(biomeEntry.value());

        BiomeData data = new BiomeData(biome, (this.chunkPos.x << 4) + 8, (this.chunkPos.z << 4) + 8);
        FoxySeedCracker.get().getDataStorage().addBiomeData(data, DataAddedEvent.POKE_BIOMES);

        return new ArrayList<>();
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return true;
    }
}
