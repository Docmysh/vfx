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
    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onRenderShadow(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!ShadowSummonManager.isShadowEntity(entity)) {
            return;
        }

        event.setCanceled(true);

        LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer =
                (LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) event.getRenderer();
        EntityModel<LivingEntity> model = renderer.getModel();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        ResourceLocation texture = renderer.getTextureLocation(entity);

        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(0.05F, 0.05F, 0.05F, 0.75F);

        model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityTranslucent(texture)),
                event.getPackedLight(),
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }
}
