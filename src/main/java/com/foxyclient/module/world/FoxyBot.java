package com.foxyclient.module.world;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.module.render.OreFinder;
import com.foxyclient.pathfinding.PathFinder;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * FoxyBot Module - Baritone-powered automation.
 * Uses the FoxyBot engine (full Baritone reimplementation at com.foxybot)
 * for Mine, GoTo, Follow, Explore, and Farm modes.
 */
public class FoxyBot extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Baritone action mode",
        "Mine", "Mine", "GoTo", "Follow", "Explore", "Farm"));

    private final BoolSetting autoResume = addSetting(new BoolSetting("AutoResume", "Auto-resume when idle", true));

    private final NumberSetting gotoX = addSetting(new NumberSetting("GoToX", "Target X", 0, -30000000, 30000000));
    private final NumberSetting gotoY = addSetting(new NumberSetting("GoToY", "Target Y (-1=any)", -1, -1, 320));
    private final NumberSetting gotoZ = addSetting(new NumberSetting("GoToZ", "Target Z", 0, -30000000, 30000000));

    private boolean dispatched = false;
    private int idleTicks = 0;

    public FoxyBot() {
        super("FoxyBot", "Baritone-powered automation (Mine, GoTo, Follow, Explore, Farm)", Category.WORLD);
    }

    private PathFinder pathFinder() {
        return FoxyClient.INSTANCE.getPathFinder();
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        dispatched = false;
        idleTicks = 0;
        dispatch();
    }

    @Override
    public void onDisable() {
        PathFinder pf = pathFinder();
        if (pf != null) pf.cancelAll();
        dispatched = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        PathFinder pf = pathFinder();
        if (pf == null) return;

        boolean active = pf.isAnyProcessActive();
        if (!active) {
            idleTicks++;
            if (autoResume.get() && dispatched && idleTicks > 40) {
                dispatch();
                idleTicks = 0;
            }
        } else {
            idleTicks = 0;
        }
    }

    private void dispatch() {
        switch (mode.get()) {
            case "Mine" -> dispatchMine();
            case "GoTo" -> dispatchGoTo();
            case "Follow" -> dispatchFollow();
            case "Explore" -> dispatchExplore();
            case "Farm" -> dispatchFarm();
        }
        dispatched = true;
    }

    private void dispatchMine() {
        OreFinder oreFinder = FoxyClient.INSTANCE.getModuleManager().getModule(OreFinder.class);
        if (oreFinder == null) { error("OreFinder module not found!"); return; }

        List<Block> oreBlocks = oreFinder.getEnabledOreBlocks();
        if (oreBlocks.isEmpty()) { error("No ore types enabled in OreFinder!"); return; }

        String[] blockNames = oreBlocks.stream()
            .map(block -> Registries.BLOCK.getId(block).toString())
            .distinct()
            .toArray(String[]::new);

        pathFinder().mineByNames(blockNames);

        StringBuilder sb = new StringBuilder("§aMining: ");
        for (int i = 0; i < Math.min(blockNames.length, 5); i++) {
            if (i > 0) sb.append(", ");
            String name = blockNames[i];
            int colon = name.indexOf(':');
            if (colon >= 0) name = name.substring(colon + 1);
            sb.append(name);
        }
        if (blockNames.length > 5) sb.append(" +").append(blockNames.length - 5).append(" more");
        info(sb.toString());
    }

    private void dispatchGoTo() {
        int x = gotoX.get().intValue();
        int y = gotoY.get().intValue();
        int z = gotoZ.get().intValue();
        if (y < 0) {
            pathFinder().pathToXZ(x, z);
            info("§aNavigating to X=" + x + " Z=" + z);
        } else {
            pathFinder().pathTo(new BlockPos(x, y, z));
            info("§aNavigating to X=" + x + " Y=" + y + " Z=" + z);
        }
    }

    private void dispatchFollow() {
        PlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            double dist = mc.player.squaredDistanceTo(player);
            if (dist < nearestDist) { nearestDist = dist; nearest = player; }
        }
        if (nearest != null) {
            pathFinder().follow(nearest);
            info("§aFollowing " + nearest.getName().getString());
        } else {
            error("No players found to follow!");
        }
    }

    private void dispatchExplore() {
        BlockPos pos = mc.player.getBlockPos();
        pathFinder().explore(pos.getX(), pos.getZ());
        info("§aExploring from current position...");
    }

    private void dispatchFarm() {
        pathFinder().farm();
        info("§aFarming nearby crops...");
    }
}
