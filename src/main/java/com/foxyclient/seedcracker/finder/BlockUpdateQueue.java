package com.foxyclient.seedcracker.finder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class BlockUpdateQueue {
    private final Queue<Task> blocksAndAction = new LinkedList<>();
    private final HashSet<BlockPos> alreadyChecked = new HashSet<>();

    public boolean add(ArrayList<BlockPos> blockPoses, BlockPos originPos, Thread operationAtEnd) {
        if (alreadyChecked.add(originPos)) {
            blocksAndAction.add(new Task(operationAtEnd, blockPoses));
            return true;
        }
        return false;
    }

    public void tick() {
        if (blocksAndAction.isEmpty()) return;

        Task current = blocksAndAction.peek();
        ArrayList<BlockPos> currentBlocks = current.blocks;
        for (int i = 0; i < 5; i++) {
            if (currentBlocks.isEmpty()) {
                current.thread.start();
                blocksAndAction.remove();
                if (blocksAndAction.isEmpty()) {
                    return;
                } else {
                    current = blocksAndAction.peek();
                    currentBlocks = current.blocks;
                }
            }
            if (MinecraftClient.getInstance().getNetworkHandler() == null) {
                blocksAndAction.clear();
                return;
            }
            PlayerActionC2SPacket p = new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                currentBlocks.remove(0),
                Direction.DOWN
            );
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(p);
        }
    }

    private static class Task {
        public final Thread thread;
        public final ArrayList<BlockPos> blocks;

        public Task(Thread thread, ArrayList<BlockPos> blocks) {
            this.thread = thread;
            this.blocks = blocks;
        }
    }
}
