package com.foxyclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class MixinPressableWidget extends ClickableWidget {

    @Unique
    private static final Identifier OUTFIT_FONT = Identifier.of("foxyclient", "outfit");

    @Unique
    private float hoverFade = 0.0f;

    @Unique
    private float clickFade = 0.0f;

    public MixinPressableWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Override
    public Text getMessage() {
        return Texts.withStyle(super.getMessage(), Style.EMPTY.withFont(new net.minecraft.text.StyleSpriteSource.Font(OUTFIT_FONT)));
    }

    @Inject(method = "onClick", at = @At("HEAD"))
    private void onButtonClick(Click click, boolean doubled, CallbackInfo ci) {
        if (this.active) {
            clickFade = 1.0f;
        }
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void onRenderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!this.active) {
            hoverFade = 0.0f;
            clickFade = 0.0f;
            return;
        }

        // Hover fade update
        float target = this.hovered ? 1.0f : 0.0f;
        float speed = 0.15f * deltaTicks;
        
        if (Math.abs(target - hoverFade) < speed) {
            hoverFade = target;
        } else if (hoverFade < target) {
            hoverFade += speed;
        } else {
            hoverFade -= speed;
        }
        hoverFade = MathHelper.clamp(hoverFade, 0.0f, 1.0f);

        // Click fade update (Faster decay)
        if (clickFade > 0) {
            clickFade -= 0.1f * deltaTicks;
            if (clickFade < 0) clickFade = 0;
        }
    }

    /**
     * Replaces the vanilla drawButton logic with custom glassmorphic rendering.
     */
    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void onDrawButton(DrawContext context, CallbackInfo ci) {
        ci.cancel();

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        // Color interpolation based on hoverFade
        // Hex colors:
        // Idle BG: 0x80101010, Hover BG: 0xCC006666
        // Idle Border: 0x4000FCFC, Hover Border: 0xFF00FCFC
        
        int bgColor = interpolateColor(0x80101010, 0xCC006666, hoverFade);
        int borderColor = interpolateColor(0x4000FCFC, 0xFF00FCFC, hoverFade);

        if (!this.active) {
            bgColor = 0x40333333;
            borderColor = 0x20888888;
        }

        // Apply global alpha
        bgColor = applyAlpha(bgColor, this.alpha);
        borderColor = applyAlpha(borderColor, this.alpha);

        // Draw main background
        context.fill(x, y, x + w, y + h, bgColor);

        // Draw modern stroked border
        context.drawHorizontalLine(x, x + w - 1, y, borderColor);
        context.drawHorizontalLine(x, x + w - 1, y + h - 1, borderColor);
        context.drawVerticalLine(x, y, y + h - 1, borderColor);
        context.drawVerticalLine(x + w - 1, y, y + h - 1, borderColor);
        
        // If hovered, draw a subtle inner glow
        if (hoverFade > 0.1f) {
            int glowColor = applyAlpha(0x2000FCFC, hoverFade * this.alpha);
            context.fill(x + 1, y + 1, x + w - 1, y + h - 1, glowColor);
        }

        // Click transition flash
        if (clickFade > 0.01f) {
            int flashColor = applyAlpha(0x4000FCFC, clickFade * this.alpha);
            context.fill(x, y, x + w, y + h, flashColor);
        }
    }

    @Unique
    private int interpolateColor(int color1, int color2, float factor) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Unique
    private int applyAlpha(int color, float alphaFactor) {
        int a = (color >> 24) & 0xFF;
        int combinedAlpha = (int) (a * alphaFactor);
        return (color & 0x00FFFFFF) | (combinedAlpha << 24);
    }
}
