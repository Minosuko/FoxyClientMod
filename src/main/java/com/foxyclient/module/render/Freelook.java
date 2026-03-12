package com.foxyclient.module.render;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Freelook - Rotate camera independently from player body.
 */
public class Freelook extends Module {
    private final NumberSetting sensitivity = addSetting(new NumberSetting("Sensitivity", "Look sensitivity", 1.0, 0.1, 3.0));
    private final BoolSetting f5Mode = addSetting(new BoolSetting("F5Mode", "Third person while active", true));

    private float cameraYaw, cameraPitch;
    private int savedPerspective;

    public Freelook() {
        super("Freelook", "Free camera rotation (F5 style)", Category.RENDER, GLFW.GLFW_KEY_V);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        cameraYaw = mc.player.getYaw();
        cameraPitch = mc.player.getPitch();
        savedPerspective = mc.options.getPerspective().ordinal();
        if (f5Mode.get()) {
            mc.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
        }
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.options.setPerspective(net.minecraft.client.option.Perspective.values()[savedPerspective]);
    }

    public void updateCamera(double deltaX, double deltaY) {
        if (!isEnabled()) return;
        
        // sensitivity adjustment - use Minecraft's mouse sensitivity as base if possible,
        // but for now, the setting + constant is fine.
        cameraYaw += (float) (deltaX * sensitivity.get() * 0.5);
        cameraPitch += (float) (deltaY * sensitivity.get() * 0.5); 
        
        cameraPitch = Math.max(-90, Math.min(90, cameraPitch));
    }

    public float getCameraYaw() { return cameraYaw; }
    public float getCameraPitch() { return cameraPitch; }

    public static Freelook get() {
        return FoxyClient.INSTANCE.getModuleManager().getModule(Freelook.class);
    }
}
