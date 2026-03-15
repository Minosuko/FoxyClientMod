package com.foxyclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.util.ApiServices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("apiServices")
    ApiServices getApiServices();

    @Accessor("session")
    void setSession(Session session);

    @Accessor("session")
    Session getSession();
}
