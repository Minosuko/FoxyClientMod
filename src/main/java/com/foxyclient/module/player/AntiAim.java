package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RotationManager;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

/**
 * Desyncs rotations to make the player harder to hit.
 */
public class AntiAim extends Module {
    private final ModeSetting yawMode = addSetting(new ModeSetting("Yaw Mode", "Horizontal rotation mode", "None", "None", "Backward", "Sideways", "Spin", "Jitter", "Static", "Random"));
    private final NumberSetting yawAdd = addSetting(new NumberSetting("Yaw Add", "Additional yaw offset", 0, -180, 180));
    private final ModeSetting pitchMode = addSetting(new ModeSetting("Pitch Mode", "Vertical rotation mode", "None", "None", "Down", "Up", "Zero", "Jitter", "Static", "Random"));
    private final NumberSetting pitchAdd = addSetting(new NumberSetting("Pitch Add", "Additional pitch offset", 0, -90, 90));
    
    private final NumberSetting spinSpeed = addSetting(new NumberSetting("Spin Speed", "Speed for Spin mode", 10.0, 1.0, 50.0));
    private final NumberSetting jitterRange = addSetting(new NumberSetting("Jitter Range", "Range for Jitter mode", 30.0, 1.0, 180.0));

    private float spinYaw = 0;
    private final Random random = new Random();

    public AntiAim() {
        super("AntiAim", "Rotation desync", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        float targetYaw = mc.player.getYaw();
        float targetPitch = mc.player.getPitch();

        // Yaw Logic
        switch (yawMode.get()) {
            case "Backward" -> targetYaw += 180;
            case "Sideways" -> targetYaw += 90;
            case "Spin" -> {
                spinYaw += spinSpeed.get().floatValue();
                targetYaw = spinYaw;
            }
            case "Jitter" -> {
                targetYaw += (random.nextBoolean() ? jitterRange.get().floatValue() : -jitterRange.get().floatValue());
            }
            case "Static" -> targetYaw = yawAdd.get().floatValue();
            case "Random" -> targetYaw = random.nextFloat() * 360 - 180;
        }

        if (!yawMode.get().equals("Static") && !yawMode.get().equals("Spin") && !yawMode.get().equals("Random") && !yawMode.get().equals("None")) {
            targetYaw += yawAdd.get().floatValue();
        }

        // Pitch Logic
        switch (pitchMode.get()) {
            case "Down" -> targetPitch = 90;
            case "Up" -> targetPitch = -90;
            case "Zero" -> targetPitch = 0;
            case "Jitter" -> {
                targetPitch += (random.nextBoolean() ? jitterRange.get().floatValue() * 0.5f : -jitterRange.get().floatValue() * 0.5f);
            }
            case "Static" -> targetPitch = pitchAdd.get().floatValue();
            case "Random" -> targetPitch = random.nextFloat() * 180 - 90;
        }

        if (!pitchMode.get().equals("Static") && !pitchMode.get().equals("Down") && !pitchMode.get().equals("Up") && !pitchMode.get().equals("Zero") && !pitchMode.get().equals("Random") && !pitchMode.get().equals("None")) {
            targetPitch += pitchAdd.get().floatValue();
        }

        targetYaw = MathHelper.wrapDegrees(targetYaw);
        targetPitch = MathHelper.clamp(targetPitch, -90, 90);

        if (!yawMode.get().equals("None") || !pitchMode.get().equals("None")) {
            RotationManager.setRotation(targetYaw, targetPitch, true, RotationManager.Priority.LOW);
        }
    }
}
