package Vfx.vfx.client.render;

import Vfx.vfx.Vfx;
import Vfx.vfx.client.model.ShadowHandModel;
import Vfx.vfx.entity.shadow.ShadowHandEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ShadowHandRenderer extends EntityRenderer<ShadowHandEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Vfx.MODID, "textures/entity/shadow_hand.png");
    private final ShadowHandModel model;

    public ShadowHandRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ShadowHandModel(context.bakeLayer(ShadowHandModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ShadowHandEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, -1.5F, 0.0F);
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(1.75F, 1.75F, 1.75F);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        this.model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))), packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.95F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowHandEntity entity) {
        return TEXTURE;
    }
}
