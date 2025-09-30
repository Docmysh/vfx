package Vfx.vfx.item;

import Vfx.vfx.Vfx;
import Vfx.vfx.shadow.ShadowSummonManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

    @SubscribeEvent
    public static void onShadowInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        Entity target = event.getTarget();
        if (!(target instanceof Mob mob)) {
            return;
        }

        if (!ShadowSummonManager.isShadowEntity(mob) || !ShadowSummonManager.isOwnedBy(mob, serverPlayer)) {
            return;
        }

        EquipmentSlot slot = Mob.getEquipmentSlotForItem(stack);
        if (!canEquipShadowItem(mob, stack, slot)) {
            return;
        }

        ItemStack toEquip = stack.copy();
        toEquip.setCount(1);

        if (!equipShadowItem(player, mob, slot, toEquip)) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        player.swing(event.getHand(), true);
    }

    private static boolean canEquipShadowItem(Mob mob, ItemStack stack, EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            return mob.isSlotEnabled(slot) && mob.canTakeItem(stack);
        }
        if (slot.getType() == EquipmentSlot.Type.HAND) {
            return mob.isSlotEnabled(slot) && mob.canHoldItem(stack);
        }
        return false;
    }

    private static boolean equipShadowItem(Player player, Mob mob, EquipmentSlot slot, ItemStack item) {
        ItemStack existing = mob.getItemBySlot(slot);
        if (ItemStack.isSameItemSameTags(existing, item)) {
            return false;
        }

        ItemStack previous = existing.copy();
        mob.setItemSlot(slot, item);
        mob.setDropChance(slot, 0.0F);

        if (!previous.isEmpty()) {
            if (!player.addItem(previous)) {
                player.drop(previous, false);
            }
        }

        return true;
    }
}
