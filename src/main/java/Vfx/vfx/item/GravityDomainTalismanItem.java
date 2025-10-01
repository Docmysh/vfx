package Vfx.vfx.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class GravityDomainTalismanItem extends Item {
    private static final double EFFECT_RADIUS = 12.0D;
    private static final int COOLDOWN_TICKS = 20 * 20; // 20 seconds
    private static final int MAX_DROP_BLOCKS = 2;

    public GravityDomainTalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel serverLevel = serverPlayer.serverLevel();
        AABB area = serverPlayer.getBoundingBox().inflate(EFFECT_RADIUS);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != serverPlayer);

        boolean affectedAny = false;
        for (LivingEntity target : targets) {
            affectedAny |= slamEntityDown(serverLevel, target);
        }

        if (affectedAny) {
            serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.6F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vfx.gravity_domain_talisman"));
    }

    private boolean slamEntityDown(ServerLevel level, LivingEntity entity) {
        double availableDrop = Math.min(MAX_DROP_BLOCKS, entity.getY() - level.getMinBuildHeight());
        if (availableDrop <= 0) {
            return false;
        }

        for (int blocks = MAX_DROP_BLOCKS; blocks > 0; blocks--) {
            if (blocks > availableDrop) {
                continue;
            }

            double offset = -blocks;
            if (level.noCollision(entity, entity.getBoundingBox().move(0.0D, offset, 0.0D))) {
                entity.teleportTo(entity.getX(), entity.getY() + offset, entity.getZ());
                entity.setDeltaMovement(entity.getDeltaMovement().x, -1.2D, entity.getDeltaMovement().z);
                entity.hurtMarked = true;
                return true;
            }
        }

        return false;
    }
}
