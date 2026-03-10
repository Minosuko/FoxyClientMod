package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

/**
 * ColorSigns - Translates '&' color codes to '§' formatting codes on signs.
 * When you type '&4Hello' on a sign, it becomes red '§4Hello'.
 * Also supports formatting: &l=bold, &o=italic, &n=underline, &m=strikethrough, &k=obfuscated, &r=reset.
 */
public class ColorSigns extends Module {
    private final BoolSetting allPrefixes = addSetting(new BoolSetting("AllPrefixes", "Also allow $ prefix", true));

    public ColorSigns() {
        super("ColorSigns", "Color codes on signs (&4 = red)", Category.PLAYER);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof UpdateSignC2SPacket pkt) {
            event.cancel();

            // Translate color codes in all 4 sign lines
            String[] lines = pkt.getText();
            String[] translated = new String[lines.length];

            for (int i = 0; i < lines.length; i++) {
                translated[i] = translateColorCodes(lines[i]);
            }

            // Send the modified packet
            mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(
                pkt.getPos(), pkt.isFront(),
                translated[0], translated[1], translated[2], translated[3]
            ));
        }
    }

    private String translateColorCodes(String text) {
        if (text == null || text.isEmpty()) return text;

        // Replace &X with §X for all valid color/format codes
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (i + 1 < chars.length && isColorPrefix(chars[i])) {
                char code = chars[i + 1];
                if (isValidCode(code)) {
                    result.append('§').append(code);
                    i++; // Skip next char
                    continue;
                }
            }
            result.append(chars[i]);
        }

        return result.toString();
    }

    private boolean isColorPrefix(char c) {
        if (c == '&') return true;
        if (allPrefixes.get() && c == '$') return true;
        return false;
    }

    private boolean isValidCode(char c) {
        // 0-9, a-f (colors), k-o (formatting), r (reset)
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'A' && c <= 'F') ||
               c == 'k' || c == 'K' ||  // Obfuscated
               c == 'l' || c == 'L' ||  // Bold
               c == 'm' || c == 'M' ||  // Strikethrough
               c == 'n' || c == 'N' ||  // Underline
               c == 'o' || c == 'O' ||  // Italic
               c == 'r' || c == 'R';    // Reset
    }
}
