package Vfx.vfx;

import Vfx.vfx.entity.HandGrabEntity;
import Vfx.vfx.entity.ShadowBallEntity;
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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
