package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinChatInputSuggestor {

    @Shadow @Final private net.minecraft.client.gui.widget.TextFieldWidget textField;

    @Inject(method = "refresh", at = @At("TAIL"))
    private void onRefresh(CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;

        String text = textField.getText();
        String prefix = FoxyClient.INSTANCE.getCommandManager().getPrefix();
        if (text.startsWith(prefix) || text.startsWith("#")) {
            List<String> suggestions = FoxyClient.INSTANCE.getCommandManager().getSuggestions(text);
            if (!suggestions.isEmpty()) {
                int start = text.lastIndexOf(' ') + 1;
                if (start == 0) start = 0; // The whole word including prefix

                SuggestionsBuilder builder = new SuggestionsBuilder(text, start);
                for (String s : suggestions) {
                    builder.suggest(s);
                }
                
                this.pendingSuggestions = builder.buildFuture();
                this.show(true);
            }
        }
    }

    @Shadow public abstract void show(boolean setSelection);
    @Shadow private CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> pendingSuggestions;
}
