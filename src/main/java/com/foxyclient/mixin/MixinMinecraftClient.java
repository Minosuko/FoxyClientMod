package com.foxyclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow public abstract Session getSession();
    @Shadow public abstract void setScreen(net.minecraft.client.gui.screen.Screen screen);

    @org.spongepowered.asm.mixin.Unique
    private int foxy_setScreenDepth = 0;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onPreSetScreen(net.minecraft.client.gui.screen.Screen screen, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        foxy_setScreenDepth++;
        if (foxy_setScreenDepth > 1) {
            return; // We are in a nested setScreen (e.g., InventoryScreen -> CreativeInventoryScreen during init). Allow vanilla.
        }

        MinecraftClient mc = (MinecraftClient) (Object) this;

        // Music management: if entering a menu while world is null, play music
        if (screen != null && mc.world == null) {
            com.foxyclient.util.FoxyMusicManager.play();
        } else if (screen == null) {
            // Entering game world (or just closing a popup)
            com.foxyclient.util.FoxyMusicManager.stop();
        }

        // If ScreenTransitionManager is completing a transition, let the setScreen proceed
        // without interception so init() is called on the target screen.
        if (com.foxyclient.util.ScreenTransitionManager.INSTANCE != null
                && com.foxyclient.util.ScreenTransitionManager.INSTANCE.isCompletingTransition) {
            return;
        }

        // Skip transition if transitions are disabled or config not loaded
        if (com.foxyclient.util.FoxyConfig.INSTANCE == null || !com.foxyclient.util.FoxyConfig.INSTANCE.transitionsEnabled.get()) return;

        // In-game transitions setting controls ANY transitions occurring while actively in a world
        if (!com.foxyclient.util.FoxyConfig.INSTANCE.inGameTransitions.get()) {
            if (mc.world != null) return;
        }

        // Always skip the very first TitleScreen load (no world, no current screen)
        if (mc.currentScreen == null && mc.world == null) return;

        // If already transitioning, abort the transition so it doesn't later complete and overwrite the screen we are setting right now
        if (com.foxyclient.util.ScreenTransitionManager.INSTANCE != null && com.foxyclient.util.ScreenTransitionManager.INSTANCE.isTransitioning()) {
            com.foxyclient.util.ScreenTransitionManager.INSTANCE.abortTransition();
            return;
        }

        if (mc.currentScreen == null) {
            // Opening from game: allow vanilla setScreen so it initializes, but set the state to start Entry Phase (Phase 2)
            com.foxyclient.util.ScreenTransitionManager.INSTANCE.startEntryTransition();
            return;
        }

        // Mod-to-mod or Mod-to-null transitions: Cancel native and do Exit (Phase 1)
        com.foxyclient.util.ScreenTransitionManager.INSTANCE.startTransition(screen);
        
        // Since we are cancelling, the RETURN/TAIL mixin below won't run, so we must decrement the depth manually
        foxy_setScreenDepth--;
        ci.cancel();
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void onPostSetScreen(net.minecraft.client.gui.screen.Screen screen, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        foxy_setScreenDepth--;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (com.foxyclient.FoxyClient.INSTANCE != null) {
            com.foxyclient.FoxyClient.INSTANCE.onTick();
        }
    }
}
