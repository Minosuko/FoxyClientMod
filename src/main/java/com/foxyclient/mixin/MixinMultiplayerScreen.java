package com.foxyclient.mixin;

import com.foxyclient.gui.ProxyScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the Proxy Manager button to the Multiplayer screen.
 */
@Mixin(MultiplayerScreen.class)
public class MixinMultiplayerScreen {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MultiplayerScreen screen = (MultiplayerScreen) (Object) this;

        // Position the Proxy Manager button in the top right corner
        ((com.foxyclient.mixin.ScreenAccessor) screen).invokeAddDrawableChild(ButtonWidget.builder(Text.literal("Proxy Manager"), button -> {
            ((com.foxyclient.mixin.ScreenAccessor) screen).getClient().setScreen(new ProxyScreen(screen));
        }).dimensions(screen.width - 105, 5, 100, 20).build());
    }
}
