package com.foxyclient.gui;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.module.ui.CustomKeybinds;
import com.foxyclient.setting.*;
import com.foxyclient.util.BaritoneSettings;
import com.foxyclient.util.FoxyConfig;
import com.foxyclient.util.RenderUtil;
import com.foxyclient.util.SkinResourceManager;
import com.foxyclient.util.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ClickGUI - Liquid Glass Edition
 * Premium aesthetics with gradients, glows, and shimmer effects.
 * Includes full Cosmetic management with server sync.
 */
public class ClickGUI extends Screen {
    // ===== UI Constants =====
    private static final int SIDEBAR_WIDTH = 115;
    private static final int TOPBAR_HEIGHT = 48;
    private static final int CARD_GAP = 12;
    private static final int PADDING = 15;
    private static final Identifier OUTFIT_FONT = Identifier.of("foxyclient", "outfit");

    // ===== Colors =====
    private static final int COL_BG_TOP     = 0x440B0E14; // More transparent
    private static final int COL_BG_BOTTOM  = 0x6611151C;
    private static final int COL_SIDEBAR    = 0x330D1017;
    private static final int COL_ACCENT     = 0xFF00E5CC;
    private static final int COL_ACCENT_DIM = 0xAA00E5CC;
    private static final int COL_CARD       = 0x221C2333; // Very transparent
    private static final int COL_CARD_HOV   = 0x44242D3D;
    private static final int COL_BORDER     = 0x2230363D;
    private static final int COL_GLOW       = 0x1100E5CC;
    private static final int COL_TEXT       = 0xFFE6EDF3;
    private static final int COL_TEXT_DIM   = 0xFF8B949E;
    private static final int COL_FROST      = 0x11FFFFFF; // Frost layer

    // ===== State =====
    private String selectedSidebar = "Cheat"; 
    private Category selectedCategory = null; 
    private StateFilter stateFilter = StateFilter.ALL;
    private String searchQuery = "";
    private float scrollAmount = 0;
    private float targetScroll = 0;
    private Module expandedModule = null;

    private boolean searchFocused = false;
    private boolean categoryDropdownOpen = false;
    private boolean stateDropdownOpen = false;

    // Creation State
    private int addingMacroStep = 0;
    private int tempMacroKey = 0;
    private String tempTextBuffer = "";
    private boolean addingWaypoint = false;
    private String[] wpBuffer = {"", "0", "0", "0"};
    private int wpInputIdx = 0;

    // Cosmetic State
    private float skinYaw = 0;
    private float skinPitch = 0;
    private boolean isDraggingSkin = false;
    private boolean skinDropdownOpen = false;
    private boolean capeDropdownOpen = false;
    private String syncStatus = "";
    private long syncStatusTime = 0;

    private static final String[] SIDEBAR_ITEMS = {"Cheat", "Cosmetic", "UI", "Macro", "Waypoint", "Baritone", "Setting"};
    private Module listeningKeybind = null;
    private String hoveredDesc = null;

    // Color Picker State
    private ColorSetting activeColorPicker = null;
    private int pickerX, pickerY;
    private boolean isDraggingHue, isDraggingAlpha, isDraggingSB;

    public ClickGUI() { super(Text.literal("FoxyClient")); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollAmount = MathHelper.lerp(delta * 0.15f, scrollAmount, targetScroll);
        hoveredDesc = null;
        if (Math.abs(scrollAmount - targetScroll) < 0.1f) scrollAmount = targetScroll;

        // Main Background with Gradient
        context.fill(0, 0, width, height, 0x44000000); 
        context.fillGradient(PADDING, PADDING, width - PADDING, height - PADDING, COL_BG_TOP, COL_BG_BOTTOM);
        
        // Frost Layer (simulates blur)
        context.fill(PADDING, PADDING, width - PADDING, height - PADDING, COL_FROST);
        
        // Liquid Border Glow
        drawGlowBorder(context, PADDING, PADDING, width - PADDING, height - PADDING, COL_ACCENT_DIM);

        renderSidebar(context, mouseX, mouseY);
        drawText(context, "FOXYCLIENT §b" + selectedSidebar.toUpperCase() + (selectedCategory != null ? " §7> §f" + selectedCategory.getDisplayName() : ""), PADDING + SIDEBAR_WIDTH + 20, PADDING + 12, COL_TEXT, true);
        renderTopBar(context, mouseX, mouseY);
        
        // Automatic caching of official cosmetics (parity with FoxyConfig)
        if (selectedSidebar.equals("Cosmetic")) {
            try {
                var supplier = MinecraftClient.getInstance().getPlayerSkinCache().getSupplier(net.minecraft.component.type.ProfileComponent.ofDynamic(MinecraftClient.getInstance().getSession().getUuidOrNull()));
                net.minecraft.client.texture.PlayerSkinCache.Entry entry = supplier.get();
                if (entry != null) {
                    for (java.lang.reflect.Method m : entry.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().equals("textures") || m.getName().equals("getSkinTextures"))) {
                            Object textures = m.invoke(entry);
                            if (textures != null) { SkinResourceManager.cacheDefaultCosmetics(textures); break; }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        
        int x = PADDING + SIDEBAR_WIDTH + 20, y = PADDING + TOPBAR_HEIGHT + 20, w = width - PADDING*2 - SIDEBAR_WIDTH - 40, h = height - PADDING*2 - TOPBAR_HEIGHT - 40;
        context.enableScissor(x, y, x + w, y + h);
        
        switch (selectedSidebar) {
            case "Macro" -> renderMacroPage(context, x, y, w, h, mouseX, mouseY);
            case "Waypoint" -> renderWaypointPage(context, x, y, w, h, mouseX, mouseY);
            case "Cosmetic" -> renderCosmeticPage(context, x, y, w, h, mouseX, mouseY);
            case "Baritone" -> renderBaritonePage(context, x, y, w, h, mouseX, mouseY);
            case "Setting" -> renderSettingPage(context, x, y, w, h, mouseX, mouseY);
            default -> renderGrid(context, x, y, w, h, mouseX, mouseY);
        }
        context.disableScissor();
        drawScrollbar(context, x, y, w, h);

        if (listeningKeybind != null) renderModal(context, "Press any key to bind " + listeningKeybind.getName() + "...", "ESC to unbind | ANY KEY to set");
        else if (addingMacroStep == 1) renderModal(context, "Press a key for the new macro...", "ESC to cancel");
        else if (addingMacroStep == 2) renderInputModal(context, "Type command for key: §b" + getKeyName(tempMacroKey), tempTextBuffer);
        else if (addingWaypoint) renderInputModal(context, "Adding Waypoint - §b" + (wpInputIdx == 0 ? "Name" : (wpInputIdx == 1 ? "X" : (wpInputIdx == 2 ? "Y" : "Z"))), tempTextBuffer);

        // Dropdowns last for Z-index
        int sx = width - PADDING - 150, ty = PADDING + 34;
        if (stateDropdownOpen) renderStateDropdown(context, sx - 205, ty, 85);
        if (categoryDropdownOpen) renderCategoryDropdown(context, sx - 110, ty, 100);
        if (skinDropdownOpen) renderSkinDropdown(context, x + 10, y + 45, 110);
        if (capeDropdownOpen) renderCapeDropdown(context, x + 10, y + 120, 110);
        
        if (activeColorPicker != null) renderColorPicker(context, pickerX, pickerY);
        
        if (hoveredDesc != null && !hoveredDesc.isEmpty()) {
            int tw = textRenderer.getWidth(hoveredDesc) + 10;
            int tooltipX = Math.min(mouseX + 10, width - tw - 10);
            int tooltipY = mouseY + 12;
            context.fill(tooltipX, tooltipY, tooltipX + tw, tooltipY + 18, 0xDD0B0E14);
            drawGlowBorder(context, tooltipX, tooltipY, tooltipX + tw, tooltipY + 18, COL_ACCENT_DIM);
            drawText(context, hoveredDesc, tooltipX + 5, tooltipY + 5, COL_TEXT, true);
        }
    }

    private void drawGlowBorder(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y1 + 1, color); // Top
        context.fill(x1, y2 - 1, x2, y2, color); // Bot
        context.fill(x1, y1, x1 + 1, y2, color); // Left
        context.fill(x2 - 1, y1, x2, y2, color); // Right
        // Subtle glow
        int gc = (color & 0x00FFFFFF) | 0x11000000;
        context.fill(x1 - 1, y1 - 1, x2 + 1, y1, gc);
        context.fill(x1 - 1, y2, x2 + 1, y2 + 1, gc);
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY) {
        int x = PADDING, y = PADDING, h = height - PADDING * 2;
        context.fill(x, y, x + SIDEBAR_WIDTH, y + h, COL_SIDEBAR);
        context.fill(x + SIDEBAR_WIDTH - 1, y, x + SIDEBAR_WIDTH, y + h, COL_BORDER);
        
        // Sidebar Gloss
        context.fillGradient(x, y, x + SIDEBAR_WIDTH - 1, y + 40, 0x11FFFFFF, 0x00FFFFFF);

        drawText(context, "FOXY", x + 18, y + 20, COL_ACCENT, true);
        drawText(context, "CLIENT", x + 18, y + 30, COL_TEXT_DIM, true);
        
        int iy = y + 75;
        for (String label : SIDEBAR_ITEMS) {
            boolean sel = selectedSidebar.equals(label), hov = mouseX >= x && mouseX <= x+SIDEBAR_WIDTH && mouseY >= iy && mouseY <= iy+22 && !isMouseOverDropdown(mouseX, mouseY);
            if (sel) {
                context.fill(x, iy, x + 3, iy + 20, COL_ACCENT);
                context.fill(x + 3, iy, x + SIDEBAR_WIDTH - 1, iy + 20, 0x2200E5CC);
            }
            drawText(context, label.toLowerCase(), x + 15, iy + 6, sel ? COL_ACCENT : (hov ? COL_TEXT : COL_TEXT_DIM), true);
            iy += 26;
        }
    }

    private void renderTopBar(DrawContext context, int mouseX, int mouseY) {
        int x = PADDING + SIDEBAR_WIDTH, y = PADDING, w = width - PADDING*2 - SIDEBAR_WIDTH;
        context.fill(x, y, x + w, y + TOPBAR_HEIGHT, 0x880D1117);
        context.fill(x, y + TOPBAR_HEIGHT - 1, x + w, y + TOPBAR_HEIGHT, COL_BORDER);
        
        int ty = y + 14;
        int sx = width - PADDING - 150;
        
        if (selectedSidebar.equals("Cheat")) {
            int bx = sx - 110;
            drawGlassBtn(context, bx - 95, ty, 85, 20, stateFilter.name().toLowerCase() + " §7▾", stateDropdownOpen);
            drawGlassBtn(context, bx, ty, 100, 20, (selectedCategory == null ? "category" : selectedCategory.getDisplayName().toLowerCase()) + " §7▾", categoryDropdownOpen);
        }
        
        context.fill(sx, ty, sx+135, ty+20, 0xAA050709);
        context.fill(sx, ty+19, sx+135, ty+20, searchFocused ? COL_ACCENT : COL_BORDER);
        drawText(context, searchQuery.isEmpty() ? (searchFocused ? "" : "search...") : searchQuery, sx + 8, ty + 6, (searchQuery.isEmpty() && !searchFocused) ? 0xFF555555 : COL_TEXT, true);
    }

    private void drawGlassBtn(DrawContext context, int x, int y, int w, int h, String text, boolean active) {
        context.fill(x, y, x + w, y + h, active ? 0x4400E5CC : 0x441C2333);
        context.fill(x, y, x + w, y + 1, active ? COL_ACCENT : COL_BORDER);
        drawText(context, text, x + 8, y + 6, active ? COL_ACCENT : COL_TEXT_DIM, true);
    }

    private void renderCosmeticPage(DrawContext context, int x, int y, int w, int h, int mx, int my) {
        int ox = x + 10, oy = y + 10;
        // Selection group
        drawText(context, "SKIN", ox, oy, COL_ACCENT, true);
        drawGlassBtn(context, ox, oy+15, 110, 20, FoxyConfig.INSTANCE.skinName.get().toLowerCase() + " §7▾", skinDropdownOpen);
        drawShimmerBtn(context, ox, oy+40, 110, 20, "§bChange PNG", COL_CARD);

        drawText(context, "CAPE", ox, oy+75, COL_ACCENT, true);
        drawGlassBtn(context, ox, oy+90, 110, 20, FoxyConfig.INSTANCE.capeName.get().toLowerCase() + " §7▾", capeDropdownOpen);
        drawShimmerBtn(context, ox, oy+115, 110, 20, "§dChange PNG", COL_CARD);

        drawText(context, "MODEL", ox, oy+150, COL_TEXT_DIM, true);
        drawGlassBtn(context, ox, oy+165, 110, 20, FoxyConfig.INSTANCE.slimModel.get() ? "§aSLIM" : "CLASSIC", false);
        
        if (FoxyClient.INSTANCE.isFoxyAccount()) {
            String lbl = syncStatus.isEmpty() ? "§fSync to Server" : syncStatus;
            drawShimmerBtn(context, ox, oy+200, 110, 22, lbl, 0x440099FF);
            if (!syncStatus.isEmpty() && System.currentTimeMillis() - syncStatusTime > 3000) syncStatus = "";
        }

        // Preview Area
        int px = x + w - 180, py = y + 10, pw = 170, ph = h - 20;
        context.fillGradient(px, py, px+pw, py+ph, 0x44000000, 0x88000000);
        drawGlowBorder(context, px, py, px+pw, py+ph, COL_ACCENT_DIM);
        drawCenteredText(context, "3D PREVIEW", px+pw/2, py+10, COL_ACCENT);
        
        drawPlayerPreview(context, px + pw/2, py + ph - 45, 65);
    }

    private void drawShimmerBtn(DrawContext context, int x, int y, int w, int h, String text, int color) {
        boolean hov = getMouseX()>=x && getMouseX()<=x+w && getMouseY()>=y && getMouseY()<=y+h;
        context.fill(x, y, x + w, y + h, color);
        if (hov) {
            context.fill(x, y, x + w, y + h, 0x22FFFFFF);
            // Shimmer line
            int sx = x + (int)((System.currentTimeMillis()/5 % (w+40)) - 40);
            context.fillGradient(Math.max(x, sx), y, Math.min(x+w, sx+20), y+h, 0x00FFFFFF, 0x22FFFFFF);
        }
        drawCenteredText(context, text, x + w/2, y + h/2 - 4, COL_TEXT);
    }

    private void drawPlayerPreview(DrawContext context, int x, int y, int size) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            net.minecraft.client.render.entity.state.PlayerEntityRenderState state = new net.minecraft.client.render.entity.state.PlayerEntityRenderState();
            var supplier = mc.getPlayerSkinCache().getSupplier(net.minecraft.component.type.ProfileComponent.ofDynamic(mc.getSession().getUuidOrNull()));
            net.minecraft.entity.player.SkinTextures base = supplier.get().getTextures();
            net.minecraft.util.AssetInfo.TextureAsset skin = base.body();
            net.minecraft.entity.player.PlayerSkinType model = base.model();
            
            String sn = FoxyConfig.INSTANCE.skinName.get();
            if ("Custom".equals(sn)) { net.minecraft.util.Identifier id = SkinResourceManager.getCustomSkinId(); skin = new net.minecraft.util.AssetInfo.TextureAssetInfo(id, id); }
            else if ("Alex".equals(sn)) { skin = new net.minecraft.util.AssetInfo.TextureAssetInfo(net.minecraft.util.Identifier.ofVanilla("entity/player/slim/alex")); model = net.minecraft.entity.player.PlayerSkinType.SLIM; }
            else if ("Steve".equals(sn)) { skin = new net.minecraft.util.AssetInfo.TextureAssetInfo(net.minecraft.util.Identifier.ofVanilla("entity/player/wide/steve")); model = net.minecraft.entity.player.PlayerSkinType.WIDE; }
            
            if (FoxyConfig.INSTANCE.slimModel.get()) model = net.minecraft.entity.player.PlayerSkinType.SLIM;
            else if (!"Default".equals(sn)) model = net.minecraft.entity.player.PlayerSkinType.WIDE;
            
            net.minecraft.util.AssetInfo.TextureAsset cape = base.cape();
            String cn = FoxyConfig.INSTANCE.capeName.get();
            if ("Custom".equals(cn)) { net.minecraft.util.Identifier id = SkinResourceManager.getCustomCapeId(); cape = new net.minecraft.util.AssetInfo.TextureAssetInfo(id, id); }
            else if ("None".equals(cn)) cape = null;
            
            state.skinTextures = new net.minecraft.entity.player.SkinTextures(skin, cape, base.elytra(), model, base.secure());
            state.capeVisible = true;
            state.height = 1.8f;
            state.width = 0.6f;
            state.entityType = net.minecraft.entity.EntityType.PLAYER;
            if (state instanceof net.minecraft.client.render.entity.state.LivingEntityRenderState ls) {
                ls.baseScale = 1.0f; ls.bodyYaw = 180.0f + skinYaw; ls.pitch = -skinPitch;
            }
            org.joml.Quaternionf q = new org.joml.Quaternionf().rotateZ(3.14159f).rotateX(skinPitch * 0.01745f);
            context.addEntity(state, size, new org.joml.Vector3f(0, 0.9f, 0), q, null, x-50, y-150, x+50, y);
        } catch (Exception ignored) {}
    }

    private void renderSkinDropdown(DrawContext context, int x, int y, int w) {
        String[] opts = {"Default", "Steve", "Alex", "Custom"};
        context.fill(x, y, x+w, y+opts.length*20, COL_SIDEBAR);
        for(int i=0; i<opts.length; i++) {
            boolean hov = getMouseX()>=x && getMouseX()<=x+w && getMouseY()>=y+i*20 && getMouseY()<=y+(i+1)*20;
            if(hov) context.fill(x, y+i*20, x+w, y+(i+1)*20, 0x22FFFFFF);
            drawText(context, opts[i].toLowerCase(), x+8, y+i*20+6, FoxyConfig.INSTANCE.skinName.get().equals(opts[i]) ? COL_ACCENT : COL_TEXT_DIM, true);
        }
    }

    private void renderCapeDropdown(DrawContext context, int x, int y, int w) {
        String[] opts = {"Default", "None", "Custom"};
        context.fill(x, y, x+w, y+opts.length*20, COL_SIDEBAR);
        for(int i=0; i<opts.length; i++) {
            boolean hov = getMouseX()>=x && getMouseX()<=x+w && getMouseY()>=y+i*20 && getMouseY()<=y+(i+1)*20;
            if(hov) context.fill(x, y+i*20, x+w, y+(i+1)*20, 0x22FFFFFF);
            drawText(context, opts[i].toLowerCase(), x+8, y+i*20+6, FoxyConfig.INSTANCE.capeName.get().equals(opts[i]) ? COL_ACCENT : COL_TEXT_DIM, true);
        }
    }

    private void renderBaritonePage(DrawContext context, int x, int y, int w, int h, int mx, int my) {
        renderSettingsList(context, BaritoneSettings.SETTINGS, x, y, w, h, mx, my);
    }

    private void renderSettingPage(DrawContext context, int x, int y, int w, int h, int mx, int my) {
        renderSettingsList(context, FoxyConfig.INSTANCE.getSettings(), x, y, w, h, mx, my);
    }

    private void renderSettingsList(DrawContext context, List<Setting<?>> settings, int x, int y, int w, int h, int mx, int my) {
        int sy = y + 5 - (int)scrollAmount;
        for (Setting<?> s : settings) {
            if (sy + 35 < y || sy > y + h) { sy += 35; continue; }
            boolean hov = mx>=x && mx<=x+w && my>=sy && my<=sy+32 && !isMouseOverDropdown(mx, my);
            if (hov) hoveredDesc = s.getDescription();
            context.fill(x, sy, x+w, sy+32, hov ? COL_CARD_HOV : COL_CARD);
            context.fill(x, sy, x+w, sy+1, COL_BORDER);
            context.fill(x, sy, x+w, sy+32, COL_FROST);
            
            drawText(context, s.getName().toLowerCase(), x+8, sy+12, COL_TEXT, true);
            
            if (s instanceof BoolSetting b) drawToggle(context, x+w-24, sy+12, b.get());
            else if (s instanceof NumberSetting n) {
                float r = (float)((n.get()-n.getMin())/(n.getMax()-n.getMin()));
                int sx = x+w-80, sw = 72;
                context.fill(sx, sy+20, sx+sw, sy+22, COL_BORDER); 
                context.fill(sx, sy+20, sx+(int)(sw*r), sy+22, COL_ACCENT);
                context.fill(sx+(int)(sw*r)-2, sy+18, sx+(int)(sw*r)+2, sy+24, COL_TEXT);
                drawText(context, String.format("%.1f", n.get()), sx-textRenderer.getWidth(String.format("%.1f", n.get()))-4, sy+13, COL_ACCENT, true);
            }
            else if (s instanceof ModeSetting md) {
                String str = md.get().toLowerCase();
                drawText(context, str, x+w-textRenderer.getWidth(str)-8, sy+12, COL_ACCENT, true);
            }
            sy += 35;
        }
    }

    private void renderMacroPage(DrawContext context, int x, int y, int w, int h, int mx, int my) {
        CustomKeybinds ck = FoxyClient.INSTANCE.getModuleManager().getModule(CustomKeybinds.class);
        if (ck == null) return;
        Map<Integer, String> macros = ck.getMacros();
        int cy = y + 5 - (int)scrollAmount;
        for (Map.Entry<Integer, String> e : macros.entrySet()) {
            boolean hov = mx >= x && mx <= x+w && my >= cy && my <= cy+22 && !isMouseOverDropdown(mx, my);
            context.fill(x, cy, x+w, cy+22, hov ? COL_CARD_HOV : COL_CARD);
            if (hov) drawShimmerEffect(context, x, cy, w, 22);
            drawText(context, "§b" + getKeyName(e.getKey()).toUpperCase(), x + 10, cy + 6, COL_TEXT, true);
            drawText(context, "§7-> §f" + e.getValue(), x + 85, cy + 6, COL_TEXT, true);
            if (mx >= x+w-30 && my >= cy && my <= cy+22 && !isMouseOverDropdown(mx, my)) drawText(context, "§c[X]", x+w-25, cy+6, 0xFFFF0000, true);
            cy += 26;
        }
        drawShimmerBtn(context, x+w/2-50, y+h-32, 100, 22, "§a+ Add Macro", COL_CARD);
    }

    private void renderWaypointPage(DrawContext context, int x, int y, int w, int h, int mx, int my) {
        WaypointManager wm = FoxyClient.INSTANCE.getWaypointManager();
        int cy = y+5-(int)scrollAmount;
        for (WaypointManager.Waypoint wp : wm.getAll()) {
            boolean hov = mx>=x && mx<=x+w && my>=cy && my<=cy+22 && !isMouseOverDropdown(mx, my);
            context.fill(x, cy, x+w, cy+22, hov ? COL_CARD_HOV : COL_CARD);
            if (hov) drawShimmerEffect(context, x, cy, w, 22);
            drawText(context, "§b" + wp.name(), x+10, cy+6, COL_TEXT, true);
            drawText(context, String.format("§7%d, %d, %d §8(%s)", wp.x(), wp.y(), wp.z(), wp.dimension().replace("minecraft:", "")), x+100, cy+6, 0xFFBBBBBB, true);
            if (mx>=x+w-30 && my>=cy && my<=cy+22 && !isMouseOverDropdown(mx, my)) drawText(context, "§c[X]", x+w-25, cy+6, 0xFFFF0000, true);
            cy += 26;
        }
        drawShimmerBtn(context, x+w/2-55, y+h-32, 110, 22, "§a+ Add Waypoint", COL_CARD);
    }

    private void renderGrid(DrawContext context, int x, int y, int w, int h, int mx, int my) {
        List<Module> mods = getFilteredModules();
        int cw = (w - 3*CARD_GAP)/4, cy = y - (int)scrollAmount;
        for (int i=0; i<mods.size(); i++) {
            int col = i % 4, cx = x + col*(cw+CARD_GAP);
            int ch = (expandedModule == mods.get(i)) ? 65+(mods.get(i).getSettings().size()*22)+20 : 65;
            if (cy+ch > y && cy < y+h) renderModuleCard(context, mods.get(i), cx, cy, cw, ch, mx, my);
            if (col == 3 || i == mods.size()-1) {
                int rH = 65; for (int j=i-(i%4); j<=i; j++) rH = Math.max(rH, (expandedModule == mods.get(j)) ? 65+(mods.get(j).getSettings().size()*22)+20 : 65);
                cy += rH + CARD_GAP;
            }
        }
    }

    private int getTotalContentHeight(int w) {
        if (selectedSidebar.equals("Macro")) {
            CustomKeybinds ck = FoxyClient.INSTANCE.getModuleManager().getModule(CustomKeybinds.class);
            return (ck != null ? ck.getMacros().size() : 0) * 26 + 10;
        }
        if (selectedSidebar.equals("Waypoint")) return FoxyClient.INSTANCE.getWaypointManager().getAll().size() * 26 + 10;
        if (selectedSidebar.equals("Baritone")) return BaritoneSettings.SETTINGS.size() * 35 + 10;
        if (selectedSidebar.equals("Setting")) return FoxyConfig.INSTANCE.getSettings().size() * 35 + 10;
        if (selectedSidebar.equals("Cheat")) {
            List<Module> mods = getFilteredModules();
            int cw = (w - 3*CARD_GAP)/4, th = 0, rowH = 0;
            for (int i=0; i<mods.size(); i++) {
                int col = i % 4;
                int ch = (expandedModule == mods.get(i)) ? 65+(mods.get(i).getSettings().size()*22)+20 : 65;
                rowH = Math.max(rowH, ch);
                if (col == 3 || i == mods.size()-1) { th += rowH + CARD_GAP; rowH = 0; }
            }
            return th;
        }
        return height;
    }

    private void drawScrollbar(DrawContext context, int x, int y, int w, int h) {
        int totalH = getTotalContentHeight(w);
        if (totalH <= h) return;
        int trackX = x + w + 12, trackW = 2;
        context.fill(trackX, y, trackX + trackW, y + h, 0x22FFFFFF);
        int th = Math.max(20, (int)((h / (float)totalH) * h));
        int ty = (int)((scrollAmount / (float)(totalH - h)) * (h - th));
        context.fill(trackX, y + ty, trackX + trackW, y + ty + th, COL_ACCENT);
    }

    private void renderModuleCard(DrawContext context, Module m, int x, int y, int w, int h, int mx, int my) {
        boolean hov = mx>=x && mx<=x+w && my>=y && my<=y+h && !isMouseOverDropdown(mx, my);
        if (hov) hoveredDesc = m.getDescription();
        context.fill(x, y, x+w, y+h, hov ? COL_CARD_HOV : COL_CARD);
        context.fill(x, y, x+w, y+1, m.isEnabled() ? COL_ACCENT : COL_BORDER);
        context.fill(x, y, x+w, y+h, COL_FROST); // Card frost
        if (hov) drawShimmerEffect(context, x, y, w, h);
        drawText(context, m.getName().toLowerCase(), x+8, y+8, m.isEnabled() ? COL_ACCENT : COL_TEXT, true);
        drawText(context, m.getCategory().getDisplayName().toLowerCase(), x+8, y+18, COL_TEXT_DIM, true);
        drawText(context, (expandedModule == m ? "§b▲" : "§7▼"), x+w-15, y+8, COL_TEXT_DIM, true);
        
        drawToggle(context, x+w-24, y+24, m.isEnabled());

        if (expandedModule == m) {
            context.fill(x+5, y+42, x+w-5, y+43, COL_BORDER);
            int sy = y+50;
            for (Setting<?> s : m.getSettings()) {
                boolean settingHov = mx>=x && mx<=x+w && my>=sy && my<=sy+20 && !isMouseOverDropdown(mx, my);
                if (settingHov) hoveredDesc = s.getDescription();
                drawText(context, s.getName().toLowerCase(), x+8, sy, COL_TEXT_DIM, true);
                if (s instanceof BoolSetting b) drawToggle(context, x+w-24, sy, b.get());
                else if (s instanceof ColorSetting cs) {
                    context.fill(x+w-20, sy, x+w-8, sy+8, cs.get().getRGB() | 0xFF000000);
                    drawGlowBorder(context, x+w-20, sy, x+w-8, sy+8, COL_BORDER);
                }
                else if (s instanceof NumberSetting n) {
                    float r = (float)((n.get()-n.getMin())/(n.getMax()-n.getMin()));
                    int sx = x+w-60, sw = 52;
                    context.fill(sx, sy+3, sx+sw, sy+5, COL_BORDER); 
                    context.fill(sx, sy+3, sx+(int)(sw*r), sy+5, COL_ACCENT);
                    context.fill(sx+(int)(sw*r)-2, sy+1, sx+(int)(sw*r)+2, sy+7, COL_TEXT); // Knob
                    drawText(context, String.format("%.1f", n.get()), sx-textRenderer.getWidth(String.format("%.1f", n.get()))-4, sy, COL_ACCENT, true);
                }
                else if (s instanceof ModeSetting md) { String str = md.get().toLowerCase(); drawText(context, str, x+w-textRenderer.getWidth(str)-8, sy, COL_ACCENT, true); }
                sy += 22;
            }
            drawCenteredText(context, "§8bind: §b" + getKeyName(m.getKeybind()), x+w/2, y+h-12, 0xFF555555);
        }
    }

    private void drawShimmerEffect(DrawContext context, int x, int y, int w, int h) {
        int sx = x + (int)((System.currentTimeMillis()/3 % (w+60)) - 60);
        context.fillGradient(Math.max(x, sx), y, Math.min(x+w, sx+25), y+h, 0x00FFFFFF, 0x11FFFFFF);
    }

    private void renderModal(DrawContext context, String title, String sub) {
        int x = width/2-110, y = height/2-35, w = 220, h = 70;
        context.fillGradient(x, y, x+w, y+h, 0xEE0B0E14, 0xDD11151C);
        drawGlowBorder(context, x, y, x+w, y+h, COL_ACCENT);
        drawCenteredText(context, title, width/2, y+18, COL_TEXT);
        drawCenteredText(context, sub, width/2, y+42, COL_TEXT_DIM);
    }

    private void renderInputModal(DrawContext context, String title, String input) {
        int x = width/2-110, y = height/2-35, w = 220, h = 70;
        context.fillGradient(x, y, x+w, y+h, 0xEE0B0E14, 0xDD11151C);
        drawGlowBorder(context, x, y, x+w, y+h, COL_ACCENT);
        drawCenteredText(context, title, width/2, y+12, COL_TEXT);
        context.fill(x+15, y+35, x+w-15, y+55, 0xAA050709);
        drawText(context, input + "_", x+20, y+41, COL_ACCENT, true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mx = click.x(), my = click.y();
        if (listeningKeybind != null || addingMacroStep > 0 || addingWaypoint) return true;
        
        if (activeColorPicker != null) {
            if (mx >= pickerX && mx <= pickerX + 130 && my >= pickerY && my <= pickerY + 150) {
                // Inside picker
                float[] hsb = activeColorPicker.getHSB();
                int a = activeColorPicker.getAlpha();
                
                if (mx >= pickerX + 8 && mx <= pickerX + 108 && my >= pickerY + 25 && my <= pickerY + 125) {
                    isDraggingSB = true;
                    float s = MathHelper.clamp((float)(mx - (pickerX + 8)) / 100f, 0, 1);
                    float b = MathHelper.clamp(1.0f - (float)(my - (pickerY + 25)) / 100f, 0, 1);
                    activeColorPicker.setHSB(hsb[0], s, b, a);
                } else if (mx >= pickerX + 112 && mx <= pickerX + 118 && my >= pickerY + 25 && my <= pickerY + 125) {
                    isDraggingHue = true;
                    float h = MathHelper.clamp((float)(my - (pickerY + 25)) / 100f, 0, 1);
                    activeColorPicker.setHSB(h, hsb[1], hsb[2], a);
                } else if (mx >= pickerX + 122 && mx <= pickerX + 128 && my >= pickerY + 25 && my <= pickerY + 125) {
                    isDraggingAlpha = true;
                    int na = (int)(MathHelper.clamp((float)(my - (pickerY + 25)) / 100f, 0, 1) * 255);
                    activeColorPicker.setHSB(hsb[0], hsb[1], hsb[2], na);
                }
                return true;
            } else {
                activeColorPicker = null;
            }
        }

        if (stateDropdownOpen) {
            int sx = width-PADDING-150, tx = sx-205, ty = PADDING+34;
            if (mx>=tx && mx<=tx+85 && my>=ty && my<=ty+StateFilter.values().length*20) {
                stateFilter = StateFilter.values()[(int)((my-ty)/20)];
                stateDropdownOpen = false; 
                return true; 
            } stateDropdownOpen = false; 
        }
        if (categoryDropdownOpen) {
            List<Category> rel = getRelevantCategories(selectedSidebar);
            int sx = width-PADDING-150, tx = sx-110, ty = PADDING+34;
            if (mx>=tx && mx<=tx+100 && my>=ty && my<=ty+rel.size()*20+20) {
                int idx = (int)((my-ty)/20);
                selectedCategory = (idx==0)?null:rel.get(idx-1);
                categoryDropdownOpen=false; targetScroll=0; return true; 
            }
            categoryDropdownOpen = false; 
        }
        if (skinDropdownOpen) {
            int tx = gx()+10, ty = gy()+10+15+20;
            if(mx>=tx && mx<=tx+110 && my>=ty && my<=ty+80) {
                String[] o={"Default","Steve","Alex","Custom"};
                FoxyConfig.INSTANCE.skinName.set(o[(int)((my-ty)/20)]);
                skinDropdownOpen=false; return true; 
            }
            skinDropdownOpen=false; 
        }
        if (capeDropdownOpen) {
            int tx = gx()+10, ty = gy()+10+75+15+20;
            if(mx>=tx && mx<=tx+110 && my>=ty && my<=ty+60) {
                String[] o={"Default","None","Custom"};
                FoxyConfig.INSTANCE.capeName.set(o[(int)((my-ty)/20)]);
                capeDropdownOpen=false; return true; 
            }
            capeDropdownOpen=false; 
        }

        if (mx >= PADDING && mx <= PADDING + SIDEBAR_WIDTH) {
            int sy = PADDING + 75;
            for (String item : SIDEBAR_ITEMS) {
                if (my>=sy && my<=sy+22) {
                    selectedSidebar=item;
                    selectedCategory=null;
                    targetScroll=0;
                    scrollAmount=0;
                    return true; 
                }
                sy += 26;
            }
        }

        int sx = width-PADDING-150, ty = PADDING+14;
        if (mx>=sx && mx<=sx+135 && my>=ty && my<=ty+20) {
            searchFocused=true;
            return true; 
        }
        searchFocused=false;
        if (selectedSidebar.equals("Cheat")) {
            int bx = sx - 110;
            if (mx >= bx - 95 && mx <= bx - 10 && my >= ty && my <= ty + 20) { stateDropdownOpen = !stateDropdownOpen; categoryDropdownOpen = false; return true; }
            if (mx >= bx && mx <= bx + 100 && my >= ty && my <= ty + 20) { categoryDropdownOpen = !categoryDropdownOpen; stateDropdownOpen = false; return true; }
        }

        int gx = gx(), gy = gy(), gw = gw(), gh = gh();
        if (selectedSidebar.equals("Cosmetic")) {
            int ox=gx+20, oy=gy+20;
            if(mx>=ox && mx<=ox+110 && my>=oy+15 && my<=oy+35) {
                skinDropdownOpen=true;
                return true; 
            }
            if(mx>=ox && mx<=ox+110 && my>=oy+40 && my<=oy+60) {
                openFileSelector("Select Skin", true);
                return true; 
            }
            if(mx>=ox && mx<=ox+110 && my>=oy+90 && my<=oy+110) {
                capeDropdownOpen=true;
                return true; 
            }
            if(mx>=ox && mx<=ox+110 && my>=oy+115 && my<=oy+135) {
                openFileSelector("Select Cape", false);
                return true; 
            }
            if(mx>=ox && mx<=ox+110 && my>=oy+165 && my<=oy+185) {
                FoxyConfig.INSTANCE.slimModel.set(!FoxyConfig.INSTANCE.slimModel.get());
                return true; 
            }
            if(FoxyClient.INSTANCE.isFoxyAccount() && mx>=ox && mx<=ox+110 && my>=oy+200 && my<=oy+222) {
                syncStatus = "§eSyncing...";
                syncStatusTime = System.currentTimeMillis();
                new Thread(() -> {
                    boolean s = uploadCosmetics();
                    MinecraftClient.getInstance().execute(() -> {
                        syncStatus = s ? "§aSuccess!" : "§cFailed";
                        syncStatusTime = System.currentTimeMillis();
                    });
                }).start();
                return true; 
            }
            if(mx>=gx+gw-180 && my>=gy+10 && my<=gy+gh-10) {
                isDraggingSkin=true; return true; 
            }
        } else if (selectedSidebar.equals("Macro")) {
            if (mx>=gx+gw/2-50 && mx<=gx+gw/2+50 && my>=gy+gh-32 && my<=gy+gh-10) {
                addingMacroStep=1; return true; 
            }
            CustomKeybinds ck = FoxyClient.INSTANCE.getModuleManager().getModule(CustomKeybinds.class); int cy = gy+5-(int)scrollAmount;
            for(Map.Entry<Integer,String>e : ck.getMacros().entrySet()) {
                if(mx>=gx+gw-30 && my>=cy && my<=cy+22) {
                    ck.removeMacro(e.getKey()); return true; 
                }
                if(mx>=gx && mx<=gx+gw-40 && my>=cy && my<=cy+22) {
                    tempMacroKey = e.getKey();
                    tempTextBuffer = e.getValue();
                    addingMacroStep = 2;
                    return true;
                }
                cy += 26; 
            }
        } else if (selectedSidebar.equals("Waypoint")) {
            if (mx>=gx+gw/2-55 && mx<=gx+gw/2+55 && my>=gy+gh-32 && my<=gy+gh-10) {
                addingWaypoint=true; wpInputIdx=0; tempTextBuffer=""; return true; 
            }
            WaypointManager wm = FoxyClient.INSTANCE.getWaypointManager(); int cy = gy+5-(int)scrollAmount;
            for(WaypointManager.Waypoint wp : wm.getAll()) {
                if(mx>=gx+gw-30 && my>=cy && my<=cy+22) {
                    wm.remove(wp.name()); return true; 
                }
                cy += 26; 
            }
        } else if (selectedSidebar.equals("Baritone") || selectedSidebar.equals("Setting")) {
            List<Setting<?>> settings = selectedSidebar.equals("Baritone") ? BaritoneSettings.SETTINGS : FoxyConfig.INSTANCE.getSettings();
            int sy = gy+20+5-(int)scrollAmount;
            for (Setting<?> s : settings) {
                if (mx>=gx && mx<=gx+gw && my>=sy && my<=sy+32) {
                    if (s instanceof BoolSetting b) b.set(!b.get());
                    else if (s instanceof ModeSetting md) md.cycle();
                    else if (s instanceof NumberSetting n) {
                        float r = MathHelper.clamp((float)((mx-(gx+gw-80))/72), 0, 1);
                        n.set(Math.round((n.getMin() + r*(n.getMax()-n.getMin()))*10.0)/10.0);
                    }
                    return true;
                }
                sy += 35;
            }
        } else {
            int cw = (gw-3*CARD_GAP)/4; List<Module> mods = getFilteredModules(); int cy = gy-(int)scrollAmount;
            for (int i=0; i<mods.size(); i++) {
                int cx = gx + (i%4)*(cw+CARD_GAP); int ch = (expandedModule == mods.get(i)) ? 65+(mods.get(i).getSettings().size()*22)+20 : 65;
                if (mx>=cx && mx<=cx+cw && my>=cy && my<=cy+ch) {
                    Module m = mods.get(i); if (my < cy+22) {
                        expandedModule=(expandedModule==m)?null:m; return true; 
                    }
                    if (mx>=cx+cw-30 && my>=cy+22 && my<=cy+35) {
                        m.toggle(); return true; 
                    }
                    if (expandedModule == m) {
                        int sy = cy+50; for (Setting<?> s : m.getSettings()) {
                            if (my>=sy && my<=sy+20) {
                                if (s instanceof BoolSetting b) { if (mx >= cx+cw-30) b.set(!b.get()); }
                                 else if (s instanceof ColorSetting cs) {
                                    if (mx >= cx+cw-20) {
                                        activeColorPicker = (activeColorPicker == cs) ? null : cs;
                                        pickerX = (int)mx;
                                        pickerY = (int)my;
                                        if (pickerX + 130 > width) pickerX -= 140;
                                        if (pickerY + 150 > height) pickerY -= 150;
                                        return true;
                                    }
                                }
                                else if (s instanceof ModeSetting md) md.cycle();
                                else if (s instanceof NumberSetting n) {
                                    float r = MathHelper.clamp((float)((mx-(cx+cw-60))/52), 0, 1);
                                    n.set(Math.round((n.getMin() + r*(n.getMax()-n.getMin()))*10.0)/10.0);
                                }
                                return true; 
                            }
                            sy += 22;
                        }
                        if (my>=cy+ch-25) {
                            listeningKeybind=m; return true; 
                        }
                    }
                    return true;
                }
                if (i%4==3 || i==mods.size()-1) {
                    int rH=65;
                    for(int j=i-(i%4); j<=i; j++) {
                        rH=Math.max(rH, (expandedModule==mods.get(j))?65+(mods.get(j).getSettings().size()*22)+20:65);
                    }
                    cy+=rH+CARD_GAP;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    private int gx() { return PADDING + SIDEBAR_WIDTH + 20; }
    private int gy() { return PADDING + TOPBAR_HEIGHT + 20; }
    private int gw() { return width - PADDING*2 - SIDEBAR_WIDTH - 40; }
    private int gh() { return height - PADDING*2 - TOPBAR_HEIGHT - 40; }

    @Override 
    public boolean mouseDragged(Click c, double dx, double dy) { 
        if (isDraggingSkin) { skinYaw -= (float)dx*2; skinPitch = MathHelper.clamp(skinPitch - (float)dy*2, -90, 90); return true; } 
        if (activeColorPicker != null) {
            double mx = c.x(), my = c.y();
            float[] hsb = activeColorPicker.getHSB();
            int a = activeColorPicker.getAlpha();
            
            if (isDraggingSB) {
                float s = MathHelper.clamp((float)(mx - (pickerX + 8)) / 100f, 0, 1);
                float b = MathHelper.clamp(1.0f - (float)(my - (pickerY + 25)) / 100f, 0, 1);
                activeColorPicker.setHSB(hsb[0], s, b, a);
                return true;
            }
            if (isDraggingHue) {
                float h = MathHelper.clamp((float)(my - (pickerY + 25)) / 100f, 0, 1);
                activeColorPicker.setHSB(h, hsb[1], hsb[2], a);
                return true;
            }
            if (isDraggingAlpha) {
                int na = (int)(MathHelper.clamp((float)(my - (pickerY + 25)) / 100f, 0, 1) * 255);
                activeColorPicker.setHSB(hsb[0], hsb[1], hsb[2], na);
                return true;
            }
        }
        return super.mouseDragged(c, dx, dy); 
    }
    
    @Override 
    public boolean mouseReleased(Click c) { 
        isDraggingSkin = false; 
        isDraggingHue = false; 
        isDraggingAlpha = false; 
        isDraggingSB = false; 
        return super.mouseReleased(c); 
    }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { targetScroll = Math.max(0, targetScroll - (float)v * 40); return true; }

    @Override
    public boolean keyPressed(KeyInput k) {
        int key = k.key();
        if (listeningKeybind != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
                listeningKeybind.setKeybind(0);
                listeningKeybind = null;
            } else {
                listeningKeybind.setKeybind(key);
                listeningKeybind = null;
            }
            return true;
        }
        if (addingMacroStep == 1) {
            if (key == GLFW.GLFW_KEY_ESCAPE) addingMacroStep = 0;
            else {
                tempMacroKey = key;
                addingMacroStep = 2;
                tempTextBuffer = "/";
            }
            return true;
        }
        if (addingMacroStep == 2 || addingWaypoint) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                addingMacroStep = 0;
                addingWaypoint = false;
            }
            else if (key == GLFW.GLFW_KEY_ENTER) {
                if (addingMacroStep == 2) {
                    FoxyClient.INSTANCE.getModuleManager().getModule(CustomKeybinds.class).addMacro(tempMacroKey, tempTextBuffer);
                    addingMacroStep = 0;
                }
                else {
                    wpBuffer[wpInputIdx] = tempTextBuffer;
                    if (wpInputIdx < 3) {
                        wpInputIdx++;
                        tempTextBuffer = wpBuffer[wpInputIdx];
                    } else {
                        try {
                            FoxyClient.INSTANCE.getWaypointManager().add(wpBuffer[0], Integer.parseInt(wpBuffer[1]), Integer.parseInt(wpBuffer[2]), Integer.parseInt(wpBuffer[3]), "minecraft:overworld");
                        } catch(Exception ignored) {}
                        addingWaypoint = false;
                    }
                }
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && !tempTextBuffer.isEmpty()) {
                tempTextBuffer = tempTextBuffer.substring(0, tempTextBuffer.length()-1);
            }
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length()-1);
                if(searchQuery.isEmpty()) tempTextBuffer="";
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(k);
    }

    @Override public boolean charTyped(CharInput c) { if ((addingMacroStep == 2 || addingWaypoint || searchFocused) && c.isValidChar()) { if(searchFocused) { searchQuery += c.asString(); } else { tempTextBuffer += c.asString(); } return true; } return false; }
    @Override public boolean shouldPause() { return false; }

    private void openFileSelector(String title, boolean skin) {
        new Thread(() -> {
            org.lwjgl.PointerBuffer filters = org.lwjgl.system.MemoryUtil.memAllocPointer(1);
            filters.put(org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8("*.png"))); filters.flip();
            String res = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(title, System.getProperty("user.home")+File.separator, filters, "PNG Images", false);
            org.lwjgl.system.MemoryUtil.memFree(filters);
            if (res != null) { File f = new File(res); if (f.exists()) {
                MinecraftClient.getInstance().execute(() -> {
                    if (skin) { SkinResourceManager.setCustomSkin(f.getAbsolutePath()); FoxyConfig.INSTANCE.skinName.set("Custom"); }
                    else { SkinResourceManager.setCustomCape(f.getAbsolutePath()); FoxyConfig.INSTANCE.capeName.set("Custom"); }
                    FoxyConfig.INSTANCE.save();
                });
            }}
        }).start();
    }

    private boolean uploadCosmetics() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            String token = mc.getSession().getAccessToken();
            if (token == null || token.isEmpty()) return false;

            String boundary = Long.toHexString(System.currentTimeMillis());
            String CRLF = "\r\n";
            URL url = new URL("https://foxyclient.qzz.io/api/profiles/foxyclient/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)) {
                // Token
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"accesstoken\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(CRLF);
                writer.append(CRLF).append(token).append(CRLF).flush();

                // Variant
                String variant = FoxyConfig.INSTANCE.slimModel.get() ? "slim" : "default";
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"variant\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(CRLF);
                writer.append(CRLF).append(variant).append(CRLF).flush();

                String sn = FoxyConfig.INSTANCE.skinName.get();
                if (sn.equals("Custom") || sn.equals("Default")) {
                    appendFile(writer, out, boundary, "skin", sn.equals("Custom") ? "custom_skin.png" : "default_skin.png");
                }
                
                String cn = FoxyConfig.INSTANCE.capeName.get();
                if (cn.equals("Custom") || cn.equals("Default")) {
                    appendFile(writer, out, boundary, "cape", cn.equals("Custom") ? "custom_cape.png" : "default_cape.png");
                }
                
                writer.append("--" + boundary + "--").append(CRLF).flush();
            }
            return conn.getResponseCode() == 200 || conn.getResponseCode() == 204;
        } catch (Exception e) { return false; }
    }

    private void appendFile(PrintWriter writer, OutputStream out, String boundary, String name, String filename) throws IOException {
        Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/foxyclient").resolve(filename);
        if (Files.exists(path)) {
            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\""+name+"\"; filename=\""+filename+"\"").append("\r\n");
            writer.append("Content-Type: image/png").append("\r\n\r\n").flush();
            Files.copy(path, out); out.flush();
            writer.append("\r\n").flush();
        }
    }

    private String getKeyName(int key) {
        if (key <= 0) return "NONE";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            default -> "KEY " + key;
        };
    }

    private List<Category> getRelevantCategories(String sb) {
        return switch(sb) { case "Cheat" -> List.of(Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.WORLD, Category.EXPLOIT, Category.MISC); case "Cosmetic" -> List.of(Category.RENDER); case "UI" -> List.of(Category.UI); default -> List.of(); };
    }

    private List<Module> getFilteredModules() {
        return FoxyClient.INSTANCE.getModuleManager().getModules().stream()
            .filter(m -> { List<Category> rel = getRelevantCategories(selectedSidebar); return selectedCategory != null ? m.getCategory() == selectedCategory : rel.contains(m.getCategory()); })
            .filter(m -> stateFilter == StateFilter.ALL || (stateFilter==StateFilter.ENABLED && m.isEnabled()) || (stateFilter==StateFilter.DISABLED && !m.isEnabled()))
            .filter(m -> m.getName().toLowerCase().contains(searchQuery.toLowerCase())).collect(Collectors.toList());
    }

    private boolean isMouseOverDropdown(double mx, double my) {
        int sx = width - PADDING - 150, ty = PADDING + 34;
        if (stateDropdownOpen) { int tx=sx-205; if(mx>=tx && mx<=tx+85 && my>=ty && my<=ty+StateFilter.values().length*20) return true; }
        if (categoryDropdownOpen) { List<Category> rel=getRelevantCategories(selectedSidebar); int tx=sx-110; if(mx>=tx && mx<=tx+100 && my>=ty && my<=ty+(rel.size()+1)*20) return true; }
        if (skinDropdownOpen) { int tx=gx()+10, dty=gy()+10+45; if(mx>=tx && mx<=tx+110 && my>=dty && my<=dty+80) return true; }
        if (capeDropdownOpen) { int tx=gx()+10, dty=gy()+10+120; if(mx>=tx && mx<=tx+110 && my>=dty && my<=dty+60) return true; }
        return false;
    }

    private void renderStateDropdown(DrawContext context, int x, int y, int w) {
        int h = StateFilter.values().length * 20; context.fill(x, y, x+w, y+h, COL_SIDEBAR); context.fill(x, y+h-1, x+w, y+h, COL_ACCENT);
        for (int i=0; i<StateFilter.values().length; i++) {
            StateFilter f = StateFilter.values()[i]; boolean hov = getMouseX()>=x && getMouseX()<=x+w && getMouseY()>=y+i*20 && getMouseY()<=y+(i+1)*20;
            if (hov) context.fill(x, y+i*20, x+w, y+(i+1)*20, 0x22FFFFFF);
            drawText(context, f.name().toLowerCase(), x+8, y+i*20+6, f == stateFilter ? COL_ACCENT : COL_TEXT_DIM, true);
        }
    }

    private void renderCategoryDropdown(DrawContext context, int x, int y, int w) {
        List<Category> rel = getRelevantCategories(selectedSidebar); int h = (rel.size() + 1) * 20;
        context.fill(x, y, x+w, y+h, COL_SIDEBAR); context.fill(x, y+h-1, x+w, y+h, COL_ACCENT);
        boolean hovAll = getMouseX()>=x && getMouseX()<=x+w && getMouseY()>=y && getMouseY()<=y+20;
        if (hovAll) context.fill(x, y, x+w, y+20, 0x22FFFFFF);
        drawText(context, "all", x+8, y+6, selectedCategory == null ? COL_ACCENT : COL_TEXT_DIM, true);
        for (int i=0; i<rel.size(); i++) {
            Category c = rel.get(i); int iy = y+20+i*20; boolean hov = getMouseX()>=x && getMouseX()<=x+w && getMouseY()>=iy && getMouseY()<=iy+20;
            if (hov) context.fill(x, iy, x+w, iy+20, 0x22FFFFFF);
            drawText(context, c.getDisplayName().toLowerCase(), x+8, iy+6, c == selectedCategory ? COL_ACCENT : COL_TEXT_DIM, true);
        }
    }

    private void renderColorPicker(DrawContext context, int x, int y) {
        int w = 130, h = 150;
        float[] hsb = activeColorPicker.getHSB();
        int a = activeColorPicker.getAlpha();
        
        context.fill(x, y, x + w, y + h, 0xEE0B0E14);
        drawGlowBorder(context, x, y, x + w, y + h, COL_BORDER);
        drawText(context, activeColorPicker.getName().toLowerCase(), x + 8, y + 8, COL_ACCENT, true);

        // HSB Square
        int sx = x + 8, sy = y + 25, sw = 100, sh = 100;
        int hueColor = Color.getHSBColor(hsb[0], 1.0f, 1.0f).getRGB();
        
        // Base Hue
        context.fill(sx, sy, sx + sw, sy + sh, hueColor);
        // White to Transparent (Saturation)
        context.fillGradient(sx, sy, sx + sw, sy + sh, 0xFFFFFFFF, 0x00FFFFFF);
        // Transparent to Black (Brightness)
        context.fillGradient(sx, sy, sx + sw, sy + sh, 0x00000000, 0xFF000000);
        
        // Hue Slider
        int hx = x + 112, hw = 6;
        for (int i = 0; i < 100; i++) {
            context.fill(hx, sy + i, hx + hw, sy + i + 1, Color.getHSBColor(i / 100f, 1.0f, 1.0f).getRGB());
        }
        
        // Alpha Slider
        int ax = x + 122, aw = 6;
        context.fillGradient(ax, sy, ax + aw, sy + sh, 0xFFFFFFFF, 0x00FFFFFF);
        
        // Selection markers
        int markerX = sx + (int)(hsb[1] * sw);
        int markerY = sy + (int)((1.0f - hsb[2]) * sh);
        context.fill(markerX - 1, markerY - 1, markerX + 1, markerY + 1, 0xFFFFFFFF);
        
        context.fill(hx - 1, sy + (int)(hsb[0] * 100) - 1, hx + hw + 1, sy + (int)(hsb[0] * 100) + 1, 0xFFFFFFFF);
        context.fill(ax - 1, sy + (int)(a / 255f * 100) - 1, ax + aw + 1, sy + (int)(a / 255f * 100) + 1, 0xFFFFFFFF);
        
        // Detailed Info
        drawText(context, String.format("#%02x%02x%02x%02x", (int)(a), (int)(hsb[0]*255), (int)(hsb[1]*255), (int)(hsb[2]*255)), x + 8, y + 135, COL_TEXT_DIM, true);
    }

    private void drawText(DrawContext context, String text, int x, int y, int color, boolean shadow) {
        context.drawText(textRenderer, Text.literal(text).setStyle(Style.EMPTY.withFont(new net.minecraft.text.StyleSpriteSource.Font(OUTFIT_FONT))), x, y, color, shadow);
    }

    private void drawCenteredText(DrawContext context, String text, int centerX, int y, int color) {
        Text t = Text.literal(text).setStyle(Style.EMPTY.withFont(new net.minecraft.text.StyleSpriteSource.Font(OUTFIT_FONT)));
        context.drawText(textRenderer, t, centerX - textRenderer.getWidth(t) / 2, y, color, true);
    }

    private void drawToggle(DrawContext context, int x, int y, boolean enabled) {
        context.fill(x, y, x + 16, y + 8, enabled ? 0x6600E5CC : 0x4430363D);
        context.fill(x + (enabled ? 10 : 2), y + 2, x + (enabled ? 14 : 6), y + 6, COL_TEXT);
    }

    private int getMouseX() { return (int) (MinecraftClient.getInstance().mouse.getX() * (double) width / (double) MinecraftClient.getInstance().getWindow().getWidth()); }
    private int getMouseY() { return (int) (MinecraftClient.getInstance().mouse.getY() * (double) height / (double) MinecraftClient.getInstance().getWindow().getHeight()); }

    private enum StateFilter { ALL, ENABLED, DISABLED }
}
