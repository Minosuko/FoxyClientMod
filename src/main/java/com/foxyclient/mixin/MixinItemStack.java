package com.foxyclient.mixin;

import com.foxyclient.module.ui.ShulkerView;
import com.foxyclient.util.ShulkerTooltipComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Inject(method = "getTooltipData", at = @At("HEAD"), cancellable = true)
    private void onGetTooltipData(CallbackInfoReturnable<Optional<TooltipData>> cir) {
        if (ShulkerView.INSTANCE == null || !ShulkerView.INSTANCE.isEnabled()) return;

        ItemStack stack = (ItemStack) (Object) this;
        if (isShulkerBox(stack)) {
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container != null) {
                // Check if we should show empty shulkers
                if (!ShulkerView.INSTANCE.showEmpty.get()) {
                    // Check if it's empty
                    if (!container.iterateNonEmpty().iterator().hasNext()) {
                        return;
                    }
                }
                cir.setReturnValue(Optional.of(new ShulkerTooltipComponent.ShulkerTooltipData(container)));
            }
        }
    }

    private boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() == Items.SHULKER_BOX ||
               stack.getItem() == Items.WHITE_SHULKER_BOX ||
               stack.getItem() == Items.ORANGE_SHULKER_BOX ||
               stack.getItem() == Items.MAGENTA_SHULKER_BOX ||
               stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX ||
               stack.getItem() == Items.YELLOW_SHULKER_BOX ||
               stack.getItem() == Items.LIME_SHULKER_BOX ||
               stack.getItem() == Items.PINK_SHULKER_BOX ||
               stack.getItem() == Items.GRAY_SHULKER_BOX ||
               stack.getItem() == Items.LIGHT_GRAY_SHULKER_BOX ||
               stack.getItem() == Items.CYAN_SHULKER_BOX ||
               stack.getItem() == Items.PURPLE_SHULKER_BOX ||
               stack.getItem() == Items.BLUE_SHULKER_BOX ||
               stack.getItem() == Items.BROWN_SHULKER_BOX ||
               stack.getItem() == Items.GREEN_SHULKER_BOX ||
               stack.getItem() == Items.RED_SHULKER_BOX ||
               stack.getItem() == Items.BLACK_SHULKER_BOX;
    }
}
