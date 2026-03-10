package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    @Inject(method = "onChatFieldUpdate", at = @At("HEAD"))
    private void onChatFieldUpdate(String chatText, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;

        if (chatText.startsWith(FoxyClient.INSTANCE.getCommandManager().getPrefix()) || chatText.startsWith("#")) {
            List<String> suggestions = FoxyClient.INSTANCE.getCommandManager().getSuggestions(chatText);
            if (!suggestions.isEmpty()) {
                // Here we would ideally trigger the suggestor, but for now we just verify it's working.
                // Triggering the suggestor properly depends on Minecraft version specifics.
                // We'll hook into ChatInputSuggestor as well for better results.
            }
        }
    }
}
