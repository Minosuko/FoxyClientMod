package com.foxyclient.seedcracker.finder.decorator;

import com.foxyclient.seedcracker.Features;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.cracker.DataAddedEvent;
import com.foxyclient.seedcracker.finder.BlockFinder;
import com.foxyclient.seedcracker.finder.Finder;
import com.foxyclient.seedcracker.render.Cuboid;
import com.foxyclient.seedcracker.util.BiomeFixer;
import com.seedfinding.mcfeature.decorator.EndGateway;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

public class EndGatewayFinder extends BlockFinder {

    public EndGatewayFinder(World world, ChunkPos chunkPos) {
        super(world, chunkPos, Blocks.END_GATEWAY);
        this.searchPositions = CHUNK_POSITIONS;
    }

    public static List<Finder> create(World world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new EndGatewayFinder(world, chunkPos));
        return finders;
    }

    @Override
    public List<BlockPos> findInChunk() {
        Biome biome = this.world.getBiome(this.chunkPos.getBlockPos(8, 64, 8)).value();
        if (!Features.END_GATEWAY.isValidBiome(BiomeFixer.swap(biome))) return new ArrayList<>();

        List<BlockPos> result = super.findInChunk();
        List<BlockPos> newResult = new ArrayList<>();

        result.forEach(pos -> {
            int height = this.findHeight(pos);

            if (height >= 3 && height <= 9) {
                newResult.add(pos);

                EndGateway.Data data = Features.END_GATEWAY.at(pos.getX(), pos.getZ(), height);

                if (FoxySeedCracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
                    this.cuboids.add(new Cuboid(pos.add(-1, -2, -1), pos.add(2, 3, 2), 0xFF6666D2));
                }
            }
        });

        return newResult;
    }

    private int findHeight(BlockPos pos) {
        int height = 0;

        while (pos.getY() >= 0) {
            pos = pos.down();
            height++;

            BlockState state = this.world.getBlockState(pos);

            //Bedrock generates below gateways.
            if (state.getBlock() == Blocks.BEDROCK || state.getBlock() != Blocks.END_STONE) {
                continue;
            }

            break;
        }

        return height - 1;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isEnd(dimension);
    }

}
