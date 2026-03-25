package com.foxyclient.pathfinding;

import com.foxybot.api.FoxyBotAPI;
import com.foxybot.api.IFoxyBot;
import com.foxybot.api.pathing.goals.GoalBlock;
import com.foxybot.api.pathing.goals.GoalXZ;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * Lightweight wrapper for FoxyBot (Baritone).
 * Delegates all pathfinding and movement to FoxyBot engine.
 */
public class PathFinder {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public MinecraftClient mc() { return mc; }

    private IFoxyBot getFoxyBot() {
        return FoxyBotAPI.getProvider().getPrimaryFoxyBot();
    }

    /** Navigate to a specific position. */
    public void pathTo(BlockPos target) {
        getFoxyBot().getCustomGoalProcess().setGoalAndPath(new GoalBlock(target));
    }

    /** Navigate to XZ coordinates (any Y). */
    public void pathToXZ(int x, int z) {
        getFoxyBot().getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
    }

    /** Follow a specific entity by filter. */
    @SuppressWarnings("unchecked")
    public void follow(Object target) {
        getFoxyBot().getFollowProcess().follow(entity -> entity == target);
    }

    /** Mine blocks by their registry name. */
    public void mine(String blockName) {
        getFoxyBot().getMineProcess().mineByName(blockName);
    }

    /** Mine multiple blocks by their registry names. */
    public void mineByNames(String... blockNames) {
        getFoxyBot().getMineProcess().mineByName(0, blockNames);
    }

    /** Start exploring from a position. */
    public void explore(int x, int z) {
        getFoxyBot().getExploreProcess().explore(x, z);
    }

    /** Start farming nearby crops. */
    public void farm() {
        getFoxyBot().getFarmProcess().farm();
    }

    /** Cancel all processes. */
    public void cancelAll() {
        getFoxyBot().getPathingBehavior().cancelEverything();
        chatInfo("§cAll tasks cancelled.");
    }

    public void pause() {
        chatInfo("§ePause not supported in this version.");
    }

    public void resume() {
        chatInfo("§aResume not supported in this version.");
    }

    public void stop() {
        cancelAll();
    }

    /** Set the navigation goal without starting pathfinding. */
    public void setGoal(BlockPos goal) {
        getFoxyBot().getCustomGoalProcess().setGoal(new GoalBlock(goal));
    }

    /** Clear the current goal. */
    public void clearGoal() {
        getFoxyBot().getCustomGoalProcess().setGoal(null);
    }

    public void tick() {
        // FoxyBot ticks itself via mixins
    }

    public void chatInfo(String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(
                net.minecraft.text.Text.literal("§7[§6FoxyBot§7] §f" + msg), false
            );
        }
    }

    // ===== Getters for HUD and State =====

    public boolean isActive() {
        return getFoxyBot().getPathingBehavior().isPathing();
    }

    /** Check if any process is running. */
    public boolean isAnyProcessActive() {
        return getFoxyBot().getPathingControlManager().mostRecentInControl().isPresent()
            || getFoxyBot().getPathingBehavior().isPathing()
            || getFoxyBot().getCustomGoalProcess().isActive()
            || getFoxyBot().getMineProcess().isActive()
            || getFoxyBot().getFollowProcess().isActive()
            || getFoxyBot().getExploreProcess().isActive()
            || getFoxyBot().getFarmProcess().isActive();
    }

    public boolean isPaused() {
        return false;
    }

    public String getCurrentProcessName() {
        var active = getFoxyBot().getPathingControlManager().mostRecentInControl();
        return active.isPresent() ? active.get().getClass().getSimpleName() : "Idle";
    }

    public BlockPos getGoalPos() {
        var goal = getFoxyBot().getCustomGoalProcess().getGoal();
        if (goal instanceof GoalBlock g) {
            return g.getGoalPos();
        }
        return null;
    }
}
