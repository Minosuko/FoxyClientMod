package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.Set;

public class AutoBreed extends Module {

    private final NumberSetting distance = addSetting(new NumberSetting("Distance", "Max distance to breed", 4.5, 1.0, 6.0));

    private final Set<Integer> bredEntities = new HashSet<>();
    private int cooldown = 0;

    public AutoBreed() {
        super("AutoBreed", "Automatically feeds animals to breed them", Category.WORLD);
    }

    @Override
    public void onEnable() {
        bredEntities.clear();
        cooldown = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof AnimalEntity animal && !animal.isBaby()) {
                if (bredEntities.contains(animal.getId())) continue;

                if (mc.player.squaredDistanceTo(animal) <= distance.get() * distance.get()) {
                    boolean mainHandCanBreed = animal.isBreedingItem(mc.player.getMainHandStack());
                    boolean offHandCanBreed = animal.isBreedingItem(mc.player.getOffHandStack());

                    if (mainHandCanBreed || offHandCanBreed) {
                        Hand hand = mainHandCanBreed ? Hand.MAIN_HAND : Hand.OFF_HAND;
                        
                        mc.interactionManager.interactEntity(mc.player, animal, hand);
                        mc.player.swingHand(hand);
                        
                        bredEntities.add(animal.getId());
                        cooldown = 5;
                        return;
                    }
                }
            }
        }
    }
}
