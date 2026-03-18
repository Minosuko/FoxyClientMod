package com.foxyclient.mixin;

import com.foxyclient.util.ScreenTransitionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
    @Unique
    private static final Identifier FOXY_BACKGROUND = Identifier.of("foxyclient", "background.png");

    @Inject(method = "render", at = @At("HEAD"))
    private void onPreRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (ScreenTransitionManager.INSTANCE != null) {
            ScreenTransitionManager.INSTANCE.update(deltaTicks);
            ScreenTransitionManager.INSTANCE.applyTransition(context);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onPostRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (ScreenTransitionManager.INSTANCE != null) {
            ScreenTransitionManager.INSTANCE.endTransition(context);
        }
    }

    /**
     * Globally replaces the background for all menus except TitleScreen (handled separately)
     * and when in-game.
     */
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            Screen screen = (Screen) (Object) this;
            // Don't draw here for TitleScreen to avoid double rendering or conflicts
            if (!(screen instanceof TitleScreen)) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, FOXY_BACKGROUND, 0, 0, 0.0F, 0.0F, screen.width, screen.height, screen.width, screen.height);
                ci.cancel(); // Skip vanilla blur/dirt
            }
        }
    }
}
