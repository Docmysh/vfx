package Vfx.vfx.client.render;

import Vfx.vfx.client.model.SphereOfDoomModel;
import Vfx.vfx.entity.SphereOfDoomEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SphereOfDoomRenderer extends GeoEntityRenderer<SphereOfDoomEntity> {
    public SphereOfDoomRenderer(EntityRendererProvider.Context context) {
        super(context, new SphereOfDoomModel());
        this.shadowRadius = 1.2F;
    }

    @Override
    public RenderType getRenderType(SphereOfDoomEntity animatable, ResourceLocation texture, net.minecraft.client.renderer.MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
