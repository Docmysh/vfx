package Vfx.vfx.client.render;

import Vfx.vfx.client.model.ShadowBallModel;
import Vfx.vfx.entity.ShadowBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ShadowBallRenderer extends GeoEntityRenderer<ShadowBallEntity> {
    public ShadowBallRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowBallModel());
        this.shadowRadius = 1.5F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(ShadowBallEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, ShadowBallEntity entity, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                               float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float fade = 1.0F;
        int lifeTicks = entity.getLifeTicks();
        if (lifeTicks < FADE_IN_TICKS) {
            fade = Math.min(1.0F, lifeTicks / (float) FADE_IN_TICKS);
        } else if (lifeTicks > ShadowBallEntity.MAX_LIFE_TICKS - FADE_OUT_TICKS) {
            int ticksPast = lifeTicks - (ShadowBallEntity.MAX_LIFE_TICKS - FADE_OUT_TICKS);
            fade = Math.max(0.0F, 1.0F - ticksPast / (float) FADE_OUT_TICKS);
        }
        super.actuallyRender(poseStack, entity, model, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, fade);
    }

    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = 20;
}
