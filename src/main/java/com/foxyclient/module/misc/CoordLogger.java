package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;

/** CoordLogger - Logs world event coordinates (JexClient port). */
public class CoordLogger extends Module {
    private final BoolSetting logExplosions = addSetting(new BoolSetting("Explosions", "Log explosions", true));
    private final BoolSetting logDragonBreath = addSetting(new BoolSetting("DragonBreath", "Log dragon breath", true));
    private final BoolSetting logWitherSpawn = addSetting(new BoolSetting("WitherSpawn", "Log wither spawns", true));

    public CoordLogger() { super("CoordLogger", "Log world event coordinates", Category.MISC); }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (nullCheck()) return;
        if (logExplosions.get() && event.getPacket() instanceof ExplosionS2CPacket pkt) {
            info("§eExplosion at §f" + String.format("%.0f, %.0f, %.0f", pkt.center().x, pkt.center().y, pkt.center().z));
        }
        if (event.getPacket() instanceof WorldEventS2CPacket pkt) {
            int type = pkt.getEventId();
            if (type == 1023 && logWitherSpawn.get()) {
                info("§eWither spawned at §f" + pkt.getPos().toShortString());
            }
            if (type == 1028 && logDragonBreath.get()) {
                info("§eDragon event at §f" + pkt.getPos().toShortString());
            }
        }
    }
}
