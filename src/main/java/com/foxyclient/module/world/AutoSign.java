package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

public class AutoSign extends Module {

    private String[] savedText = null;
    private boolean isFront = true;

    public AutoSign() {
        super("AutoSign", "Automatically writes signs using previous text", Category.WORLD);
    }

    @Override
    public void onDisable() {
        savedText = null;
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;
        
        if (event.getPacket() instanceof UpdateSignC2SPacket packet) {
            savedText = packet.getText();
            isFront = packet.isFront();
        }
    }

    public boolean hasSavedText() {
        return savedText != null;
    }

    public void applyToSign(SignBlockEntity blockEntity, boolean front) {
        if (savedText != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(blockEntity.getPos(), front, savedText[0], savedText[1], savedText[2], savedText[3]));
        }
    }
}
