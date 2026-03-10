package com.foxyclient.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor to directly get/set the value field on SimpleOption,
 * bypassing the validator (which clamps gamma to [0, 1]).
 */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor<T> {
    @Accessor("value")
    T foxyclient_getValue();

    @Accessor("value")
    void foxyclient_setValue(T value);
}
