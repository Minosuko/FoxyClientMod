package com.foxyclient.seedcracker.finder.structure;

import com.foxyclient.seedcracker.Features;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.cracker.DataAddedEvent;
import com.foxyclient.seedcracker.finder.Finder;
import com.foxyclient.seedcracker.render.Cuboid;
import com.foxyclient.seedcracker.util.BiomeFixer;
import com.seedfinding.mcfeature.structure.RegionStructure;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

public class OutpostFinder extends Finder {

    protected static Map<Direction, List<BlockPos>> SEARCH_POSITIONS;
    protected static final Vec3i size = new Vec3i(15, 21, 15);
    protected List<JigsawFinder> finders = new ArrayList<>();

    public OutpostFinder(World world, ChunkPos chunkPos) {
        super(world, chunkPos);

        for (Direction direction : Direction.Type.HORIZONTAL) {
            JigsawFinder finder = new JigsawFinder(world, chunkPos, direction, size);

            finder.searchPositions = SEARCH_POSITIONS.get(direction);

            buildStructure(finder);
            this.finders.add(finder);
        }
    }

    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = JigsawFinder.getSearchPositions(0, 0, 0, 0, size);
        Map<Direction, List<BlockPos>> additional = JigsawFinder.getSearchPositions(0, 0, 1, 0, size);
        for (Direction direction : Direction.Type.HORIZONTAL) {
            SEARCH_POSITIONS.get(direction).addAll(additional.get(direction));
        }

    }

    public static List<Finder> create(World world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new OutpostFinder(world, chunkPos));
        finders.add(new OutpostFinder(world, new ChunkPos(chunkPos.x, chunkPos.z + 1)));
        finders.add(new OutpostFinder(world, new ChunkPos(chunkPos.x + 1, chunkPos.z)));
        finders.add(new OutpostFinder(world, new ChunkPos(chunkPos.x + 1, chunkPos.z + 1)));
        return finders;
    }

    private void buildStructure(JigsawFinder finder) {
        BlockState chest = Blocks.CHEST.getDefaultState();
        BlockState birchwood = Blocks.BIRCH_PLANKS.getDefaultState();

        finder.addBlock(chest, 9, 14, 10);

        finder.fillWithOutline(4, 0, 4, 10, 0, 10, birchwood, birchwood, false);
    }

    @Override
    public List<BlockPos> findInChunk() {
        Biome biome = this.world.getBiome(this.chunkPos.getBlockPos(8, 64, 8)).value();
        if (!Features.PILLAGER_OUTPOST.isValidBiome(BiomeFixer.swap(biome))) return new ArrayList<>();

        Map<JigsawFinder, List<BlockPos>> result = this.findInChunkPieces();
        List<BlockPos> combinedResult = new ArrayList<>();

        result.forEach((pieceFinder, positions) -> {

            combinedResult.addAll(positions);

            positions.forEach(pos -> {
                RegionStructure.Data<?> data = Features.PILLAGER_OUTPOST.at(this.chunkPos.x, this.chunkPos.z);

                FoxySeedCracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_LIFTING);
                this.cuboids.add(new Cuboid(pos, pieceFinder.getLayout(), 0xFFAA5403)); // Brownish
                this.cuboids.add(new Cuboid(chunkPos.getStartPos().add(0, pos.getY(), 0), 0xFFAA5403));
            });
        });

        return combinedResult;
    }

    public Map<JigsawFinder, List<BlockPos>> findInChunkPieces() {
        Map<JigsawFinder, List<BlockPos>> result = new HashMap<>();

        this.finders.forEach(pieceFinder -> {
            result.put(pieceFinder, pieceFinder.findInChunk());
        });

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }

}
