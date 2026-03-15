package com.foxyclient.util;

import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.MinecraftClient;

public class ArmorUtil {
    
    public static double getProtectionScore(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        
        double score = 0;
        
        // Base armor and toughness from attribute modifiers
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                if (entry.attribute().equals(EntityAttributes.ARMOR)) {
                    score += entry.modifier().value();
                } else if (entry.attribute().equals(EntityAttributes.ARMOR_TOUGHNESS)) {
                    score += entry.modifier().value() * 0.5; // Toughness is less valuable than pure armor
                }
            }
        }
        
        // Enchantments
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null) {
                // In 1.21, we iterate over the registry entries of enchantments
                for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
                    RegistryEntry<Enchantment> ench = entry.getKey();
                    int level = entry.getIntValue();
                    
                    // Basic weighting for common protection enchantments
                    // We use translation keys or identifiers since we might not have easy access to the exact RegistryKey constant here
                    String key = ench.getKey().map(k -> k.getValue().toString()).orElse("");
                    
                    if (key.contains("protection")) {
                        if (key.endsWith("protection")) score += level * 1.0; // General protection
                        else score += level * 0.5; // Specific protection (fire, blast, projectile)
                    } else if (key.contains("thorns")) {
                        score += level * 0.1;
                    } else if (key.contains("unbreaking")) {
                        score += level * 0.05;
                    } else if (key.contains("mending")) {
                        score += 1.0;
                    }
                }
            }
        }
        
        return score;
    }
    
    public static EquipmentSlot getArmorSlot(ItemStack stack) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) {
            return equippable.slot();
        }
        return null;
    }
    
    public static boolean isArmor(ItemStack stack) {
        return getArmorSlot(stack) != null;
    }
}
