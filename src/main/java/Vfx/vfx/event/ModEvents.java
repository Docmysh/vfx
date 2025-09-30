package Vfx.vfx.event;

import Vfx.vfx.Vfx;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Vfx.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    @SubscribeEvent
    public static void onAttributeModify(EntityAttributeModificationEvent event) {
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            if (LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
                @SuppressWarnings("unchecked")
                EntityType<? extends LivingEntity> livingEntityType = (EntityType<? extends LivingEntity>) type;
                if (!event.has(livingEntityType, Attributes.ATTACK_DAMAGE)) {
                    event.add(livingEntityType, Attributes.ATTACK_DAMAGE, 0.0D);
                }
            }
        }
    }
}
