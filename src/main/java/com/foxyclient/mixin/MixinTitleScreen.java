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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the Minecraft title screen logo with FoxyClient icon + branding.
 * Background rendering is now handled here at HEAD of render for TitleScreen specifically.
 */
@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Unique
    private static final Identifier FOXY_BACKGROUND = Identifier.of("foxyclient", "background.png");

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
     * Inject at the beginning of render to draw the custom background.
     * This is the bottom layer.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        
        String type = com.foxyclient.util.FoxyConfig.INSTANCE.customBackgroundType.get();
        if ("Default".equals(type)) {
            return; // Let vanilla render its panorama
        }

        if (com.foxyclient.util.VideoHelper.hasCustomBackground()) {
            com.foxyclient.util.VideoHelper.updateTexture();
            context.drawTexture(RenderPipelines.GUI_TEXTURED, com.foxyclient.util.VideoHelper.getBackgroundId(), 0, 0, 0.0F, 0.0F, screen.width, screen.height, screen.width, screen.height);
        } else {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, FOXY_BACKGROUND, 0, 0, 0.0F, 0.0F, screen.width, screen.height, screen.width, screen.height);
        }
    }

    /**
     * Render the FoxyClient logo and branding BEHIND the buttons (before super.render).
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void renderFoxyBranding(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;
        int screenWidth = screen.width;
        int centerX = screenWidth / 2;
        
        float alpha = 1.0f;
        int colorAlpha = net.minecraft.util.math.ColorHelper.getWhite(alpha);

        // Larger Icon size (64x64)
        int iconSize = 64;
        int iconX = centerX - iconSize / 2;
        int iconY = 15;

        // Draw the FoxyClient icon.png texture
        context.drawTexture(RenderPipelines.GUI_TEXTURED, FOXYCLIENT_LOGO, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize, colorAlpha);

        // Title text below icon
        String title = "FoxyClient";
        int titleY = iconY + iconSize + 10;
        int a = ((int)(alpha * 255)) << 24;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, titleY);
        context.getMatrices().scale(2.0f, 2.0f);
        int titleW = textRenderer.getWidth(title);
        context.drawTextWithShadow(textRenderer, "\u00A7b\u00A7l" + title, -titleW / 2, 0, a | 0x00E5CC);
        context.getMatrices().popMatrix();

        // Version below title
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
     * On TitleScreen init: add FoxyClient buttons.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;

        // Initialize background if not already processing
        if (!com.foxyclient.util.VideoHelper.hasCustomBackground()) {
            com.foxyclient.util.VideoHelper.initBackground();
        }

        // Add buttons
        ((com.foxyclient.mixin.ScreenAccessor) screen).invokeAddDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.literal("Alt Manager"), button -> {
            MinecraftClient.getInstance().setScreen(new com.foxyclient.gui.AltManagerScreen(screen));
        }).dimensions(5, 5, 80, 20).build());

        ((com.foxyclient.mixin.ScreenAccessor) screen).invokeAddDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.literal("Foxy Config"), button -> {
            MinecraftClient.getInstance().setScreen(new com.foxyclient.gui.FoxyConfigScreen(screen));
        }).dimensions(5, 30, 80, 20).build());
    }
}
