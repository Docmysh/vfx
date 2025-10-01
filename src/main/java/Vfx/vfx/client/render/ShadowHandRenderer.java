package Vfx.vfx.client.render;

import Vfx.vfx.entity.shadow.ShadowHandEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public class ShadowHandRenderer extends EntityRenderer<ShadowHandEntity> {
    public ShadowHandRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ShadowHandEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // The shadow hand is represented by swirling particles handled on the entity itself,
        // so no model is rendered here. Intentionally left blank.
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowHandEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

}
