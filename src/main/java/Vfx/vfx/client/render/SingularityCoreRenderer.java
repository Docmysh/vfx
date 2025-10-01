package Vfx.vfx.client.render;

import Vfx.vfx.client.model.SingularityCoreModel;
import Vfx.vfx.entity.SingularityCoreEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class SingularityCoreRenderer extends GeoEntityRenderer<SingularityCoreEntity> {
    public SingularityCoreRenderer(EntityRendererProvider.Context context) {
        super(context, new SingularityCoreModel());
        this.shadowRadius = 1.2F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(SingularityCoreEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, SingularityCoreEntity entity, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                               float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float fade = computeFade(entity.getLifeTicks(), partialTick);
        super.actuallyRender(poseStack, entity, model, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, fade);
    }

    private static float computeFade(int lifeTicks, float partialTick) {
        float totalTicks = lifeTicks + partialTick;
        if (totalTicks < FADE_IN_TICKS) {
            return Math.min(1.0F, totalTicks / FADE_IN_TICKS);
        }

        if (totalTicks > SingularityCoreEntity.MAX_LIFE_TICKS - FADE_OUT_TICKS) {
            float ticksPast = totalTicks - (SingularityCoreEntity.MAX_LIFE_TICKS - FADE_OUT_TICKS);
            return Math.max(0.0F, 1.0F - ticksPast / FADE_OUT_TICKS);
        }

        return 1.0F;
    }

    private static final float FADE_IN_TICKS = 8.0F;
    private static final float FADE_OUT_TICKS = 12.0F;
}
