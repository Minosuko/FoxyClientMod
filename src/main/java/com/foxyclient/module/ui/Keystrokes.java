package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

/**
 * Displays pressed keys (WASD, space, mouse buttons) as an overlay on the HUD.
 */
public class Keystrokes extends Module {
    private final NumberSetting posX = addSetting(new NumberSetting("X", "Horizontal position", 5.0, 0.0, 500.0));
    private final NumberSetting posY = addSetting(new NumberSetting("Y", "Vertical position", 50.0, 0.0, 500.0));
    private final NumberSetting scale = addSetting(new NumberSetting("Scale", "Size multiplier", 1.0, 0.5, 2.0));
    private final BoolSetting showMouse = addSetting(new BoolSetting("Mouse", "Show mouse buttons", true));
    private final BoolSetting showSpace = addSetting(new BoolSetting("Spacebar", "Show spacebar", true));
    private final BoolSetting showSneak = addSetting(new BoolSetting("Sneak", "Show sneak key", true));

    // Key box dimensions (before scale)
    private static final int BOX_SIZE = 22;
    private static final int GAP = 2;

    // Colors
    private static final int BG_PRESSED = 0xCC3498DB;     // Blue when pressed
    private static final int BG_RELEASED = 0x80000000;    // Dark translucent when released
    private static final int TEXT_PRESSED = 0xFFFFFFFF;   // White text when pressed
    private static final int TEXT_RELEASED = 0xBBBBBBBB;  // Gray text when released

    public Keystrokes() {
        super("Keystrokes", "Displays pressed keys on screen", Category.UI);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        DrawContext ctx = event.getContext();
        TextRenderer tr = mc.textRenderer;
        float s = scale.get().floatValue();
        int x = posX.get().intValue();
        int y = posY.get().intValue();

        int boxW = (int)(BOX_SIZE * s);
        int gap = (int)(GAP * s);
        int stride = boxW + gap;

        // ─── Row 1: W (centered) ─────────────────────────────
        boolean w = mc.options.forwardKey.isPressed();
        drawKeyBox(ctx, tr, x + stride, y, boxW, "W", w, s);

        // ─── Row 2: A S D ────────────────────────────────────
        int row2Y = y + stride;
        boolean a = mc.options.leftKey.isPressed();
        boolean sKey = mc.options.backKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();
        drawKeyBox(ctx, tr, x, row2Y, boxW, "A", a, s);
        drawKeyBox(ctx, tr, x + stride, row2Y, boxW, "S", sKey, s);
        drawKeyBox(ctx, tr, x + stride * 2, row2Y, boxW, "D", d, s);

        int currentY = row2Y + stride;

        // ─── Row 3: Mouse buttons (LMB / RMB) ───────────────
        if (showMouse.get()) {
            boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            int totalW = boxW * 3 + gap * 2;
            int halfW = (totalW - gap) / 2;
            int rmbW = totalW - gap - halfW;
            
            drawKeyBox(ctx, tr, x, currentY, halfW, "LMB", lmb, s);
            drawKeyBox(ctx, tr, x + halfW + gap, currentY, rmbW, "RMB", rmb, s);
            currentY += stride;
        }

        // ─── Row 4: Spacebar ─────────────────────────────────
        if (showSpace.get()) {
            boolean space = mc.options.jumpKey.isPressed();
            int totalW = boxW * 3 + gap * 2;
            drawKeyBox(ctx, tr, x, currentY, totalW, "—", space, s);
            currentY += stride;
        }

        // ─── Row 5: Sneak ────────────────────────────────────
        if (showSneak.get()) {
            boolean sneak = mc.options.sneakKey.isPressed();
            int totalW = boxW * 3 + gap * 2;
            drawKeyBox(ctx, tr, x, currentY, totalW, "Sneak", sneak, s);
        }
    }

    private void drawKeyBox(DrawContext ctx, TextRenderer tr, int x, int y, int width, String label, boolean pressed, float s) {
        int height = (int)(BOX_SIZE * s);
        int bg = pressed ? BG_PRESSED : BG_RELEASED;
        int textColor = pressed ? TEXT_PRESSED : TEXT_RELEASED;

        // Background
        ctx.fill(x, y, x + width, y + height, bg);

        // Border
        int borderColor = pressed ? 0xFF5DADE2 : 0x40FFFFFF;
        ctx.drawHorizontalLine(x, x + width - 1, y, borderColor);
        ctx.drawHorizontalLine(x, x + width - 1, y + height - 1, borderColor);
        ctx.drawVerticalLine(x, y, y + height - 1, borderColor);
        ctx.drawVerticalLine(x + width - 1, y, y + height - 1, borderColor);

        // Centered text
        int textW = tr.getWidth(label);
        int textX = x + (width - textW) / 2;
        int textY = y + (height - 8) / 2;
        ctx.drawTextWithShadow(tr, label, textX, textY, textColor);
    }
}
