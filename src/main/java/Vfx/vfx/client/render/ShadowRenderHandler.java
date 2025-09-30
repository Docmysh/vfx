package Vfx.vfx.client.render;

import Vfx.vfx.Vfx;
import Vfx.vfx.shadow.ShadowSummonManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Vfx.MODID, value = Dist.CLIENT)
public class ShadowRenderHandler {
    @SubscribeEvent
    public static void onRenderShadowPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!ShadowSummonManager.isShadowEntity(event.getEntity())) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 1.0F);
    }

    @SubscribeEvent
    public static void onRenderShadowPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!ShadowSummonManager.isShadowEntity(entity)) {
            return;
        }

        LivingEntityRenderer<?, ?> renderer = event.getRenderer();
        EntityModel<?> model = renderer.getModel();
        @SuppressWarnings("unchecked")
        ResourceLocation texture = ((LivingEntityRenderer<LivingEntity, ?>) renderer).getTextureLocation(entity);
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();

        poseStack.pushPose();
        model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityTranslucent(texture)),
                event.getPackedLight(),
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                0.0F,
                0.0F,
                0.0F,
                0.75F
        );
        poseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
