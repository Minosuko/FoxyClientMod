package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;

/**
 * Detailed target information overlay, including armor and items.
 */
public class TargetInfoHUD extends Module {
    public TargetInfoHUD() {
        super("TargetInfo", "Detailed target status", Category.UI);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        PlayerEntity target = findNearestTarget();
        if (target == null) return;

        int x = mc.getWindow().getScaledWidth() / 2 - 50;
        int y = mc.getWindow().getScaledHeight() - 120;

        // Draw background
        event.getContext().fill(x - 4, y - 4, x + 104, y + 40, 0x88000000);

        // Name and Health
        event.getContext().drawTextWithShadow(mc.textRenderer, target.getName().getString(), x, y, 0xFFFFFFFF);
        String hp = String.format("%.1f HP", target.getHealth() + target.getAbsorptionAmount());
        event.getContext().drawTextWithShadow(mc.textRenderer, hp, x, y + 10, 0xFFFF0000);

        // Armor display
        int armorX = x;
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (EquipmentSlot slot : slots) {
            ItemStack armor = target.getEquippedStack(slot);
            if (!armor.isEmpty()) {
                event.getContext().drawItem(armor, armorX, y + 20);
                armorX += 18;
            }
        }
        
        // Main hand item
        if (!target.getMainHandStack().isEmpty()) {
            event.getContext().drawItem(target.getMainHandStack(), x + 80, y + 20);
        }
    }

    private PlayerEntity findNearestTarget() {
        PlayerEntity nearest = null;
        double dist = Double.MAX_VALUE;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            double d = mc.player.distanceTo(player);
            if (d < dist && d < 15) {
                dist = d;
                nearest = player;
            }
        }
        return nearest;
    }
}
