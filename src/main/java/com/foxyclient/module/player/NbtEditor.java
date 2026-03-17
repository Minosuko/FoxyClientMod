package com.foxyclient.module.player;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * NbtEditor - Edit item NBT data.
 * Opens a GUI to modify item properties like display name, lore, and enchantments in Creative Mode.
 */
public class NbtEditor extends Module {
    public NbtEditor() {
        super("NbtEditor", "Edit item NBT data", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            toggle();
            return;
        }

        if (!mc.player.getAbilities().creativeMode) {
            error("You must be in Creative Mode to use NBT Editor!");
            toggle();
            return;
        }

        mc.setScreen(new NbtEditorScreen());
        toggle(); // Disable module as it's an action to open the GUI
    }

    private static class NbtEditorScreen extends Screen {
        public NbtEditorScreen() {
            super(Text.literal("NBT Editor"));
        }

        @Override
        protected void init() {
            super.init();
            
            int startX = this.width / 2 - 100;
            int startY = this.height / 2 - 30;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Max Enchants"), button -> {
                modifyItem(item -> {
                    // Grab enchantment registry
                    var registry = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                    ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                    
                    // Add max level of every enchantment
                    registry.streamEntries().forEach(entry -> {
                        Enchantment ench = entry.value();
                        builder.set(entry, 32767); // 32767 is max short level
                    });

                    item.set(DataComponentTypes.ENCHANTMENTS, builder.build());
                });
            }).dimensions(startX, startY, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Name: FoxyClient Item"), button -> {
                modifyItem(item -> {
                    item.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§cFoxyClient §fItem"));
                });
            }).dimensions(startX, startY + 25, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Make Unbreakable"), button -> {
                modifyItem(item -> {
                    item.set(DataComponentTypes.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
                });
            }).dimensions(startX, startY + 50, 200, 20).build());
        }

        private void modifyItem(java.util.function.Consumer<ItemStack> modifier) {
            if (client == null || client.player == null) return;
            ItemStack item = client.player.getMainHandStack();
            if (item.isEmpty()) return;

            // Make a copy to edit safely
            ItemStack copy = item.copy();
            modifier.accept(copy);
            
            // Send unvalidated modified item back to our chosen slot using Creative action packet
            client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + client.player.getInventory().selectedSlot, copy));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
            
            ItemStack item = client.player.getMainHandStack();
            if (item.isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer, "Hold an item to edit it!", this.width / 2, 40, 0xFF5555);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, "Editing: " + item.getName().getString(), this.width / 2, 40, 0x55FF55);
            }
        }
    }
}
