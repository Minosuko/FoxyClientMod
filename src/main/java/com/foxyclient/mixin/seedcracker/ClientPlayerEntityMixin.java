package com.foxyclient.mixin.seedcracker;

import com.foxyclient.seedcracker.command.ClientCommands;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    // SeedCrackerX uses this for command registration if Fabric API isn't used,
    // but we use Fabric Client Command API in ClientCommands.java, which is registered 
    // in the ModInitializer or ClientModInitializer.
    // However, SeedCrackerX also does some custom processing here.
}
