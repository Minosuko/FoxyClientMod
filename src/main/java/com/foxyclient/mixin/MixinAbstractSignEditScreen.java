package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.world.AutoSign;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen {
    @Shadow private SignBlockEntity blockEntity;
    @Shadow private boolean front;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        if (FoxyClient.INSTANCE != null) {
            AutoSign autoSign = FoxyClient.INSTANCE.getModuleManager().getModule(AutoSign.class);
            if (autoSign != null && autoSign.isEnabled() && autoSign.hasSavedText()) {
                autoSign.applyToSign(blockEntity, front);
                net.minecraft.client.MinecraftClient.getInstance().setScreen(null);
            }
        }
    }
}
