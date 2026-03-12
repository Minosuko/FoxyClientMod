package com.foxyclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.world.World;
import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity extends PlayerEntity {

    public MixinAbstractClientPlayerEntity(World world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void onGetSkin(CallbackInfoReturnable<SkinTextures> info) {
        int id = this.getId();
        // Check for FakePlayer (-1337) ID
        if (id == -1337) {
            if (MinecraftClient.getInstance().player != null) {
                info.setReturnValue(MinecraftClient.getInstance().player.getSkin());
            }
        }
    }
}
