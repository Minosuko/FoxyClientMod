package com.foxyclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.foxyclient.FoxyClient;
import com.foxyclient.module.render.Freecam;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity extends PlayerEntity {

    // Recursion guard: getSkin() mixin calls getSkin() to get the original value,
    // which would re-enter this mixin infinitely without this guard.
    @Unique
    private static final ThreadLocal<Boolean> foxyClient$inGetSkin = ThreadLocal.withInitial(() -> false);

    public MixinAbstractClientPlayerEntity(World world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "getFovMultiplier", at = @At("HEAD"), cancellable = true)
    private void onGetFovMultiplier(boolean firstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
        if (FoxyClient.INSTANCE != null) {
            Freecam freecam = FoxyClient.INSTANCE.getModuleManager().getModule(Freecam.class);
            if (freecam != null && freecam.isEnabled()) {
                cir.setReturnValue(1.0f);
            }
        }
    }

    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void onGetSkin(CallbackInfoReturnable<SkinTextures> info) {
        // Prevent infinite recursion
        if (foxyClient$inGetSkin.get()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        // Apply only to local player or FakePlayer
        if (player == mc.player || player.getId() == -1337) {
            com.foxyclient.util.FoxyConfig config = com.foxyclient.util.FoxyConfig.INSTANCE;

            // Get original skin with recursion guard
            foxyClient$inGetSkin.set(true);
            SkinTextures current;
            try {
                current = player.getSkin();
            } finally {
                foxyClient$inGetSkin.set(false);
            }

            // Use accessor methods for record fields
            AssetInfo.TextureAsset skin = current.body();
            AssetInfo.TextureAsset cape = current.cape();
            PlayerSkinType model = current.model();

            boolean changed = false;

            // Simple Cosmetics: Steve, Alex, Custom
            String skinName = config.skinName.get();
            if ("Custom".equals(skinName)) {
                Identifier customId = com.foxyclient.util.SkinResourceManager.getCustomSkinId();
                // For custom textures registered in TextureManager, the id IS the texture path
                skin = new AssetInfo.TextureAssetInfo(customId, customId);
                changed = true;
            } else if ("Steve".equals(skinName)) {
                // Use asset ID format (not full path) — matches DefaultSkinHelper pattern
                skin = new AssetInfo.TextureAssetInfo(Identifier.ofVanilla("entity/player/wide/steve"));
                model = PlayerSkinType.WIDE;
                changed = true;
            } else if ("Alex".equals(skinName)) {
                skin = new AssetInfo.TextureAssetInfo(Identifier.ofVanilla("entity/player/slim/alex"));
                model = PlayerSkinType.SLIM;
                changed = true;
            }

            String capeName = config.capeName.get();
            if ("Custom".equals(capeName)) {
                Identifier customCapeId = com.foxyclient.util.SkinResourceManager.getCustomCapeId();
                cape = new AssetInfo.TextureAssetInfo(customCapeId, customCapeId);
                changed = true;
            } else if ("None".equals(capeName)) {
                cape = null;
                changed = true;
            }

            // Force model if config says so, even for Default skin
            if (config.slimModel.get() && model != PlayerSkinType.SLIM) {
                model = PlayerSkinType.SLIM;
                changed = true;
            } else if (!config.slimModel.get() && model != PlayerSkinType.WIDE) {
                model = PlayerSkinType.WIDE;
                changed = true;
            }

            if (changed) {
                info.setReturnValue(new SkinTextures(
                    skin,
                    cape,
                    current.elytra(),
                    model,
                    current.secure()
                ));
            }
        }
    }
}
