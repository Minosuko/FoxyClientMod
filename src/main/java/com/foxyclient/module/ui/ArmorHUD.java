package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import java.awt.Color;

/**
 * Displays armor icons and durability on the HUD.
 */
public class ArmorHUD extends Module {
    private final ModeSetting layout = addSetting(new ModeSetting("Layout", "HUD layout direction", "Horizontal", "Horizontal", "Vertical"));
    private final ModeSetting durability = addSetting(new ModeSetting("Durability", "Durability display mode", "Percentage", "None", "Percentage", "Value"));
    private final BoolSetting extraItems = addSetting(new BoolSetting("Extra Items", "Show mainhand and offhand", true));

    public ArmorHUD() {
        super("ArmorHUD", "Displays your armor status", Category.UI);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        boolean isHorizontal = layout.get().equals("Horizontal");

        // Adjusted starting position
        int startX = isHorizontal ? screenWidth / 2 + 10 : screenWidth / 2 + 98;
        int startY = isHorizontal ? screenHeight - 65 : screenHeight - 85;

        int x = startX;
        int y = startY;

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        
        for (EquipmentSlot slot : slots) {
            ItemStack item = mc.player.getEquippedStack(slot);
            renderItem(event, item, x, y);
            
            if (isHorizontal) {
                x += 18;
            } else {
                y += 18;
            }
        }

        if (extraItems.get()) {
            if (isHorizontal) {
                x += 4;
                renderItem(event, mc.player.getMainHandStack(), x, y);
                x += 18;
                renderItem(event, mc.player.getOffHandStack(), x, y);
            } else {
                // Vertical: move to separate column to the right
                x = startX + 18;
                y = startY;
                renderItem(event, mc.player.getMainHandStack(), x, y);
                y += 18;
                renderItem(event, mc.player.getOffHandStack(), x, y);
            }
        }
    }

    private void renderItem(Render2DEvent event, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;

        // Draw item icon
        event.getContext().drawItem(stack, x, y);
        event.getContext().drawStackOverlay(mc.textRenderer, stack, x, y);

        // Draw durability
        if (!durability.get().equals("None") && stack.isDamageable()) {
            String text = "";
            double damagePer = (double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage();
            
            if (durability.get().equals("Percentage")) {
                text = (int)(damagePer * 100) + "%";
            } else {
                text = String.valueOf(stack.getMaxDamage() - stack.getDamage());
            }

            // Color based on damage
            int color = Color.HSBtoRGB((float) (damagePer / 3f), 1f, 1f);
            
            event.getContext().getMatrices().pushMatrix();
            event.getContext().getMatrices().scale(0.5f, 0.5f);
            
            // Adjust x and y for 0.5x scale
            int textWidth = mc.textRenderer.getWidth(text);
            event.getContext().drawTextWithShadow(mc.textRenderer, text, (int)((x + 8) * 2 - textWidth / 2f), (int)((y + 16) * 2), color);
            
            event.getContext().getMatrices().popMatrix();
        }
    }
}
