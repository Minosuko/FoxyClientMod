package com.foxyclient.gui;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.registry.Registries;
import net.minecraft.block.Block;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI - Premium draggable category panels with cyan/teal theme.
 * Features: slider dragging, toggle switches, mode arrows, tooltips, keybind display.
 */
public class ClickGUI extends Screen {
    // ===== Layout Constants =====
    private int panelWidth = 130;
    private static final int HEADER_HEIGHT = 22;
    private static final int ITEM_HEIGHT = 16;
    private static final int SETTING_HEIGHT = 14;
    private static final int SLIDER_HEIGHT = 12;
    private static final int PADDING = 6;
    private int maxPanelHeight = 320;

    // ===== Cyan/Teal Color Palette =====
    private static final int COL_ACCENT       = 0xFF00E5CC;  // Bright cyan/teal
    private static final int COL_ACCENT_DIM   = 0xFF00A896;  // Dimmed cyan
    private static final int COL_ACCENT_DARK  = 0xFF006B5E;  // Dark teal
    private static final int COL_HEADER_BG    = 0xFF0D1117;  // Deep dark header
    private static final int COL_PANEL_BG     = 0xFF161B22;  // Dark panel body
    private static final int COL_ITEM_BG      = 0xFF1C2333;  // Item background
    private static final int COL_ITEM_HOVER   = 0xFF242D3D;  // Item hover
    private static final int COL_ENABLED_BG   = 0xFF0A2420;  // Dark teal tint for enabled
    private static final int COL_ENABLED_HOVER= 0xFF0D2E29;  // Enabled hover
    private static final int COL_SETTING_BG   = 0xFF111820;  // Setting row bg
    private static final int COL_BORDER       = 0xFF30363D;  // Borders
    private static final int COL_TEXT          = 0xFFE6EDF3;  // Primary text
    private static final int COL_TEXT_DIM     = 0xFF8B949E;  // Dimmed text
    private static final int COL_SLIDER_BG    = 0xFF21262D;  // Slider track
    private static final int COL_SLIDER_FILL  = 0xFF00E5CC;  // Slider filled portion
    private static final int COL_TOGGLE_OFF   = 0xFF21262D;  // Toggle off bg
    private static final int COL_TOGGLE_ON    = 0xFF00E5CC;  // Toggle on bg
    private static final int COL_DIM_OVERLAY  = 0x88000000;  // Background dim
    private static final int COL_TOOLTIP_BG   = 0xF0161B22;  // Tooltip background
    private static final int COL_SEARCH_BG    = 0xFF0D1117;  // Search bar bg

    // ===== State =====
    private final List<Panel> panels = new ArrayList<>();
    private Panel draggingPanel = null;
    private int dragOffsetX, dragOffsetY;
    private String searchQuery = "";
    private Panel searchPanel = null;

    // Tooltip state
    private String tooltipText = null;
    private int tooltipX, tooltipY;

    // Slider drag state
    private NumberSetting draggingSlider = null;
    private Panel draggingSliderPanel = null;

    // Color slider drag state
    private ColorSetting draggingColorSlider = null;
    private int draggingColorChannel = -1;

    // Keybind listening
    private Module listeningKeybind = null;

    private boolean initialized = false;

    public ClickGUI() {
        super(Text.literal("FoxyClient"));
        for (Category category : Category.values()) {
            panels.add(new Panel(category, 0, 10));
        }
    }

    @Override
    protected void init() {
        super.init();
        // Compute panel width and max height based on screen size
        int categoryCount = panels.size();
        int totalGap = 6 * (categoryCount - 1) + 20; // 6px between panels + 10px margin each side
        panelWidth = Math.max(100, Math.min(150, (width - totalGap) / categoryCount));
        maxPanelHeight = Math.max(200, height - 60);

        // Only auto-position on first open (don't override user-dragged positions)
        if (!initialized) {
            int totalWidth = categoryCount * panelWidth + (categoryCount - 1) * 6;
            int startX = Math.max(4, (width - totalWidth) / 2);
            int x = startX;
            for (Panel panel : panels) {
                panel.x = x;
                panel.y = 10;
                x += panelWidth + 6;
            }
            initialized = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim background
        context.fill(0, 0, width, height, COL_DIM_OVERLAY);

        tooltipText = null;

        // Search bar (top right)
        int sw = 160;
        int sh = 22;
        int sx = width - sw - 10;
        int sy = 10;
        context.fill(sx, sy, sx + sw, sy + sh, COL_SEARCH_BG);
        context.fill(sx, sy + sh - 1, sx + sw, sy + sh, COL_BORDER);
        // Accent line on top
        context.fill(sx, sy, sx + sw, sy + 1, COL_ACCENT);
        String displayText = searchQuery.isEmpty() ? "§7Search..." : searchQuery + "§f_";
        context.drawTextWithShadow(textRenderer, displayText, sx + 6, sy + 7, COL_TEXT);

        // Render category panels
        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY);
        }

        // Search results panel
        if (!searchQuery.isEmpty()) {
            if (searchPanel == null) {
                searchPanel = new Panel(null, width / 2 - panelWidth / 2, height / 4);
            }
            searchPanel.updateSearch(searchQuery);
            searchPanel.render(context, mouseX, mouseY);
        } else {
            searchPanel = null;
        }

        // Keybind listening indicator
        if (listeningKeybind != null) {
            String msg = "§7Press a key for §b" + listeningKeybind.getName() + " §7(ESC to cancel)";
            int msgW = textRenderer.getWidth(msg.replaceAll("§.", ""));
            int mx = width / 2 - msgW / 2 - 6;
            int my = height - 30;
            context.fill(mx, my, mx + msgW + 12, my + 16, COL_TOOLTIP_BG);
            context.fill(mx, my, mx + msgW + 12, my + 1, COL_ACCENT);
            context.drawTextWithShadow(textRenderer, msg, mx + 6, my + 4, COL_TEXT);
        }

        // Render tooltip last (on top of everything)
        if (tooltipText != null) {
            int tw = textRenderer.getWidth(tooltipText) + 8;
            int tx = tooltipX + 8;
            int ty = tooltipY - 10;
            if (tx + tw > width) tx = width - tw - 4;
            if (ty < 0) ty = tooltipY + 16;
            context.fill(tx - 2, ty - 2, tx + tw, ty + 12, COL_TOOLTIP_BG);
            context.fill(tx - 2, ty - 2, tx + tw, ty - 1, COL_ACCENT_DIM);
            context.drawTextWithShadow(textRenderer, tooltipText, tx + 2, ty, COL_TEXT_DIM);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // Search panel interaction (drawn on top of everything)
        if (searchPanel != null && !searchQuery.isEmpty()) {
            if (isInsidePanel(searchPanel, mouseX, mouseY)) {
                if (searchPanel.mouseClicked(click, bl)) return true;
                if (mouseY >= searchPanel.y && mouseY <= searchPanel.y + HEADER_HEIGHT) {
                    draggingPanel = searchPanel;
                    dragOffsetX = (int) (mouseX - searchPanel.x);
                    dragOffsetY = (int) (mouseY - searchPanel.y);
                }
                return true; // Consume click within search panel bounds
            }
        }

        // Iterate panels in REVERSE order (last rendered = on top = checked first)
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (isInsidePanel(panel, mouseX, mouseY)) {
                if (panel.mouseClicked(click, bl)) return true;
                // Header drag
                if (mouseY >= panel.y && mouseY <= panel.y + HEADER_HEIGHT) {
                    draggingPanel = panel;
                    dragOffsetX = (int) (mouseX - panel.x);
                    dragOffsetY = (int) (mouseY - panel.y);
                }
                return true; // Consume click within this panel's bounds — don't pass to panels behind
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (listeningKeybind != null) return true; // Absorb during keybind listen
        if (charInput.isValidChar()) {
            searchQuery += charInput.asString();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();

        // Keybind listening mode
        if (listeningKeybind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                listeningKeybind.setKeybind(GLFW.GLFW_KEY_UNKNOWN);
                listeningKeybind = null;
            } else {
                listeningKeybind.setKeybind(keyCode);
                listeningKeybind = null;
            }
            return true;
        }

        if (keyCode == 259) { // Backspace
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            }
            return true;
        }
        if (keyCode == 256) { // Escape
            if (!searchQuery.isEmpty()) {
                searchQuery = "";
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Search panel scrolling (on top)
        if (searchPanel != null && !searchQuery.isEmpty()) {
            if (isInsidePanel(searchPanel, mouseX, mouseY)) {
                searchPanel.scroll -= (int) (verticalAmount * ITEM_HEIGHT);
                if (searchPanel.scroll < 0) searchPanel.scroll = 0;
                return true;
            }
        }
        // Reverse order: topmost panel gets scroll first
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (isInsidePanel(panel, mouseX, mouseY)) {
                panel.scroll -= (int) (verticalAmount * ITEM_HEIGHT);
                if (panel.scroll < 0) panel.scroll = 0;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPanel = null;
        draggingSlider = null;
        draggingSliderPanel = null;
        draggingColorSlider = null;
        draggingColorChannel = -1;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();

        // Panel dragging
        if (draggingPanel != null) {
            draggingPanel.x = (int) (mouseX - dragOffsetX);
            draggingPanel.y = (int) (mouseY - dragOffsetY);
            return true;
        }

        // Number slider dragging
        if (draggingSlider != null && draggingSliderPanel != null) {
            int sliderX = draggingSliderPanel.x + PADDING + 4;
            int sliderW = panelWidth - PADDING * 2 - 8;
            float ratio = (float) ((mouseX - sliderX) / sliderW);
            ratio = Math.max(0, Math.min(1, ratio));
            double newVal = draggingSlider.getMin() + ratio * (draggingSlider.getMax() - draggingSlider.getMin());
            // Snap to reasonable precision
            double step = (draggingSlider.getMax() - draggingSlider.getMin());
            if (step <= 1) newVal = Math.round(newVal * 100.0) / 100.0;
            else if (step <= 20) newVal = Math.round(newVal * 10.0) / 10.0;
            else newVal = Math.round(newVal);
            draggingSlider.set(newVal);
            return true;
        }

        // Color slider dragging
        if (draggingColorSlider != null && draggingColorChannel >= 0) {
            int sliderX = 10; // offset from panel x
            int sliderW = panelWidth - 20;
            // We need the panel reference; use searchPanel or iterate panels
            // Since we stored the color setting, we can compute from mouseX
            float val = (float) ((mouseX - sliderX) / sliderW);
            // We need to use the panel's x for the slider offset
            // Approximate: iterate all panels to find the correct one
            for (Panel panel : getAllPanels()) {
                if (panel.expandedModule != null) {
                    int sx = panel.x + 10;
                    int sw = panelWidth - 20;
                    val = (float) ((mouseX - sx) / sw);
                    break;
                }
            }
            val = Math.max(0, Math.min(1, val));
            float[] hsb = draggingColorSlider.getHSB();
            int a = draggingColorSlider.getAlpha();
            if (draggingColorChannel == 0) draggingColorSlider.setHSB(val, hsb[1], hsb[2], a);
            else if (draggingColorChannel == 1) draggingColorSlider.setHSB(hsb[0], val, hsb[2], a);
            else if (draggingColorChannel == 2) draggingColorSlider.setHSB(hsb[0], hsb[1], val, a);
            else if (draggingColorChannel == 3) draggingColorSlider.setHSB(hsb[0], hsb[1], hsb[2], (int)(val * 255));
            return true;
        }

        // Panel-level drag handlers
        for (Panel panel : panels) {
            if (panel.mouseDragged(click, deltaX, deltaY)) return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    private List<Panel> getAllPanels() {
        List<Panel> all = new ArrayList<>(panels);
        if (searchPanel != null) all.add(searchPanel);
        return all;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        FoxyClient.INSTANCE.getModuleManager().saveConfig();
        super.close();
    }

    /**
     * Check if a point is inside a panel's full bounds (header + body if expanded).
     */
    private boolean isInsidePanel(Panel panel, double mx, double my) {
        if (mx < panel.x || mx > panel.x + panelWidth) return false;
        if (my < panel.y) return false;
        int panelBottom = panel.y + HEADER_HEIGHT;
        if (panel.expanded) {
            panelBottom += maxPanelHeight;
        }
        return my <= panelBottom;
    }

    private void setTooltip(String text, int x, int y) {
        this.tooltipText = text;
        this.tooltipX = x;
        this.tooltipY = y;
    }

    /**
     * Get GLFW key name for display.
     */
    private static String getKeyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode == 0) return "";
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) return name.toUpperCase();
        // Fallback for special keys
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            default -> "KEY" + keyCode;
        };
    }

    // =================================================================================
    //  PANEL - Draggable category panel
    // =================================================================================
    private class Panel {
        final Category category;
        final List<Module> modules;
        int x, y;
        int scroll = 0;
        boolean expanded = true;
        String expandedModule = null;
        String expandedColorSetting = null;

        Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
            if (category != null) {
                this.modules = FoxyClient.INSTANCE.getModuleManager().getModulesByCategory(category);
            } else {
                this.modules = new ArrayList<>();
            }
        }

        void updateSearch(String query) {
            modules.clear();
            for (Module m : FoxyClient.INSTANCE.getModuleManager().getModules()) {
                if (m.isVisible() && m.getName().toLowerCase().contains(query.toLowerCase())) {
                    modules.add(m);
                }
            }
        }

        void render(DrawContext context, int mouseX, int mouseY) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;

            // ===== Header =====
            context.fill(x, y, x + panelWidth, y + HEADER_HEIGHT, COL_HEADER_BG);
            // Top accent bar
            context.fill(x, y, x + panelWidth, y + 2, COL_ACCENT);
            // Bottom border
            context.fill(x, y + HEADER_HEIGHT - 1, x + panelWidth, y + HEADER_HEIGHT, COL_BORDER);

            String title = category == null ? "§bSearch" : "§b" + category.getDisplayName();
            context.drawTextWithShadow(tr, title, x + PADDING, y + 7, COL_TEXT);

            // Expand/collapse icon
            String icon = expanded ? "▾" : "▸";
            context.drawTextWithShadow(tr, icon, x + panelWidth - 12, y + 7, COL_ACCENT_DIM);

            if (!expanded) return;

            // ===== Panel Body =====
            int contentY = y + HEADER_HEIGHT;
            context.enableScissor(x, contentY, x + panelWidth, contentY + maxPanelHeight);

            int yOff = contentY - scroll;

            for (Module module : modules) {
                // ---- Module Row ----
                if (yOff + ITEM_HEIGHT > contentY && yOff < contentY + maxPanelHeight) {
                    boolean hover = mouseX >= x && mouseX <= x + panelWidth &&
                                   mouseY >= yOff && mouseY <= yOff + ITEM_HEIGHT;

                    // Background
                    int bg;
                    if (module.isEnabled()) {
                        bg = hover ? COL_ENABLED_HOVER : COL_ENABLED_BG;
                    } else {
                        bg = hover ? COL_ITEM_HOVER : COL_ITEM_BG;
                    }
                    context.fill(x, yOff, x + panelWidth, yOff + ITEM_HEIGHT, bg);

                    // Left accent bar for enabled modules
                    if (module.isEnabled()) {
                        context.fill(x, yOff, x + 2, yOff + ITEM_HEIGHT, COL_ACCENT);
                    }

                    // Module name
                    int textColor = module.isEnabled() ? COL_ACCENT : COL_TEXT;
                    context.drawTextWithShadow(tr, module.getName(), x + PADDING, yOff + 4, textColor);

                    // Keybind label (right side)
                    String keyName = getKeyName(module.getKeybind());
                    if (!keyName.isEmpty()) {
                        int kw = tr.getWidth("[" + keyName + "]");
                        context.drawTextWithShadow(tr, "§8[§7" + keyName + "§8]",
                            x + panelWidth - kw - 4, yOff + 4, COL_TEXT_DIM);
                    }

                    // Settings indicator
                    if (!module.getSettings().isEmpty()) {
                        String arrow = module.getName().equals(expandedModule) ? "−" : "+";
                        int arrowX = x + panelWidth - 14;
                        if (!keyName.isEmpty()) arrowX -= tr.getWidth("[" + keyName + "]") + 2;
                        context.drawTextWithShadow(tr, arrow, arrowX, yOff + 4, COL_TEXT_DIM);
                    }

                    // Bottom separator
                    context.fill(x + 2, yOff + ITEM_HEIGHT - 1, x + panelWidth - 2, yOff + ITEM_HEIGHT, 0xFF1A1F2B);

                    // Tooltip
                    if (hover) {
                        setTooltip(module.getDescription(), mouseX, mouseY);
                    }
                }
                yOff += ITEM_HEIGHT;

                // ---- Expanded Settings ----
                if (module.getName().equals(expandedModule)) {
                    for (Setting<?> setting : module.getSettings()) {
                        int h = getSettingHeight(setting);
                        if (yOff + h > contentY && yOff < contentY + maxPanelHeight) {
                            renderSetting(context, tr, setting, x, yOff, mouseX, mouseY);
                        }
                        yOff += h;

                        // Color picker sliders
                        if (setting instanceof ColorSetting cs && setting.getName().equals(expandedColorSetting)) {
                            yOff = renderColorPicker(context, cs, x, yOff, mouseX, mouseY);
                        }
                    }

                    // Keybind row
                    if (yOff + SETTING_HEIGHT > contentY && yOff < contentY + maxPanelHeight) {
                        context.fill(x + 4, yOff, x + panelWidth - 4, yOff + SETTING_HEIGHT, COL_SETTING_BG);
                        String bindText = "Bind: ";
                        String keyStr = getKeyName(module.getKeybind());
                        if (listeningKeybind == module) {
                            keyStr = "§e...";
                        } else if (keyStr.isEmpty()) {
                            keyStr = "§8None";
                        } else {
                            keyStr = "§b" + keyStr;
                        }
                        context.drawTextWithShadow(tr, "§7" + bindText + keyStr, x + 10, yOff + 3, COL_TEXT_DIM);

                        boolean hover = mouseX >= x + 4 && mouseX <= x + panelWidth - 4 &&
                                       mouseY >= yOff && mouseY <= yOff + SETTING_HEIGHT;
                        if (hover) {
                            setTooltip("Click to set keybind, right-click to clear", mouseX, mouseY);
                        }
                    }
                    yOff += SETTING_HEIGHT;
                }
            }

            context.disableScissor();

            // Bottom panel border
            int finalY = Math.min(yOff, contentY + maxPanelHeight);
            context.fill(x, finalY, x + panelWidth, finalY + 1, COL_BORDER);

            // Panel body background fill (behind content, actually fill before content in proper impl)
            // Scroll clamping
            int totalHeight = yOff - (contentY - scroll);
            if (totalHeight < maxPanelHeight) scroll = 0;
            else if (scroll > totalHeight - maxPanelHeight) scroll = totalHeight - maxPanelHeight;
        }

        private int getSettingHeight(Setting<?> setting) {
            if (setting instanceof BlockListSetting bls) {
                return SETTING_HEIGHT + (bls.getBlocks().size() * 14) + 4;
            }
            return SETTING_HEIGHT;
        }

        private void renderSetting(DrawContext context, net.minecraft.client.font.TextRenderer tr,
                                    Setting<?> setting, int px, int sy, int mouseX, int mouseY) {
            // Setting row background
            int height = getSettingHeight(setting);
            context.fill(px + 4, sy, px + panelWidth - 4, sy + height, COL_SETTING_BG);

            boolean hoverBase = mouseX >= px + 4 && mouseX <= px + panelWidth - 4 &&
                               mouseY >= sy && mouseY <= sy + SETTING_HEIGHT;

            if (setting instanceof BlockListSetting bls) {
                context.drawTextWithShadow(tr, "§7" + setting.getName(), px + 10, sy + 3, COL_TEXT_DIM);
                
                // Edit button — opens BlockSelectorScreen
                int btnW = 30;
                int btnX = px + panelWidth - btnW - 10;
                boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= sy + 2 && mouseY <= sy + 12;
                context.fill(btnX, sy + 2, btnX + btnW, sy + 12, btnHover ? COL_ACCENT : COL_ACCENT_DIM);
                context.drawTextWithShadow(tr, "Edit", btnX + 3, sy + 3, COL_TEXT);

                int listY = sy + SETTING_HEIGHT;
                List<Block> blocks = bls.getBlocks();
                for (int i = 0; i < blocks.size(); i++) {
                    Block b = blocks.get(i);
                    String name = BlockListSetting.getDisplayName(b);
                    if (name.length() > 14) name = name.substring(0, 11) + "...";
                    
                    // Block item icon
                    context.drawItem(new ItemStack(b), px + 10, listY - 1);
                    // Name next to icon
                    context.drawTextWithShadow(tr, "§7" + name, px + 28, listY + 3, COL_TEXT_DIM);
                    
                    // Remove "X"
                    int xX = px + panelWidth - 20;
                    boolean xHover = mouseX >= xX && mouseX <= xX + 10 && mouseY >= listY && mouseY <= listY + 12;
                    context.drawTextWithShadow(tr, "§c[X]", xX, listY + 3, xHover ? 0xFFFFFFFF : 0xFFAAAAAA);
                    
                    listY += 14;
                }
            } else if (setting instanceof BoolSetting bs) {
                // ---- Toggle Switch ----
                context.drawTextWithShadow(tr, "§7" + setting.getName(), px + 10, sy + 3, COL_TEXT_DIM);

                // Toggle track
                int toggleX = px + panelWidth - 30;
                int toggleW = 20;
                int toggleH = 8;
                int toggleY = sy + 3;
                int trackColor = bs.get() ? COL_TOGGLE_ON : COL_TOGGLE_OFF;
                context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, trackColor);

                // Toggle knob
                int knobX = bs.get() ? toggleX + toggleW - 8 : toggleX;
                context.fill(knobX, toggleY - 1, knobX + 8, toggleY + toggleH + 1, 0xFFFFFFFF);

            } else if (setting instanceof NumberSetting ns) {
                // ---- Slider ----
                String label = "§7" + setting.getName();
                String valStr;
                double range = ns.getMax() - ns.getMin();
                if (range <= 1) valStr = String.format("§b%.2f", ns.get());
                else if (range <= 20) valStr = String.format("§b%.1f", ns.get());
                else valStr = String.format("§b%.0f", ns.get());

                context.drawTextWithShadow(tr, label, px + 10, sy + 1, COL_TEXT_DIM);
                int valW = tr.getWidth(valStr.replaceAll("§.", ""));
                context.drawTextWithShadow(tr, valStr, px + panelWidth - valW - 10, sy + 1, COL_TEXT_DIM);

                // Slider track
                int sliderX = px + PADDING + 4;
                int sliderW = panelWidth - PADDING * 2 - 8;
                int sliderY = sy + SETTING_HEIGHT - 3;
                float ratio = (float) ((ns.get() - ns.getMin()) / (ns.getMax() - ns.getMin()));
                int filledW = (int) (ratio * sliderW);

                context.fill(sliderX, sliderY, sliderX + sliderW, sliderY + 2, COL_SLIDER_BG);
                context.fill(sliderX, sliderY, sliderX + filledW, sliderY + 2, COL_SLIDER_FILL);
                // Knob
                context.fill(sliderX + filledW - 1, sliderY - 1, sliderX + filledW + 1, sliderY + 3, 0xFFFFFFFF);

            } else if (setting instanceof ModeSetting ms) {
                // ---- Mode with arrows ----
                // Left arrow
                context.drawTextWithShadow(tr, "§7◀", px + 10, sy + 3, COL_TEXT_DIM);
                // Mode name centered
                String mode = ms.get();
                int modeW = tr.getWidth(mode);
                int centerX = px + panelWidth / 2 - modeW / 2;
                context.drawTextWithShadow(tr, "§b" + mode, centerX, sy + 3, COL_ACCENT);
                // Right arrow
                context.drawTextWithShadow(tr, "§7▶", px + panelWidth - 18, sy + 3, COL_TEXT_DIM);

                // Setting name above in dim (only if space, use smaller approach)
                // We put the name as a tooltip instead
                if (hoverBase) {
                    setTooltip(setting.getName() + ": " + setting.getDescription(), mouseX, mouseY);
                }

            } else if (setting instanceof ColorSetting cs) {
                // ---- Color preview ----
                context.drawTextWithShadow(tr, "§7" + setting.getName(), px + 10, sy + 3, COL_TEXT_DIM);
                int previewX = px + panelWidth - 22;
                context.fill(previewX, sy + 2, previewX + 14, sy + 12, 0xFFFFFFFF);
                context.fill(previewX + 1, sy + 3, previewX + 13, sy + 11, cs.get().getRGB());

            } else if (setting instanceof KeySetting ks) {
                String keyStr = getKeyName(ks.get());
                if (keyStr.isEmpty()) keyStr = "None";
                context.drawTextWithShadow(tr, "§7" + setting.getName() + ": §b" + keyStr,
                    px + 10, sy + 3, COL_TEXT_DIM);
            } else {
                context.drawTextWithShadow(tr, "§7" + setting.getName() + ": §f" + setting.get(),
                    px + 10, sy + 3, COL_TEXT_DIM);
            }

            // Hover tooltip for non-mode settings
            if (hoverBase && !(setting instanceof ModeSetting)) {
                setTooltip(setting.getDescription(), mouseX, mouseY);
            }
        }

        private int renderColorPicker(DrawContext context, ColorSetting cs, int px, int yOff, int mouseX, int mouseY) {
            float[] hsb = cs.getHSB();
            int alpha = cs.getAlpha();
            String[] labels = {"H", "S", "B", "A"};
            float[] values = {hsb[0], hsb[1], hsb[2], alpha / 255f};

            for (int i = 0; i < 4; i++) {
                int sy = yOff;
                // Background
                context.fill(px + 6, sy, px + panelWidth - 6, sy + SLIDER_HEIGHT, 0xFF0A0E14);

                // Label
                context.drawTextWithShadow(
                    net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                    "§8" + labels[i], px + 8, sy + 2, COL_TEXT_DIM);

                // Slider track
                int sliderX = px + 18;
                int sliderW = panelWidth - 30;

                if (i == 0) {
                    // Hue: rainbow gradient
                    for (int j = 0; j < sliderW; j++) {
                        float h = j / (float) sliderW;
                        int c = Color.HSBtoRGB(h, 1f, 1f);
                        context.fill(sliderX + j, sy + 3, sliderX + j + 1, sy + SLIDER_HEIGHT - 3, c | 0xFF000000);
                    }
                } else {
                    // Simple track
                    context.fill(sliderX, sy + 4, sliderX + sliderW, sy + SLIDER_HEIGHT - 4, COL_SLIDER_BG);
                    int filled = (int) (values[i] * sliderW);
                    context.fill(sliderX, sy + 4, sliderX + filled, sy + SLIDER_HEIGHT - 4, COL_ACCENT_DIM);
                }

                // Knob position
                int knobX = sliderX + (int) (values[i] * sliderW);
                context.fill(knobX - 1, sy + 2, knobX + 2, sy + SLIDER_HEIGHT - 2, 0xFFFFFFFF);

                yOff += SLIDER_HEIGHT;
            }
            return yOff;
        }

        boolean mouseClicked(Click click, boolean doubled) {
            double mouseX = click.x();
            double mouseY = click.y();
            int button = click.button();

            // Header click
            if (mouseX >= x && mouseX <= x + panelWidth && mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
                if (mouseX >= x + panelWidth - 20) {
                    expanded = !expanded;
                    return true;
                }
            }

            if (!expanded) return false;

            int contentY = y + HEADER_HEIGHT;
            int yOff = contentY - scroll;

            for (Module module : modules) {
                // Module row click
                if (yOff >= contentY && yOff + ITEM_HEIGHT <= contentY + maxPanelHeight) {
                    if (mouseX >= x && mouseX <= x + panelWidth &&
                        mouseY >= yOff && mouseY <= yOff + ITEM_HEIGHT) {

                        if (button == 0) {
                            module.toggle();
                            return true;
                        } else if (button == 1) {
                            expandedModule = module.getName().equals(expandedModule) ? null : module.getName();
                            return true;
                        } else if (button == 2) { // Middle click: listen for keybind
                            listeningKeybind = module;
                            return true;
                        }
                    }
                }
                yOff += ITEM_HEIGHT;

                // Settings clicks
                if (module.getName().equals(expandedModule)) {
                    for (Setting<?> setting : module.getSettings()) {
                        int h = getSettingHeight(setting);
                        if (yOff >= contentY && yOff + h <= contentY + maxPanelHeight) {
                            if (mouseX >= x + 4 && mouseX <= x + panelWidth - 4 &&
                                mouseY >= yOff && mouseY <= yOff + h) {

                                if (setting instanceof BlockListSetting bls) {
                                    // Edit button — open BlockSelectorScreen
                                    int btnW = 30;
                                    int btnX = x + panelWidth - btnW - 10;
                                    if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= yOff + 2 && mouseY <= yOff + 12) {
                                        if (button == 0) {
                                            var mc = net.minecraft.client.MinecraftClient.getInstance();
                                            mc.setScreen(new BlockSelectorScreen(bls, () -> {
                                                if (mc.worldRenderer != null) mc.worldRenderer.reload();
                                            }, ClickGUI.this));
                                            return true;
                                        }
                                    }
                                    // Check remove buttons
                                    int listY = yOff + SETTING_HEIGHT;
                                    for (int i = 0; i < bls.getBlocks().size(); i++) {
                                        int xX = x + panelWidth - 20;
                                        if (mouseX >= xX && mouseX <= xX + 15 && mouseY >= listY && mouseY <= listY + 14) {
                                            if (button == 0) {
                                                bls.removeBlock(bls.getBlocks().get(i));
                                                return true;
                                            }
                                        }
                                        listY += 14;
                                    }
                                } else if (setting instanceof BoolSetting bs) {
                                    if (button == 0) { bs.set(!bs.get()); return true; }
                                } else if (setting instanceof NumberSetting ns) {
                                    if (button == 0) {
                                        // Start slider drag
                                        int sliderX = x + PADDING + 4;
                                        int sliderW = panelWidth - PADDING * 2 - 8;
                                        float ratio = (float) ((mouseX - sliderX) / sliderW);
                                        ratio = Math.max(0, Math.min(1, ratio));
                                        double newVal = ns.getMin() + ratio * (ns.getMax() - ns.getMin());
                                        double step = ns.getMax() - ns.getMin();
                                        if (step <= 1) newVal = Math.round(newVal * 100.0) / 100.0;
                                        else if (step <= 20) newVal = Math.round(newVal * 10.0) / 10.0;
                                        else newVal = Math.round(newVal);
                                        ns.set(newVal);
                                        draggingSlider = ns;
                                        draggingSliderPanel = this;
                                        return true;
                                    } else if (button == 1) {
                                        // Right click: decrement
                                        double stp = (ns.getMax() - ns.getMin()) / 20.0;
                                        ns.set(ns.get() - stp);
                                        return true;
                                    }
                                } else if (setting instanceof ModeSetting ms) {
                                    if (button == 0) {
                                        // Check if left or right arrow area
                                        if (mouseX < x + panelWidth / 2) {
                                            // Cycle backwards
                                            String[] modes = ms.getModes();
                                            String current = ms.get();
                                            for (int i = 0; i < modes.length; i++) {
                                                if (modes[i].equals(current)) {
                                                    ms.set(modes[(i - 1 + modes.length) % modes.length]);
                                                    break;
                                                }
                                            }
                                        } else {
                                            ms.cycle();
                                        }
                                        return true;
                                    }
                                } else if (setting instanceof ColorSetting) {
                                    if (button == 0 || button == 1) {
                                        expandedColorSetting = setting.getName().equals(expandedColorSetting) ? null : setting.getName();
                                        return true;
                                    }
                                }
                            }
                        }
                        yOff += SETTING_HEIGHT;

                        // Color picker slider clicks
                        if (setting instanceof ColorSetting cs && setting.getName().equals(expandedColorSetting)) {
                            for (int i = 0; i < 4; i++) {
                                if (mouseX >= x + 18 && mouseX <= x + panelWidth - 12 &&
                                    mouseY >= yOff && mouseY <= yOff + SLIDER_HEIGHT) {

                                    int sliderX = x + 18;
                                    int sliderW = panelWidth - 30;
                                    float val = (float) ((mouseX - sliderX) / (double) sliderW);
                                    val = Math.max(0, Math.min(1, val));

                                    float[] hsb = cs.getHSB();
                                    int a = cs.getAlpha();
                                    if (i == 0) cs.setHSB(val, hsb[1], hsb[2], a);
                                    else if (i == 1) cs.setHSB(hsb[0], val, hsb[2], a);
                                    else if (i == 2) cs.setHSB(hsb[0], hsb[1], val, a);
                                    else cs.setHSB(hsb[0], hsb[1], hsb[2], (int)(val * 255));

                                    draggingColorSlider = cs;
                                    draggingColorChannel = i;
                                    return true;
                                }
                                yOff += SLIDER_HEIGHT;
                            }
                        }
                    }

                    // Keybind row click
                    if (yOff >= contentY && yOff + SETTING_HEIGHT <= contentY + maxPanelHeight) {
                        if (mouseX >= x + 4 && mouseX <= x + panelWidth - 4 &&
                            mouseY >= yOff && mouseY <= yOff + SETTING_HEIGHT) {
                            if (button == 0) {
                                listeningKeybind = module;
                                return true;
                            } else if (button == 1) {
                                module.setKeybind(GLFW.GLFW_KEY_UNKNOWN);
                                return true;
                            }
                        }
                    }
                    yOff += SETTING_HEIGHT;
                }
            }
            return false;
        }

        boolean mouseDragged(Click click, double deltaX, double deltaY) {
            // Color slider dragging is handled at the ClickGUI level
            return false;
        }
    }
}
