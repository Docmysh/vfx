package Vfx.vfx.client.render;

import Vfx.vfx.client.model.HandGrabModel;
import Vfx.vfx.entity.HandGrabEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class HandGrabRenderer extends GeoEntityRenderer<HandGrabEntity> {
    public HandGrabRenderer(EntityRendererProvider.Context context) {
        super(context, new HandGrabModel());
        this.shadowRadius = 0.25F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(HandGrabEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected void applyRotations(HandGrabEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
        float scale = 1.0F + 0.03F * Mth.sin((entity.tickCount + partialTick) * 0.3F);
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, HandGrabEntity entity, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float lifeFade = entity.getLifeTicks() < 6 ? entity.getLifeTicks() / 6.0F : 1.0F;
        float appearFade = entity.tickCount < 6 ? entity.tickCount / 6.0F : 1.0F;
        float visibility = Mth.clamp(Math.min(lifeFade, appearFade), 0.0F, 1.0F);
        super.actuallyRender(poseStack, entity, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, visibility);
    }
}
