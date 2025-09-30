package Vfx.vfx.client.render;

import Vfx.vfx.Vfx;
import Vfx.vfx.shadow.ShadowSummonManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Vfx.MODID, value = Dist.CLIENT)
public class ShadowRenderHandler {
    private static final float RED_TINT = 0.05F;
    private static final float GREEN_TINT = 0.05F;
    private static final float BLUE_TINT = 0.05F;
    private static final float ALPHA_TINT = 0.75F;

    private static boolean renderingShadow;

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onRenderShadow(RenderLivingEvent.Pre<?, ?> event) {
        if (renderingShadow) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!ShadowSummonManager.isShadowEntity(entity)) {
            return;
        }

        event.setCanceled(true);

        LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer =
                (LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) event.getRenderer();

        renderingShadow = true;
        try {
            MultiBufferSource tintedSource = new TintedMultiBufferSource(
                    event.getMultiBufferSource(),
                    RED_TINT,
                    GREEN_TINT,
                    BLUE_TINT,
                    ALPHA_TINT
            );
            renderer.render(
                    entity,
                    event.getEntityYaw(),
                    event.getPartialTick(),
                    event.getPoseStack(),
                    tintedSource,
                    event.getPackedLight()
            );
        } finally {
            renderingShadow = false;
        }
    }

    private static class TintedMultiBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        private TintedMultiBufferSource(MultiBufferSource delegate, float red, float green, float blue, float alpha) {
            this.delegate = delegate;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new TintedVertexConsumer(delegate.getBuffer(renderType), red, green, blue, alpha);
        }
    }

    private static class TintedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        private TintedVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha) {
            this.delegate = delegate;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(
                    multiply(red, this.red),
                    multiply(green, this.green),
                    multiply(blue, this.blue),
                    multiply(alpha, this.alpha)
            );
            return this;
        }

        @Override
        public VertexConsumer color(float red, float green, float blue, float alpha) {
            delegate.color(red * this.red, green * this.green, blue * this.blue, alpha * this.alpha);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(
                    multiply(red, this.red),
                    multiply(green, this.green),
                    multiply(blue, this.blue),
                    multiply(alpha, this.alpha)
            );
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }

        private static int multiply(int value, float factor) {
            return Mth.clamp(Math.round(value * factor), 0, 255);
        }
    }
}
