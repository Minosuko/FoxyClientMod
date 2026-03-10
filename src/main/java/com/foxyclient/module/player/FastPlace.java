package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * FastPlace - Removes or reduces the right-click delay for instant block/item placement.
 * Supports configurable delay, fast crystals, fast XP bottles, and fast use for all items.
 */
public class FastPlace extends Module {
    private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Place delay in ticks", 0, 0, 4));
    private final BoolSetting blocks = addSetting(new BoolSetting("Blocks", "Fast block placing", true));
    private final BoolSetting crystals = addSetting(new BoolSetting("Crystals", "Fast end crystal placing", true));
    private final BoolSetting xpBottles = addSetting(new BoolSetting("XPBottles", "Fast XP bottle throwing", true));
    private final BoolSetting fireworks = addSetting(new BoolSetting("Fireworks", "Fast firework use", true));
    private final BoolSetting all = addSetting(new BoolSetting("All", "Fast use for all items", false));

    private int timer = 0;

    public FastPlace() {
        super("FastPlace", "Remove right-click delay", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        ItemStack mainHand = mc.player.getMainHandStack();
        Item item = mainHand.getItem();

        boolean shouldFastPlace = false;

        if (all.get()) {
            shouldFastPlace = true;
        } else {
            if (blocks.get() && item instanceof BlockItem) shouldFastPlace = true;
            if (crystals.get() && item == Items.END_CRYSTAL) shouldFastPlace = true;
            if (xpBottles.get() && item == Items.EXPERIENCE_BOTTLE) shouldFastPlace = true;
            if (fireworks.get() && item == Items.FIREWORK_ROCKET) shouldFastPlace = true;
        }

        if (!shouldFastPlace) return;

        // Fast place logic: when right-click is held, bypass the cooldown
        if (mc.options.useKey.isPressed()) {
            if (timer <= 0) {
                // Trigger placement
                if (mc.crosshairTarget instanceof BlockHitResult bhr && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    if (item instanceof BlockItem || item == Items.END_CRYSTAL) {
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                }
                if (item == Items.EXPERIENCE_BOTTLE || item == Items.FIREWORK_ROCKET) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                timer = delay.get().intValue();
            } else {
                timer--;
            }
        } else {
            timer = 0;
        }
    }

    public int getDelay() {
        return delay.get().intValue();
    }
}
