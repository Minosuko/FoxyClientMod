package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;

/**
 * Disables various rendering effects for better visibility/FPS.
 */
public class NoRender extends Module {
    private final BoolSetting fog = addSetting(new BoolSetting("Fog", "Disable fog", true));
    private final BoolSetting weather = addSetting(new BoolSetting("Weather", "Disable weather particles", true));
    private final BoolSetting fire = addSetting(new BoolSetting("Fire", "Disable fire overlay", true));
    private final BoolSetting water = addSetting(new BoolSetting("Water", "Disable water overlay", true));
    private final BoolSetting hurtCam = addSetting(new BoolSetting("HurtCam", "Disable hurt camera shake", true));
    private final BoolSetting pumpkin = addSetting(new BoolSetting("Pumpkin", "Disable pumpkin overlay", true));
    private final BoolSetting blindness = addSetting(new BoolSetting("Blindness", "Disable blindness effect", true));
    private final BoolSetting totem = addSetting(new BoolSetting("TotemAnim", "Disable totem animation", false));
    private final BoolSetting particles = addSetting(new BoolSetting("Particles", "Reduce particles", false));
    private final BoolSetting armor = addSetting(new BoolSetting("Armor", "Hide own armor", false));
    private final BoolSetting hideItems = addSetting(new BoolSetting("HideItems", "Hide dropped items", false));

    public NoRender() {
        super("NoRender", "Disable visual effects", Category.RENDER);
    }

    public boolean noFog() { return isEnabled() && fog.get(); }
    public boolean noWeather() { return isEnabled() && weather.get(); }
    public boolean noFire() { return isEnabled() && fire.get(); }
    public boolean noWater() { return isEnabled() && water.get(); }
    public boolean noHurtCam() { return isEnabled() && hurtCam.get(); }
    public boolean noPumpkin() { return isEnabled() && pumpkin.get(); }
    public boolean noBlindness() { return isEnabled() && blindness.get(); }
    public boolean noTotem() { return isEnabled() && totem.get(); }
    public boolean noParticles() { return isEnabled() && particles.get(); }
    public boolean noArmor() { return isEnabled() && armor.get(); }
    public boolean noItems() { return isEnabled() && hideItems.get(); }
}
