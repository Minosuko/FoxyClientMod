package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.item.FishingRodItem;
import net.minecraft.util.Hand;

/**
 * Automatically reels in and recasts fishing rod.
 */
public class AutoFish extends Module {
    private final BoolSetting autoReel = addSetting(new BoolSetting("AutoReel", "Auto reel when fish bites", true));
    private final BoolSetting autoCast = addSetting(new BoolSetting("AutoCast", "Auto recast after catching", true));

    private int castDelay = 0;
    private boolean needRecast = false;

    public AutoFish() {
        super("AutoFish", "Automatic fishing", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (needRecast) {
            castDelay++;
            if (castDelay > 20) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                needRecast = false;
                castDelay = 0;
            }
            return;
        }

        if (mc.player.fishHook != null && mc.player.fishHook.isInOpenWater()) {
            // Check for bite (bobber goes down)
            if (mc.player.fishHook.getVelocity().y < -0.04) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (autoCast.get()) {
                    needRecast = true;
                    castDelay = 0;
                }
            }
        }
    }
}
