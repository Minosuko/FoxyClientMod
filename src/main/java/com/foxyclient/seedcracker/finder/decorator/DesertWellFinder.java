package com.foxyclient.seedcracker.finder.decorator;

import com.foxyclient.seedcracker.Features;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.cracker.DataAddedEvent;
import com.foxyclient.seedcracker.finder.Finder;
import com.foxyclient.seedcracker.finder.structure.PieceFinder;
import com.foxyclient.seedcracker.util.BiomeFixer;
import com.seedfinding.mcfeature.decorator.DesertWell;
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
import java.util.List;

public class DesertWellFinder extends PieceFinder {

    protected static Vec3i SIZE = new Vec3i(5, 6, 5);

    public DesertWellFinder(World world, ChunkPos chunkPos) {
        super(world, chunkPos, Direction.NORTH, SIZE);
        this.searchPositions = CHUNK_POSITIONS;
        this.buildStructure();
    }

    public static List<Finder> create(World world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new DesertWellFinder(world, chunkPos));

        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x - 1, chunkPos.z)));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x, chunkPos.z - 1)));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x - 1, chunkPos.z - 1)));

        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x + 1, chunkPos.z)));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x, chunkPos.z + 1)));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x + 1, chunkPos.z + 1)));

        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x - 1, chunkPos.z - 1)));
        finders.add(new DesertWellFinder(world, new ChunkPos(chunkPos.x - 1, chunkPos.z + 1)));
        return finders;
    }

    @Override
    public List<BlockPos> findInChunk() {
        Biome biome = this.world.getBiome(this.chunkPos.getBlockPos(8, 64, 8)).value();

        if (!Features.DESERT_WELL.isValidBiome(BiomeFixer.swap(biome))) {
            return new ArrayList<>();
        }

        List<BlockPos> result = super.findInChunk();

        result.forEach(pos -> {
            BlockPos centralPos = pos.add(2, 1, 2);

            DesertWell.Data data = Features.DESERT_WELL.at(centralPos.getX(), centralPos.getZ());

            if (FoxySeedCracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
                this.cuboids.add(new com.foxyclient.seedcracker.render.Cuboid(pos, SIZE, 0xFF8080FF));
                this.cuboids.add(new com.foxyclient.seedcracker.render.Cuboid(centralPos, 0xFF8080FF));
            }
        });

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }

    protected void buildStructure() {
        BlockState sandstone = Blocks.SANDSTONE.getDefaultState();
        BlockState sandstoneSlab = Blocks.SANDSTONE_SLAB.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();

        this.fillWithOutline(0, 0, 0, 4, 1, 4, sandstone, sandstone, false);
        this.fillWithOutline(1, 5, 1, 3, 5, 3, sandstoneSlab, sandstoneSlab, false);
        this.addBlock(sandstone, 2, 5, 2);

        BlockPos p1 = new BlockPos(2, 1, 2);
        this.addBlock(water, p1.getX(), p1.getY(), p1.getZ());

        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockPos p2 = p1.offset(facing);
            this.addBlock(water, p2.getX(), p2.getY(), p2.getZ());
        }
    }

}
