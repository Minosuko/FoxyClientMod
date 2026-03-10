package com.foxyclient.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.item.tooltip.TooltipData;

/**
 * Custom tooltip component to render shulker box contents.
 */
public class ShulkerTooltipComponent implements TooltipComponent {
    private final DefaultedList<ItemStack> inventory;

    public ShulkerTooltipComponent(ShulkerTooltipData data) {
        this.inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
        data.container().copyTo(this.inventory);
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 9 * 18;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 3 * 18 + 2;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext drawContext) {
        for (int i = 0; i < inventory.size(); i++) {
            int slotX = x + (i % 9) * 18;
            int slotY = y + (i / 9) * 18;
            ItemStack stack = inventory.get(i);
            
            // Draw slot background highlight
            drawContext.fill(slotX, slotY, slotX + 18, slotY + 18, 0x44000000);
            
            // Draw border
            drawContext.fill(slotX, slotY, slotX + 18, slotY + 1, 0x88FFFFFF);
            drawContext.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0x88FFFFFF);
            drawContext.fill(slotX, slotY, slotX + 1, slotY + 18, 0x88FFFFFF);
            drawContext.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0x88FFFFFF);
            
            if (!stack.isEmpty()) {
                drawContext.drawItem(stack, slotX + 1, slotY + 1);
                drawContext.drawStackOverlay(textRenderer, stack, slotX + 1, slotY + 1);
            }
        }
    }

    /**
     * Data holder for the shulker tooltip.
     */
    public record ShulkerTooltipData(ContainerComponent container) implements TooltipData {}
}
