package Vfx.vfx.item;

import Vfx.vfx.VfxEntities;
import Vfx.vfx.entity.HandGrabEntity;
import Vfx.vfx.entity.shadow.ShadowHandEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ShadowHandRelicItem extends Item {
    private static final int COOLDOWN_TICKS = 20 * 25;

    public ShadowHandRelicItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vfx.shadow_hand_relic"));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (target.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (target.isDeadOrDying()) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        if (ShadowHandEntity.hasActiveHand(target.level(), target)) {
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel = serverPlayer.serverLevel();
        ShadowHandEntity handEntity = VfxEntities.SHADOW_HAND.get().create(serverLevel);
        if (handEntity == null) {
            return InteractionResult.FAIL;
        }

        handEntity.moveTo(target.getX(), target.getY(), target.getZ());
        handEntity.setTarget(target);
        handEntity.setOwner(serverPlayer);
        serverLevel.addFreshEntity(handEntity);

        HandGrabEntity.spawn(serverLevel, serverPlayer, target, 1.0F, 40);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.swing(hand, true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
