package com.foxyclient.module.world;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.EntityListSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

public class AutoMobFarm extends Module {
    private final EntityListSetting targetEntities = addSetting(new EntityListSetting("Targets", "Mobs to target in Farm mode"));
    private final NumberSetting attackRange = addSetting(new NumberSetting("Range", "Attack Range", 4.0, 1.0, 6.0));
    private final BoolSetting silent = addSetting(new BoolSetting("Silent", "Attack invisibly?", false));

    private Entity currentTarget = null;
    private int ticksIdle = 0;

    public AutoMobFarm() {
        super("AutoMobFarm", "Automatically walk to and kill targeted entities", Category.WORLD);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        currentTarget = null;
        ticksIdle = 0;
    }

    @Override
    public void onDisable() {
        if (FoxyClient.INSTANCE.getPathFinder() != null) {
            FoxyClient.INSTANCE.getPathFinder().cancelAll();
        }
        currentTarget = null;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (currentTarget == null || !currentTarget.isAlive() || currentTarget.isRemoved()) {
            findNewTarget();
        }

        if (currentTarget != null) {
            double dist = mc.player.distanceTo(currentTarget);
            if (dist <= attackRange.get()) {
                if (mc.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                    mc.interactionManager.attackEntity(mc.player, currentTarget);
                    if (!silent.get()) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                }
            } else {
                if (!FoxyClient.INSTANCE.getPathFinder().isAnyProcessActive()) {
                    FoxyClient.INSTANCE.getPathFinder().follow(currentTarget);
                }
            }
        }
    }

    private void findNewTarget() {
        currentTarget = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            if (e instanceof LivingEntity && e.isAlive() && targetEntities.contains(e.getType())) {
                double d = mc.player.distanceTo(e);
                if (d < nearestDist && d < 128) {
                    nearestDist = d;
                    currentTarget = e;
                }
            }
        }
        if (currentTarget != null) {
            FoxyClient.INSTANCE.getPathFinder().follow(currentTarget);
            info("Targeting " + currentTarget.getName().getString());
        } else {
            // Cancel any leftover pathing if our target just disappeared and there are none left
            if (FoxyClient.INSTANCE.getPathFinder().isAnyProcessActive()) {
                FoxyClient.INSTANCE.getPathFinder().cancelAll();
            }
        }
    }
}
