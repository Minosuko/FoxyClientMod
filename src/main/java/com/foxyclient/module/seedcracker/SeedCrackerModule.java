package com.foxyclient.module.seedcracker;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.config.Config;
import com.foxyclient.seedcracker.finder.FinderQueue;
import com.foxyclient.seedcracker.render.Cuboid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class SeedCrackerModule extends Module {

    public SeedCrackerModule() {
        super("SeedCracker", "Port of SeedCrackerX for finding world seeds.", Category.WORLD);
    }

    public void setHashedSeed(long seed) {
        FoxySeedCracker.get().getDataStorage().addHashedSeed(seed);
    }

    @Override
    public void onEnable() {
        Config.get().active = true;
        if (FoxySeedCracker.get() != null) {
            FoxySeedCracker.get().registerApi(seed -> {
                // Callback when seed is found
                MinecraftClient.getInstance().execute(() -> {
                    // Handle found seed (e.g. notification)
                });
            });
        }
    }

    @Override
    public void onDisable() {
        Config.get().active = false;
        FoxySeedCracker.get().reset();
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

        List<Cuboid> cuboids = FinderQueue.get().getRenderers();
        for (Cuboid cuboid : cuboids) {
            // Only render cuboids within 256 blocks
            if (cuboid.getPos().isWithinDistance(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()), 256)) {
                cuboid.render(event.getMatrices(), event.getVertexConsumers(), cameraPos);
            }
        }
    }
}
