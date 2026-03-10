package com.foxyclient.seedcracker.finder.structure;

import com.foxyclient.seedcracker.Features;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.config.Config;
import com.foxyclient.seedcracker.cracker.DataAddedEvent;
import com.foxyclient.seedcracker.finder.Finder;
import com.foxyclient.seedcracker.util.BiomeFixer;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.RegionStructure;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndCityFinder extends Finder {

    protected static List<BlockPos> SEARCH_POSITIONS;
    protected final Vec3i size = new Vec3i(8, 4, 8);
    protected List<PieceFinder> finders = new ArrayList<>();

    public EndCityFinder(World world, ChunkPos chunkPos) {
        super(world, chunkPos);

        for (Direction direction : Direction.Type.HORIZONTAL) {
            PieceFinder finder = new PieceFinder(world, chunkPos, direction, size);

            finder.searchPositions = SEARCH_POSITIONS;

            buildStructure(finder);
            this.finders.add(finder);
        }
    }

    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> pos.getY() > 90 || pos.getY() < 40);
    }

    public static List<Finder> create(World world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new EndCityFinder(world, chunkPos));
        finders.add(new EndCityFinder(world, new ChunkPos(chunkPos.x - 1, chunkPos.z)));
        finders.add(new EndCityFinder(world, new ChunkPos(chunkPos.x, chunkPos.z - 1)));
        finders.add(new EndCityFinder(world, new ChunkPos(chunkPos.x - 1, chunkPos.z - 1)));
        return finders;
    }

    private void buildStructure(PieceFinder finder) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState endstoneBricks = Blocks.END_STONE_BRICKS.getDefaultState();
        BlockState purpur = Blocks.PURPUR_BLOCK.getDefaultState();
        BlockState purpurPillar = Blocks.PURPUR_PILLAR.getDefaultState();
        BlockState purpleGlass = Blocks.MAGENTA_STAINED_GLASS.getDefaultState();

        //Walls
        finder.fillWithOutline(0, 0, 0, 7, 4, 7, endstoneBricks, null, false);

        //Wall sides
        finder.fillWithOutline(0, 0, 0, 0, 3, 0, purpurPillar, purpurPillar, false);
        finder.fillWithOutline(7, 0, 0, 7, 3, 0, purpurPillar, purpurPillar, false);
        finder.fillWithOutline(0, 0, 7, 0, 3, 7, purpurPillar, purpurPillar, false);
        finder.fillWithOutline(7, 0, 7, 7, 3, 7, purpurPillar, purpurPillar, false);

        //Floor
        finder.fillWithOutline(0, 0, 0, 7, 0, 7, purpur, purpur, false);

        //Doorway
        finder.fillWithOutline(3, 1, 0, 4, 3, 0, air, air, false);

        //Windows
        finder.fillWithOutline(0, 2, 2, 0, 3, 2, purpleGlass, purpleGlass, false);
        finder.fillWithOutline(0, 2, 5, 0, 3, 5, purpleGlass, purpleGlass, false);
        finder.fillWithOutline(7, 2, 2, 7, 3, 2, purpleGlass, purpleGlass, false);
        finder.fillWithOutline(7, 2, 5, 7, 3, 5, purpleGlass, purpleGlass, false);
    }

    @Override
    public List<BlockPos> findInChunk() {
        Biome biome = this.world.getBiome(this.chunkPos.getBlockPos(8, 64, 8)).value();
        if (!Features.END_CITY.isValidBiome(BiomeFixer.swap(biome))) return new ArrayList<>();

        Map<PieceFinder, List<BlockPos>> result = this.findInChunkPieces();
        List<BlockPos> combinedResult = new ArrayList<>();

        result.forEach((pieceFinder, positions) -> {
            combinedResult.addAll(positions);

            positions.forEach(pos -> {
                if (Config.get().getVersion().isNewerOrEqualTo(MCVersion.v1_19)) {
                    BlockPos posFix = pos.add(1, 0, 1);
                    RegionStructure.Data<?> data = Features.END_CITY.at(posFix.getX() >> 4, posFix.getZ() >> 4);

                    if (FoxySeedCracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
                        this.cuboids.add(new com.foxyclient.seedcracker.render.Cuboid(pos, pieceFinder.getLayout(), 0xFF990099));
                        this.cuboids.add(new com.foxyclient.seedcracker.render.Cuboid(posFix, 0xFF990099));
                    }
                } else {
                    RegionStructure.Data<?> data = Features.END_CITY.at(this.chunkPos.x, this.chunkPos.z);

                    if (FoxySeedCracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
                        this.cuboids.add(new com.foxyclient.seedcracker.render.Cuboid(pos, pieceFinder.getLayout(), 0xFF990099));
                        this.cuboids.add(new com.foxyclient.seedcracker.render.Cuboid(pos, 0xFF990099));
                    }
                }
            });
        });

        return combinedResult;
    }

    public Map<PieceFinder, List<BlockPos>> findInChunkPieces() {
        Map<PieceFinder, List<BlockPos>> result = new HashMap<>();

        this.finders.forEach(pieceFinder -> {
            result.put(pieceFinder, pieceFinder.findInChunk());
        });

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isEnd(dimension);
    }

}
