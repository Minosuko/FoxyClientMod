package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.ChunkDataEvent;
import com.foxyclient.module.ModuleManager;
import com.foxyclient.module.seedcracker.SeedCrackerLogic;
import com.foxyclient.module.seedcracker.SeedCrackerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        SeedCrackerModule sc = FoxyClient.INSTANCE.getModuleManager().getModule(SeedCrackerModule.class);
        if (sc != null && sc.isEnabled()) {
            sc.setHashedSeed(packet.commonPlayerSpawnInfo().seed());
            SeedCrackerLogic.onJoinWorld(null);
        }
    }

    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        
        WorldChunk chunk = MinecraftClient.getInstance().world.getChunk(packet.getChunkX(), packet.getChunkZ());
        if (chunk != null) {
            FoxyClient.INSTANCE.getEventBus().post(new ChunkDataEvent(chunk));
        }

        SeedCrackerModule sc = FoxyClient.INSTANCE.getModuleManager().getModule(SeedCrackerModule.class);
        if (sc != null && sc.isEnabled()) {
            SeedCrackerLogic.onChunkData(null, new ChunkPos(packet.getChunkX(), packet.getChunkZ()));
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        if (FoxyClient.INSTANCE.getCommandManager().handleChat(message)) {
            ci.cancel();
        }
    }
}
