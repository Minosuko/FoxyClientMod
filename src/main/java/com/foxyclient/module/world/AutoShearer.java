package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoShearer extends Module {

    private final NumberSetting distance = addSetting(new NumberSetting("Distance", "Max distance to shear", 4.5, 1.0, 6.0));

    // Simple cooldown to prevent spamming packets
    private int cooldown = 0;

    public AutoShearer() {
        super("AutoShearer", "Automatically shears nearby sheep", Category.WORLD);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof SheepEntity sheep && !sheep.isSheared() && !sheep.isBaby()) {
                if (mc.player.squaredDistanceTo(sheep) <= distance.get() * distance.get()) {
                    
                    int shearSlot = -1;
                    for (int i = 0; i < 9; i++) {
                        if (mc.player.getInventory().getStack(i).getItem() == Items.SHEARS) {
                            shearSlot = i;
                            break;
                        }
                    }

                    if (shearSlot != -1) {
                        int previousSlot = mc.player.getInventory().selectedSlot;
                        mc.player.getInventory().selectedSlot = shearSlot;
                        
                        mc.interactionManager.interactEntity(mc.player, sheep, Hand.MAIN_HAND);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        
                        mc.player.getInventory().selectedSlot = previousSlot;
                        
                        cooldown = 5; // Wait 5 ticks before shearing next
                        return; // Shear one sheep per tick
                    }
                }
            }
        }
    }
}
