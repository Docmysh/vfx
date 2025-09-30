package Vfx.vfx.item;

import Vfx.vfx.Vfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Vfx.MODID)
public class ShadowCollectorEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity) || target.getType() == EntityType.PLAYER) {
            return;
        }

        boolean stored = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ShadowCollectorItem && ShadowCollectorItem.storeShadow(stack, target)) {
                stored = true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ShadowCollectorItem && ShadowCollectorItem.storeShadow(stack, target)) {
                stored = true;
            }
        }

        if (stored) {
            serverPlayer.displayClientMessage(Component.translatable("message.vfx.shadow_collector.captured", target.getDisplayName()), true);
        }
    }
}