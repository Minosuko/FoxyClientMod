package com.foxyclient.module.render;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced multi-segmented cape model for realistic physics.
 * Replaces the vanilla one-piece cape.
 */
public class PhysicsCapeModel extends BipedEntityModel<PlayerEntityRenderState> {
    private final List<ModelPart> segments = new ArrayList<>();

    public PhysicsCapeModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        ModelPart current = body.getChild("segment_0");
        segments.add(current);
        for (int i = 1; i < 16; i++) {
            current = current.getChild("segment_" + i);
            segments.add(current);
        }
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        
        // BipedEntityModel expects these parts to exist in the root
        root.addChild("head", ModelPartBuilder.create(), ModelTransform.NONE);
        root.addChild("hat", ModelPartBuilder.create(), ModelTransform.NONE);
        root.addChild("right_arm", ModelPartBuilder.create(), ModelTransform.NONE);
        root.addChild("left_arm", ModelPartBuilder.create(), ModelTransform.NONE);
        root.addChild("right_leg", ModelPartBuilder.create(), ModelTransform.NONE);
        root.addChild("left_leg", ModelPartBuilder.create(), ModelTransform.NONE);

        // We only need the body part to hold our cape segments.
        ModelPartData body = root.addChild("body", ModelPartBuilder.create(), ModelTransform.NONE);
        
        ModelPartData current = body;
        for (int i = 0; i < 16; i++) {
            // Each segment: 10 wide, 1 high, 1 deep
            current = current.addChild("segment_" + i,
                ModelPartBuilder.create().uv(0, i).cuboid(-5.0F, 0.0F, -1.0F, 10.0F, 1.0F, 1.0F),
                ModelTransform.of(0.0F, i == 0 ? 0.0F : 1.0F, i == 0 ? 2.0F : 0.0F, 0.0f, 0.0f, 0.0f));
        }
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(PlayerEntityRenderState state) {
        super.setAngles(state);
        
        com.foxyclient.module.render.CapePhysics module = com.foxyclient.FoxyClient.INSTANCE.getModuleManager().getModule(com.foxyclient.module.render.CapePhysics.class);
        boolean enabled = module != null && module.isEnabled();

        float vertical = state.field_53536;
        float horizontal = state.field_53537;
        float side = state.field_53538;

        for (int i = 0; i < segments.size(); i++) {
            ModelPart segment = segments.get(i);
            
            if (!enabled) {
                // Vanilla-like behavior for a single part (only top segment rotates)
                if (i == 0) {
                    segment.pitch = (6.0F + (horizontal / 2.0F) + vertical) * 0.017453292F;
                    segment.roll = (side / 2.0F) * 0.017453292F;
                    segment.yaw = 3.1415927F;
                } else {
                    segment.pitch = 0;
                    segment.roll = 0;
                    segment.yaw = 0;
                }
                continue;
            }

            // Real Physics: Progressive bending
            float angleX = (6.0F + (horizontal / 2.0F) + vertical) * 0.017453292F;
            float angleZ = (side / 2.0F) * 0.017453292F;
            
            // Bent curve: segments further down rotate a fraction of the total angle
            segment.pitch = angleX * 0.12f; // Each segment adds a bit of rotation
            if (i == 0) segment.pitch += angleX * 0.1f;

            segment.roll = angleZ * 0.08f;
            
            if (i == 0) {
                segment.yaw = 3.1415927F;
            } else {
                segment.yaw = 0;
            }
        }
    }
}
