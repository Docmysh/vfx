package Vfx.vfx.client.model;

import Vfx.vfx.Vfx;
import Vfx.vfx.entity.SphereOfDoomEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SphereOfDoomModel extends GeoModel<SphereOfDoomEntity> {
    @Override
    public ResourceLocation getModelResource(SphereOfDoomEntity animatable) {
        return resource("geo/sphere_of_doom.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SphereOfDoomEntity animatable) {
        return resource("textures/entity/sphere_of_doom.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SphereOfDoomEntity animatable) {
        return resource("animations/sphere_doom.animation.json");
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(Vfx.MODID, path);
    }
}
