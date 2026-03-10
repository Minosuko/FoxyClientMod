package com.foxyclient.util;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import java.util.Arrays;

/**
 * Tracks server TPS by listening to time update packets.
 */
public class TickRateManager {
    public static final TickRateManager INSTANCE = new TickRateManager();
    
    private final float[] tpsArr = new float[20];
    private int tpsPtr = 0;
    private long lastPacketTime = -1;

    public TickRateManager() {
        FoxyClient.INSTANCE.getEventBus().register(this);
        Arrays.fill(tpsArr, 20);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            long now = System.currentTimeMillis();
            if (lastPacketTime != -1) {
                long timeElapsed = now - lastPacketTime;
                float tps = 20000f / timeElapsed;
                if (tps > 20f) tps = 20f;
                
                tpsArr[tpsPtr] = tps;
                tpsPtr = (tpsPtr + 1) % tpsArr.length;
            }
            lastPacketTime = now;
        }
    }

    public float getTPS() {
        float sum = 0;
        for (float f : tpsArr) sum += f;
        return sum / tpsArr.length;
    }
}
