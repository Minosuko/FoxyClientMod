package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.setting.NumberSetting;
import java.awt.*;

/**
 * Shows entity hitboxes with configurable colors.
 */
public class Hitboxes extends Module {
    private final BoolSetting players = addSetting(new BoolSetting("Players", "Show player hitboxes", true));
    private final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Show mob hitboxes", false));
    private final BoolSetting items = addSetting(new BoolSetting("Items", "Show item hitboxes", false));
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Hitbox color", new Color(255, 255, 255, 150)));
    private final NumberSetting lineWidth = addSetting(new NumberSetting("LineWidth", "Line width", 1.5, 0.5, 5.0));

    public Hitboxes() {
        super("Hitboxes", "Show entity hitboxes", Category.RENDER);
    }

    public boolean showPlayers() { return isEnabled() && players.get(); }
    public boolean showMobs() { return isEnabled() && mobs.get(); }
    public boolean showItems() { return isEnabled() && items.get(); }
    public Color getColor() { return color.get(); }
    public float getLineWidth() { return lineWidth.get().floatValue(); }
}
