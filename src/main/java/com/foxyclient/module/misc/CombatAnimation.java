package com.foxyclient.module.misc;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.util.Hand;

/**
 * Combat Animation - visual enhancements for combat.
 * - Sword waving: continuously wave the held item when right-click is held and KillAura is active.
 * - No swing: suppress the arm swing animation when attacking.
 */
public class CombatAnimation extends Module {

    private final BoolSetting noSwing = addSetting(
        new BoolSetting("NoSwing", "Suppress arm swing animation when attacking", true)
    );

    private final BoolSetting swordWave = addSetting(
        new BoolSetting("SwordWave", "Wave item when holding right click with KillAura", true)
    );

    private final NumberSetting waveSpeed = addSetting(
        new NumberSetting("WaveSpeed", "Speed of item waving animation", 6.0, 1.0, 20.0)
    );

    public CombatAnimation() {
        super("CombatAnimation", "Visual combat animation tweaks", Category.MISC);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        // Sword waving: continuously replay the full swing arc (like the drawing)
        if (shouldSwordWave() && mc.options.useKey.isPressed()) {
            // Sawtooth 0→1 loop: replays the full swing animation continuously
            double time = System.currentTimeMillis() / 1000.0 * waveSpeed.get();
            float progress = (float) (time % 1.0); // 0.0 → 1.0 repeating
            mc.player.handSwingProgress = progress;
            mc.player.lastHandSwingProgress = Math.max(0, progress - 0.1f);
            mc.player.handSwinging = true;
            mc.player.handSwingTicks = (int) (progress * 6); // keep swing state active
        }
    }

    public boolean shouldNoSwing() {
        return isEnabled() && noSwing.get();
    }

    public boolean shouldSwordWave() {
        if (!isEnabled() || !swordWave.get()) return false;
        Module killAura = FoxyClient.INSTANCE.getModuleManager().getModule("KillAura");
        return killAura != null && killAura.isEnabled();
    }

    public static CombatAnimation get() {
        if (FoxyClient.INSTANCE == null || FoxyClient.INSTANCE.getModuleManager() == null) return null;
        return FoxyClient.INSTANCE.getModuleManager().getModule(CombatAnimation.class);
    }
}
