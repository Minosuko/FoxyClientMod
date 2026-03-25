package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.render.CapePhysics;
import com.foxyclient.module.render.PhysicsCapeModel;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PlayerCapeModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public abstract class MixinCapeFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    @Shadow @Final @Mutable
    private BipedEntityModel<PlayerEntityRenderState> model;

    public MixinCapeFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, LoadedEntityModels modelLoader, EquipmentModelLoader equipmentModelLoader, CallbackInfo ci) {
        // We replace the vanilla model with our custom physics model at init time.
        // Even if the module is disabled, the model can stay replaced as long as 
        // its setAngles handles the fallback case (which PhysicsCapeModel does by using vanilla orientations).
        this.model = new PhysicsCapeModel(PhysicsCapeModel.getTexturedModelData().createModel());
    }
}
