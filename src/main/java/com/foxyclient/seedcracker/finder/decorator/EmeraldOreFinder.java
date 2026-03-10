package com.foxyclient.seedcracker.finder.decorator;

import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.config.Config;
import com.foxyclient.seedcracker.cracker.decorator.EmeraldOre;
import com.foxyclient.seedcracker.finder.BlockFinder;
import com.foxyclient.seedcracker.render.Cuboid;
import com.foxyclient.seedcracker.util.BiomeFixer;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import com.foxyclient.seedcracker.finder.Finder;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

public class EmeraldOreFinder extends BlockFinder {

    protected static List<BlockPos> SEARCH_POSITIONS;

    public EmeraldOreFinder(World world, ChunkPos chunkPos) {
        super(world, chunkPos, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        this.searchPositions = SEARCH_POSITIONS;
    }

    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> pos.getY() < 0 || pos.getY() > 256);
    }

    @Override
    public List<BlockPos> findInChunk() {
        if (!Config.get().emeraldOre.get()) return new ArrayList<>();

        List<BlockPos> result = super.findInChunk();

        result.forEach(pos -> {
            Biome biome = this.world.getBiome(pos).value();
            com.seedfinding.mcbiome.biome.Biome sBiome = BiomeFixer.swap(biome);

            EmeraldOre.Data data = new EmeraldOre.Data(com.foxyclient.seedcracker.Features.EMERALD_ORE, pos.getX(), pos.getY(), pos.getZ(), sBiome);
            // In a real port, we'd add this to DataStorage for structure seed reversal
            this.cuboids.add(new Cuboid(pos, 0xFF00FF00));
        });

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }
}
