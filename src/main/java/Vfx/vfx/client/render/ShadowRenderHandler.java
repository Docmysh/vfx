package Vfx.vfx.client.render;

import Vfx.vfx.Vfx;
import Vfx.vfx.shadow.ShadowSummonManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Vfx.MODID, value = Dist.CLIENT)
public class ShadowRenderHandler {
    private static boolean tintApplied = false;

    @SubscribeEvent
    public static void onRenderShadowPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!ShadowSummonManager.isShadowEntity(event.getEntity())) {
            return;
        }
        tintApplied = true;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.05F, 0.05F, 0.05F, 0.6F);
    }

    @SubscribeEvent
    public static void onRenderShadowPost(RenderLivingEvent.Post<?, ?> event) {
        if (!tintApplied || !ShadowSummonManager.isShadowEntity(event.getEntity())) {
            return;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        tintApplied = false;
    }
}
