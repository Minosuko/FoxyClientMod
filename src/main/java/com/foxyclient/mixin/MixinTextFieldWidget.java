package com.foxyclient.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class MixinTextFieldWidget extends ClickableWidget {

    @Shadow private boolean drawsBackground;
    @Unique private float hoverFade = 0.0f;
    @Unique private static final Identifier OUTFIT_FONT = Identifier.of("foxyclient", "outfit");

    public MixinTextFieldWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Redirect(
        method = "format",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/text/OrderedText;styledForwardsVisitedString(Ljava/lang/String;Lnet/minecraft/text/Style;)Lnet/minecraft/text/OrderedText;")
    )
    private OrderedText applyOutfitFont(String string, Style style) {
        return OrderedText.styledForwardsVisitedString(string, style.withFont(new net.minecraft.text.StyleSpriteSource.Font(OUTFIT_FONT)));
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void onRenderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!this.visible) return;

        // Update hover animation
        float target = (this.hovered || this.isFocused()) ? 1.0f : 0.0f;
        float speed = 0.15f * deltaTicks;
        if (Math.abs(target - hoverFade) < speed) hoverFade = target;
        else if (hoverFade < target) hoverFade += speed;
        else hoverFade -= speed;
        hoverFade = MathHelper.clamp(hoverFade, 0.0f, 1.0f);

        if (this.drawsBackground) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            // Background: Deep translucent black
            int bgColor = 0x60000000;
            // Border: Smooth transition to teal
            int borderColor = interpolateColor(0x30AAAAAA, 0xFF00FCFC, hoverFade);

            if (!this.active) {
                bgColor = 0x30000000;
                borderColor = 0x20555555;
            }

            // Draw background
            context.fill(x, y, x + w, y + h, bgColor);

            // Draw border
            drawStrokedRect(context, x, y, w, h, borderColor);

            // If focused/hovered, add a subtle inner glow
            if (hoverFade > 0.1f) {
                int glowColor = applyAlpha(0x1500FCFC, hoverFade);
                context.fill(x + 1, y + 1, x + w - 1, y + h - 1, glowColor);
            }
        }
    }

    /**
     * Redirects the vanilla background rendering to "nothing" so our custom background shows instead.
     */
    @Redirect(
        method = "renderWidget",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V")
    )
    private void skipVanillaBackground(DrawContext instance, RenderPipeline pipeline, Identifier identifier, int x, int y, int width, int height) {
        // Do nothing! This prevents the vanilla button/textbox texture from drawing.
    }

    @Unique
    private void drawStrokedRect(DrawContext context, int x, int y, int w, int h, int color) {
        context.drawHorizontalLine(x, x + w - 1, y, color);
        context.drawHorizontalLine(x, x + w - 1, y + h - 1, color);
        context.drawVerticalLine(x, y, y + h - 1, color);
        context.drawVerticalLine(x + w - 1, y, y + h - 1, color);
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
