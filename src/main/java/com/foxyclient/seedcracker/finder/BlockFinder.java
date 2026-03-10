package com.foxyclient.seedcracker.finder;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BlockFinder extends Finder {

    private final Set<BlockState> targetBlockStates = new HashSet<>();
    protected List<BlockPos> searchPositions = new ArrayList<>();

    public BlockFinder(World world, ChunkPos chunkPos, Block block) {
        super(world, chunkPos);
        this.targetBlockStates.addAll(block.getStateManager().getStates());
    }

    public BlockFinder(World world, ChunkPos chunkPos, Block... blocks) {
        super(world, chunkPos);
        for (Block block : blocks) {
            this.targetBlockStates.addAll(block.getStateManager().getStates());
        }
    }

    public BlockFinder(World world, ChunkPos chunkPos, BlockState... blockStates) {
        super(world, chunkPos);
        this.targetBlockStates.addAll(Arrays.asList(blockStates));
    }

    @Override
    public List<BlockPos> findInChunk() {
        List<BlockPos> result = new ArrayList<>();
        Chunk chunk = this.world.getChunk(this.chunkPos.x, this.chunkPos.z);

        for (BlockPos blockPos : this.searchPositions) {
            // blockPos in searchPositions are local (0-15), so we use getBlockState(x, y, z) on chunk
            // Wait, upstream searchPositions are global or local?
            // Upstream findInChunk: BlockState currentState = chunk.getBlockState(blockPos);
            // and then: result.add(this.chunkPos.getWorldPosition().offset(blockPos));
            
            // In Yarn: chunk.getBlockState(BlockPos) takes a block pos.
            // If blockPos is local, we need to be careful.
            
            BlockState currentState = chunk.getBlockState(blockPos);

            if (this.targetBlockStates.contains(currentState)) {
                result.add(this.chunkPos.getBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            }
        }

        return result;
    }

}
