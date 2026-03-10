package com.foxyclient.mixin;

import com.foxyclient.util.ShulkerTooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to handle custom TooltipData and return custom TooltipComponents.
 */
@Mixin(TooltipComponent.class)
public interface MixinTooltipComponent {
    @Inject(method = "of(Lnet/minecraft/item/tooltip/TooltipData;)Lnet/minecraft/client/gui/tooltip/TooltipComponent;", at = @At("HEAD"), cancellable = true)
    private static void onOf(TooltipData data, CallbackInfoReturnable<TooltipComponent> cir) {
        if (data instanceof ShulkerTooltipComponent.ShulkerTooltipData shulkerData) {
            cir.setReturnValue(new ShulkerTooltipComponent(shulkerData));
        }
    }
}
