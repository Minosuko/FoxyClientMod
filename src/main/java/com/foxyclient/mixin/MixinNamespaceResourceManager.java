package com.foxyclient.mixin;

import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Dynamically intercepts resource loading to serve the custom TTF font entirely in memory,
 * fulfilling the user's request for custom fonts without physical ResourcePacks.
 */
@Mixin(NamespaceResourceManager.class)
public class MixinNamespaceResourceManager {
    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void onGetResource(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (id.getNamespace().equals("foxyclient")) {
            String path = id.getPath();
            if (path.equals("font/outfit.json") || path.equals("font/custom_font.ttf")) {
                String type = com.foxyclient.util.FoxyConfig.INSTANCE.customFontType.get();
                
                // If "Default" (Minecraft), we don't return anything special for our outfit font, 
                // but since widgets specifically ask for it, we should probably fall back to vanilla.
                // However, returning empty might make them use nothing. 
                // Let's explicitly serve a redirect to vanilla if Default is selected?
                // Actually, the widgets use Identifier.of("foxyclient", "outfit").
                
                if (path.equals("font/outfit.json")) {
                    String json;
                    if ("Default".equals(type)) {
                        json = "{\n  \"providers\": [\n    {\n      \"type\": \"reference\",\n      \"id\": \"minecraft:default\"\n    }\n  ]\n}";
                    } else {
                        // Bridge to custom_font.ttf
                        json = "{\n  \"providers\": [\n    {\n      \"type\": \"ttf\",\n      \"file\": \"foxyclient:custom_font.ttf\",\n      \"shift\": [0, 0],\n      \"size\": 11.0,\n      \"oversample\": 4.0\n    }\n  ]\n}";
                    }
                    cir.setReturnValue(Optional.of(new Resource(null, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))));
                } else if (path.equals("font/custom_font.ttf")) {
                    try {
                        if ("FoxyClient".equals(type)) {
                            // Serve built-in JetBrainsMono
                            cir.setReturnValue(Optional.of(new Resource(null, () -> 
                                net.minecraft.client.MinecraftClient.getInstance().getResourceManager()
                                .getResource(Identifier.of("foxyclient", "font/JetBrainsMono-Regular.ttf")).get().getInputStream()
                            )));
                        } else if ("Custom".equals(type)) {
                            // Serve user's custom TTF
                            String fontPathStr = com.foxyclient.util.FoxyConfig.INSTANCE.customFontPath.get();
                            if (fontPathStr != null && !fontPathStr.isEmpty()) {
                                java.io.File fontFile = new java.io.File(fontPathStr);
                                if (!fontFile.isAbsolute()) {
                                    fontFile = new java.io.File(net.minecraft.client.MinecraftClient.getInstance().runDirectory, "config/foxyclient/" + fontPathStr);
                                }
                                if (fontFile.exists()) {
                                    final String fontPath = fontFile.getAbsolutePath();
                                    cir.setReturnValue(Optional.of(new Resource(null, () -> new FileInputStream(fontPath))));
                                }
                            }
                        }
                    } catch (Exception e) {}
                }
            }
        }
    }
}
