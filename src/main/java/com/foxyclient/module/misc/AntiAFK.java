package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.util.PlayerInput;
import java.util.Random;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;

/**
 * Prevents AFK kick by performing random actions.
 */
public class AntiAFK extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Anti-AFK mode", "Spin", "Spin", "Jump", "Walk"));
    private final NumberSetting interval = addSetting(new NumberSetting("Interval", "Action interval (ticks)", 40, 10, 200));

    private int timer = 0;

    public AntiAFK() {
        super("AntiAFK", "Prevent AFK kick", Category.MISC);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        timer++;
        if (timer < interval.get()) return;
        timer = 0;

        switch (mode.get()) {
            case "Spin" -> mc.player.setYaw(mc.player.getYaw() + 15);
            case "Jump" -> { if (mc.player.isOnGround()) mc.player.jump(); }
            case "Walk" -> {
                mc.player.setYaw(mc.player.getYaw() + (float) (Math.random() * 30 - 15));
                mc.player.input.playerInput = new PlayerInput(true, mc.player.input.playerInput.backward(), mc.player.input.playerInput.left(), mc.player.input.playerInput.right(), mc.player.input.playerInput.jump(), mc.player.input.playerInput.sneak(), mc.player.input.playerInput.sprint());
            }
        }
    }
}
