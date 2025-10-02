package Vfx.vfx;

import Vfx.vfx.entity.GravityWellFieldEntity;
import Vfx.vfx.entity.HandGrabEntity;
import Vfx.vfx.entity.ShadowBallEntity;
import Vfx.vfx.entity.SingularityCoreEntity;
import Vfx.vfx.entity.SphereOfDoomEntity;
import Vfx.vfx.entity.shadow.ShadowHandEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VfxEntities {
    private VfxEntities() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Vfx.MODID);

    public static final RegistryObject<EntityType<ShadowHandEntity>> SHADOW_HAND = ENTITY_TYPES.register(
            "shadow_hand",
            () -> EntityType.Builder.<ShadowHandEntity>of(ShadowHandEntity::new, MobCategory.MISC)
                    .sized(1.5F, 2.5F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("shadow_hand")
    );

    public static final RegistryObject<EntityType<HandGrabEntity>> HAND_GRAB = ENTITY_TYPES.register(
            "hand_grab",
            () -> EntityType.Builder.<HandGrabEntity>of(HandGrabEntity::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("hand_grab")
    );

    public static final RegistryObject<EntityType<ShadowBallEntity>> SHADOW_BALL = ENTITY_TYPES.register(
            "shadow_ball",
            () -> EntityType.Builder.<ShadowBallEntity>of(ShadowBallEntity::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("shadow_ball")
    );

    public static final RegistryObject<EntityType<SingularityCoreEntity>> SINGULARITY_CORE = ENTITY_TYPES.register(
            "singularity_core",
            () -> EntityType.Builder.<SingularityCoreEntity>of(SingularityCoreEntity::new, MobCategory.MISC)
                    .sized(2.5F, 2.5F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("singularity_core")
    );

    public static final RegistryObject<EntityType<SphereOfDoomEntity>> SPHERE_OF_DOOM = ENTITY_TYPES.register(
            "sphere_of_doom",
            () -> EntityType.Builder.<SphereOfDoomEntity>of(SphereOfDoomEntity::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("sphere_of_doom")
    );

    public static final RegistryObject<EntityType<GravityWellFieldEntity>> GRAVITY_WELL = ENTITY_TYPES.register(
            "gravity_well",
            () -> EntityType.Builder.<GravityWellFieldEntity>of(GravityWellFieldEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("gravity_well")
    );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
