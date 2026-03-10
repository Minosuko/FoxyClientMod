package com.foxyclient.seedcracker.config;

import com.foxyclient.FoxyClient;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.seedfinding.mccore.version.MCVersion;
import com.foxyclient.seedcracker.cracker.HashedSeedData;
import com.foxyclient.seedcracker.cracker.storage.DataStorage;
import com.foxyclient.seedcracker.cracker.storage.TimeMachine;
import com.foxyclient.seedcracker.finder.Finder;
import com.foxyclient.seedcracker.util.FeatureToggle;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SeedCracker GUI redesigned to match FoxyClient ClickGUI aesthetic.
 */
public class SeedCrackerScreen extends Screen {
    private static final int PANEL_WIDTH = 140;
    private static final int HEADER_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 14;
    private static final int PADDING = 4;

    private final List<Panel> panels = new ArrayList<>();
    private Panel draggingPanel = null;
    private int dragOffsetX, dragOffsetY;
    private final Screen parent;

    public SeedCrackerScreen(Screen parent) {
        super(Text.literal("SeedCracker"));
        this.parent = parent;
        
        int x = 10;
        int y = 10;
        
        // Panel 1: Main Settings
        panels.add(new Panel("Settings", x, y, Panel.Type.SETTINGS));
        x += PANEL_WIDTH + 10;
        
        // Panel 2: Finders (Structures)
        panels.add(new Panel("Structures", x, y, Panel.Type.FINDERS_STRUCTURES));
        x += PANEL_WIDTH + 10;
        
        // Panel 3: Finders (Decorators & Biomes)
        panels.add(new Panel("Other Finders", x, y, Panel.Type.FINDERS_OTHER));
        x += PANEL_WIDTH + 10;
        
        // Panel 4: Progress & Results
        panels.add(new Panel("Progress", x, y, Panel.Type.PROGRESS));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim background
        context.fill(0, 0, width, height, 0x88000000);

        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        for (Panel panel : panels) {
            if (panel.mouseClicked(click, bl)) return true;

            // Header drag
            if (mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH &&
                mouseY >= panel.y && mouseY <= panel.y + HEADER_HEIGHT) {
                draggingPanel = panel;
                dragOffsetX = (int) (mouseX - panel.x);
                dragOffsetY = (int) (mouseY - panel.y);
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPanel = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingPanel != null) {
            draggingPanel.x = (int) (click.x() - dragOffsetX);
            draggingPanel.y = (int) (click.y() - dragOffsetY);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        Config.save();
        this.client.setScreen(this.parent);
    }

    private static class Panel {
        enum Type { SETTINGS, FINDERS_STRUCTURES, FINDERS_OTHER, PROGRESS }
        
        final String title;
        final Type type;
        int x, y;
        boolean expanded = true;
        int scroll = 0;

        Panel(String title, int x, int y, Type type) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.type = type;
        }

        void render(DrawContext context, int mouseX, int mouseY) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            
            // Header
            context.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, 0xFF1A1A2E);
            context.fill(x, y, x + PANEL_WIDTH, y + 2, 0xFFFF6600);
            context.drawTextWithShadow(tr, title, x + PADDING, y + 6, 0xFFFF6600);
            
            String icon = expanded ? "▲" : "▼";
            context.drawTextWithShadow(tr, icon, x + PANEL_WIDTH - 12, y + 6, 0xFFFF6600);

            if (!expanded) return;

            int currY = y + HEADER_HEIGHT;
            Config config = Config.get();
            DataStorage storage = FoxySeedCracker.get().getDataStorage();
            TimeMachine tm = storage.getTimeMachine();

            switch (type) {
                case SETTINGS -> {
                    currY = renderToggle(context, mouseX, mouseY, currY, "Active", config.active, val -> config.active = val);
                    currY = renderToggle(context, mouseX, mouseY, currY, "Database", config.databaseSubmits, val -> config.databaseSubmits = val);
                    currY = renderToggle(context, mouseX, mouseY, currY, "Anonymous", config.anonymusSubmits, val -> config.anonymusSubmits = val);
                    currY = renderToggle(context, mouseX, mouseY, currY, "Anti-Xray", config.antiXrayBypass, val -> config.antiXrayBypass = val);
                    currY = renderCycle(context, mouseX, mouseY, currY, "Render", config.render.name(), () -> {
                        int idx = (config.render.ordinal() + 1) % Config.RenderType.values().length;
                        config.render = Config.RenderType.values()[idx];
                    });
                    currY = renderLabel(context, currY, "Vers: " + config.getVersion().name);
                    currY = renderButton(context, mouseX, mouseY, currY, "§cClear Data", () -> FoxySeedCracker.get().reset());
                }
                case FINDERS_STRUCTURES -> {
                    for (Finder.Type f : Finder.Type.getForCategory(Finder.Category.STRUCTURES)) {
                        currY = renderToggle(context, mouseX, mouseY, currY, formatName(f.nameKey), f.enabled.get(), val -> f.enabled.set(val));
                    }
                }
                case FINDERS_OTHER -> {
                    for (Finder.Type f : Finder.Type.getForCategory(Finder.Category.DECORATORS)) {
                        currY = renderToggle(context, mouseX, mouseY, currY, formatName(f.nameKey), f.enabled.get(), val -> f.enabled.set(val));
                    }
                    for (Finder.Type f : Finder.Type.getForCategory(Finder.Category.BIOMES)) {
                        currY = renderToggle(context, mouseX, mouseY, currY, formatName(f.nameKey), f.enabled.get(), val -> f.enabled.set(val));
                    }
                }
                case PROGRESS -> {
                    double base = storage.getBaseBits();
                    double lift = storage.getLiftingBits();
                    double want = storage.getWantedBits();
                    
                    currY = renderLabel(context, currY, "§7Reg Bits: " + (base >= want ? "§a" : "§e") + String.format("%.1f/%.0f", base, want));
                    currY = renderLabel(context, currY, "§7Lift Bits: " + (lift >= 40 ? "§a" : "§e") + String.format("%.1f/40", lift));
                    
                    if (tm.isRunning) currY = renderLabel(context, currY, "§6⏳ Cracking...");
                    
                    if (!tm.worldSeeds.isEmpty()) {
                        currY = renderLabel(context, currY, "§aSeed Found!");
                        for (long s : tm.worldSeeds) {
                            currY = renderLabel(context, currY, "§f" + s);
                        }
                    } else if (!tm.structureSeeds.isEmpty()) {
                        currY = renderLabel(context, currY, "§eStruc Seeds: " + tm.structureSeeds.size());
                    }
                }
            }
            
            // Bottom border
            context.fill(x, currY, x + PANEL_WIDTH, currY + 1, 0xFF333333);
        }

        int renderToggle(DrawContext context, int mouseX, int mouseY, int yOffset, String name, boolean val, java.util.function.Consumer<Boolean> callback) {
            boolean hover = mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT;
            int bgColor = hover ? 0xFF2A2A3E : 0xFF16162E;
            if (val) bgColor = hover ? 0xFF3A2A1E : 0xFF2A1A0E;
            
            context.fill(x, yOffset, x + PANEL_WIDTH, yOffset + ITEM_HEIGHT, bgColor);
            context.drawTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer, name, x + PADDING, yOffset + 3, val ? 0xFFFF6600 : 0xFFCCCCCC);
            return yOffset + ITEM_HEIGHT;
        }

        int renderCycle(DrawContext context, int mouseX, int mouseY, int yOffset, String name, String val, Runnable action) {
            boolean hover = mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT;
            context.fill(x, yOffset, x + PANEL_WIDTH, yOffset + ITEM_HEIGHT, hover ? 0xFF2A2A3E : 0xFF16162E);
            context.drawTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer, name + ": §e" + val, x + PADDING, yOffset + 3, 0xFFCCCCCC);
            return yOffset + ITEM_HEIGHT;
        }

        int renderLabel(DrawContext context, int yOffset, String text) {
            context.fill(x, yOffset, x + PANEL_WIDTH, yOffset + ITEM_HEIGHT, 0xFF16162E);
            context.drawTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer, text, x + PADDING, yOffset + 3, 0xFFAAAAAA);
            return yOffset + ITEM_HEIGHT;
        }

        int renderButton(DrawContext context, int mouseX, int mouseY, int yOffset, String text, Runnable action) {
            boolean hover = mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT;
            context.fill(x, yOffset, x + PANEL_WIDTH, yOffset + ITEM_HEIGHT, hover ? 0xFF3A1A1E : 0xFF2A0A0E);
            context.drawTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer, text, x + PADDING, yOffset + 3, 0xFFFFFFFF);
            return yOffset + ITEM_HEIGHT;
        }

        boolean mouseClicked(Click click, boolean doubled) {
            double mouseX = click.x();
            double mouseY = click.y();
            int button = click.button();

            if (mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
                if (mouseX >= x + PANEL_WIDTH - 20) {
                    expanded = !expanded;
                    return true;
                }
            }

            if (!expanded) return false;

            int currY = y + HEADER_HEIGHT;
            Config config = Config.get();
            DataStorage storage = FoxySeedCracker.get().getDataStorage();

            // Handle clicks based on current Y offsets
            switch (type) {
                case SETTINGS -> {
                    if (isHovered(mouseX, mouseY, currY)) { config.active = !config.active; return true; } currY += ITEM_HEIGHT;
                    if (isHovered(mouseX, mouseY, currY)) { config.databaseSubmits = !config.databaseSubmits; return true; } currY += ITEM_HEIGHT;
                    if (isHovered(mouseX, mouseY, currY)) { config.anonymusSubmits = !config.anonymusSubmits; return true; } currY += ITEM_HEIGHT;
                    if (isHovered(mouseX, mouseY, currY)) { config.antiXrayBypass = !config.antiXrayBypass; return true; } currY += ITEM_HEIGHT;
                    if (isHovered(mouseX, mouseY, currY)) { 
                        int idx = (config.render.ordinal() + 1) % Config.RenderType.values().length;
                        config.render = Config.RenderType.values()[idx];
                        return true; 
                    } currY += ITEM_HEIGHT;
                    currY += ITEM_HEIGHT; // Label
                    if (isHovered(mouseX, mouseY, currY)) { FoxySeedCracker.get().reset(); return true; } currY += ITEM_HEIGHT;
                }
                case FINDERS_STRUCTURES -> {
                    for (Finder.Type f : Finder.Type.getForCategory(Finder.Category.STRUCTURES)) {
                        if (isHovered(mouseX, mouseY, currY)) { f.enabled.set(!f.enabled.get()); return true; }
                        currY += ITEM_HEIGHT;
                    }
                }
                case FINDERS_OTHER -> {
                    for (Finder.Type f : Finder.Type.getForCategory(Finder.Category.DECORATORS)) {
                        if (isHovered(mouseX, mouseY, currY)) { f.enabled.set(!f.enabled.get()); return true; }
                        currY += ITEM_HEIGHT;
                    }
                    for (Finder.Type f : Finder.Type.getForCategory(Finder.Category.BIOMES)) {
                        if (isHovered(mouseX, mouseY, currY)) { f.enabled.set(!f.enabled.get()); return true; }
                        currY += ITEM_HEIGHT;
                    }
                }
            }

            return false;
        }

        boolean isHovered(double mouseX, double mouseY, int yOffset) {
            return mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT;
        }

        String formatName(String key) {
            String s = key.startsWith("finder.") ? key.substring(7) : key;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (i == 0) sb.append(Character.toUpperCase(c));
                else if (Character.isUpperCase(c)) sb.append(" ").append(c);
                else sb.append(c);
            }
            return sb.toString();
        }
    }
}
