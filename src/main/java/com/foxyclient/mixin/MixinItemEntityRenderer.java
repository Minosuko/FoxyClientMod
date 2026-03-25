package com.foxyclient.mixin;

import com.foxyclient.module.render.ItemPhysics;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class MixinItemEntityRenderer {

    @Unique
    private final Random random = Random.create();

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void onRender(ItemEntityRenderState itemEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        ItemPhysics module = ItemPhysics.get();
        if (module != null && module.isEnabled()) {
            if (!itemEntityRenderState.itemRenderState.isEmpty()) {
                matrixStack.push();
                Box box = itemEntityRenderState.itemRenderState.getModelBoundingBox();

                matrixStack.translate(0.0F, 0.05F, 0.0F);

                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                float randOffset = (itemEntityRenderState.uniqueOffset * 360.0f);
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(randOffset));
                
                float scale = module.scale.get().floatValue();
                matrixStack.scale(scale, scale, scale);

                ItemEntityRenderer.render(matrixStack, orderedRenderCommandQueue, itemEntityRenderState.light, itemEntityRenderState, this.random, box);

                matrixStack.pop();
            }
            ci.cancel();
        }
    }
}
