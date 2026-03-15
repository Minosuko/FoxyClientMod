package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the Minecraft title screen logo with FoxyClient icon + branding.
 */
@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Unique
    private static final Identifier FOXYCLIENT_LOGO = Identifier.of("foxyclient", "icon.png");

    /**
     * Suppress the vanilla logo rendering.
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"))
    private void suppressLogo(LogoDrawer logoDrawer, DrawContext context, int screenWidth, float alpha) {
        // Don't render the vanilla logo
    }

    /**
     * Render the FoxyClient logo and branding BEHIND the buttons (before super.render).
     */
    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void renderFoxyBranding(DrawContext context, int mouseX, int mouseY, float deltaTicks, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;
        int screenWidth = screen.width;
        int centerX = screenWidth / 2;
        
        // Calculate alpha the same way TitleScreen does
        float alpha = 1.0f;
        // backgroundFadeStart and doBackgroundFade are private, but we can just use 1.0f for simplicity 
        // or approximate it. Let's just use 1.0f for the custom logo since opacity fades usually finish fast
        int colorAlpha = net.minecraft.util.math.ColorHelper.getWhite(alpha);

        // Larger Icon size (64x64)
        int iconSize = 64;
        int iconX = centerX - iconSize / 2;
        int iconY = 15;

        // Draw the FoxyClient icon.png texture
        context.drawTexture(RenderPipelines.GUI_TEXTURED, FOXYCLIENT_LOGO, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize, colorAlpha);

        // Larger Title text below icon — "FoxyClient" in bold teal using scale trick
        String title = "FoxyClient";
        int titleY = iconY + iconSize + 10;
        int a = ((int)(alpha * 255)) << 24;

        context.getMatrices().pushMatrix();
        // Translate to slightly below icon, then scale 2x
        context.getMatrices().translate(centerX, titleY);
        context.getMatrices().scale(2.0f, 2.0f);
        int titleW = textRenderer.getWidth(title);
        // Draw centered at (0,0) relative to translated position
        context.drawTextWithShadow(textRenderer, "\u00A7b\u00A7l" + title, -titleW / 2, 0, a | 0x00E5CC);
        context.getMatrices().popMatrix();

        // Version below title (adjust Y accounting for the 2x scale)
        String version = "v" + FoxyClient.VERSION;
        int versionW = textRenderer.getWidth(version);
        context.drawTextWithShadow(textRenderer, version, centerX - versionW / 2, titleY + 22, a | 0xAAAAAA);
    }

    /**
     * Suppress the vanilla splash text rendering.
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashTextRenderer;render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/client/font/TextRenderer;F)V"))
    private void suppressSplash(SplashTextRenderer splashTextRenderer, DrawContext context, int screenWidth, TextRenderer textRenderer, float alpha) {
        // Don't render the vanilla splash text
    }

    /**
     * Inject into TitleScreen.init() to add the Alt Manager button in the top left.
     */
    @org.spongepowered.asm.mixin.injection.Inject(method = "init", at = @At("TAIL"))
    private void onInit(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        ((com.foxyclient.mixin.ScreenAccessor) screen).invokeAddDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.literal("Alt Manager"), button -> {
            MinecraftClient.getInstance().setScreen(new com.foxyclient.gui.AltManagerScreen(screen));
        }).dimensions(5, 5, 100, 20).build());
    }
}
