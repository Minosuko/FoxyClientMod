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
    private final BoolSetting autoCollect = addSetting(new BoolSetting("AutoCollect", "Auto-collect dropped items?", true));
    private final BoolSetting autoWeapon = addSetting(new BoolSetting("AutoWeapon", "Auto-switch to best weapon?", true));

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
            if (currentTarget instanceof net.minecraft.entity.ItemEntity) {
                if (dist > 1.5) {
                    if (!FoxyClient.INSTANCE.getPathFinder().isAnyProcessActive()) {
                        FoxyClient.INSTANCE.getPathFinder().follow(currentTarget);
                    }
                } else {
                    if (FoxyClient.INSTANCE.getPathFinder().isAnyProcessActive()) {
                        FoxyClient.INSTANCE.getPathFinder().cancelAll();
                    }
                }
            } else {
                if (dist <= attackRange.get()) {
                    equipBestWeapon();
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
    }

    private void findNewTarget() {
        currentTarget = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            
            if (autoCollect.get() && e instanceof net.minecraft.entity.ItemEntity && !e.isRemoved()) {
                double d = mc.player.distanceTo(e);
                if (d < nearestDist && d < 128) {
                    nearestDist = d;
                    currentTarget = e;
                }
            } else if (e instanceof LivingEntity && e.isAlive() && targetEntities.contains(e.getType())) {
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

    private void equipBestWeapon() {
        if (!autoWeapon.get()) return;
        
        int bestSlot = -1;
        double bestScore = -1.0;
        
        for (int i = 0; i < 9; i++) {
            net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                double score = 0;
                String name = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();
                
                if (name.contains("sword")) {
                    score = 10.0;
                } else if (name.contains("axe")) {
                    score = 5.0;
                } else if (stack.get(net.minecraft.component.DataComponentTypes.WEAPON) != null) {
                    score = 1.0;
                }
                
                if (score > 0) {
                    if (name.contains("netherite")) score += 4.0;
                    else if (name.contains("diamond")) score += 3.0;
                    else if (name.contains("iron")) score += 2.0;
                    else if (name.contains("stone")) score += 1.0;
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestSlot = i;
                    }
                }
            }
        }
        
        if (bestSlot != -1 && mc.player.getInventory().selectedSlot != bestSlot) {
            mc.player.getInventory().selectedSlot = bestSlot;
        }
    }
}
