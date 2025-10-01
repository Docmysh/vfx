package Vfx.vfx.client.model;

import Vfx.vfx.Vfx;
import Vfx.vfx.entity.ShadowBallEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShadowBallModel extends GeoModel<ShadowBallEntity> {
    @Override
    public ResourceLocation getModelResource(ShadowBallEntity animatable) {
        return resource("geo/ball_of_shadow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ShadowBallEntity animatable) {
        return resource("textures/entity/ball_of_shadow.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShadowBallEntity animatable) {
        return resource("animations/ball_of_shadow.animation.json");
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(Vfx.MODID, path);
    }
}
