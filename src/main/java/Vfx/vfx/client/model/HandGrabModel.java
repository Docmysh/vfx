package Vfx.vfx.client.model;

import Vfx.vfx.Vfx;
import Vfx.vfx.entity.HandGrabEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HandGrabModel extends GeoModel<HandGrabEntity> {
    @Override
    public ResourceLocation getModelResource(HandGrabEntity animatable) {
        return resource("geo/hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HandGrabEntity animatable) {
        return resource("textures/entity/hand.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HandGrabEntity animatable) {
        return resource("animations/hand.animation.json");
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(Vfx.MODID, path);
    }
}
