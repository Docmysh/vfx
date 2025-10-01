package Vfx.vfx.item;

import Vfx.vfx.VfxEntities;
import Vfx.vfx.entity.HandGrabEntity;
import Vfx.vfx.entity.shadow.ShadowHandEntity;
import Vfx.vfx.entity.shadow.ShadowHandMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class ShadowHandRelicItem extends Item {
    private static final String TAG_MODE = "Mode";
    private static final double CAST_RANGE = 6.0D;

    public ShadowHandRelicItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vfx.shadow_hand_relic"));
        tooltip.add(Component.translatable("tooltip.vfx.shadow_hand_relic.mode",
                getMode(stack).getDisplayName()));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (tryReleaseExistingThrow(serverPlayer)) {
            player.swing(hand, true);
            return InteractionResult.SUCCESS;
        }
        if (target.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (target.isDeadOrDying()) {
            return InteractionResult.PASS;
        }
        if (ShadowHandEntity.hasActiveHand(target.level(), target)) {
            return InteractionResult.FAIL;
        }
        if (summonHand(serverPlayer, stack, target, hand)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ShadowHandMode mode = getMode(stack).cycle();
                setMode(stack, mode);
                player.displayClientMessage(Component.translatable("message.vfx.shadow_hand_relic.mode",
                        mode.getDisplayName()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        if (tryReleaseExistingThrow(serverPlayer)) {
            player.swing(hand, true);
            return InteractionResultHolder.success(stack);
        }
        LivingEntity target = findTarget(serverPlayer, CAST_RANGE);
        if (target != null && !target.isDeadOrDying()) {
            if (!ShadowHandEntity.hasActiveHand(target.level(), target) && summonHand(serverPlayer, stack, target, hand)) {
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    private ShadowHandMode getMode(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_MODE)) {
            return ShadowHandMode.byId(stack.getTag().getInt(TAG_MODE));
        }
        return ShadowHandMode.CRUSH;
    }

    private void setMode(ItemStack stack, ShadowHandMode mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.getId());
    }

    private boolean summonHand(ServerPlayer serverPlayer, ItemStack stack, LivingEntity target, InteractionHand hand) {
        ServerLevel serverLevel = serverPlayer.serverLevel();
        ShadowHandEntity handEntity = VfxEntities.SHADOW_HAND.get().create(serverLevel);
        if (handEntity == null) {
            return false;
        }

        ShadowHandMode mode = getMode(stack);
        handEntity.moveTo(target.getX(), target.getY(), target.getZ());
        handEntity.setTarget(target);
        handEntity.setOwner(serverPlayer);
        handEntity.setMode(mode);
        serverLevel.addFreshEntity(handEntity);

        int duration = mode == ShadowHandMode.THROW ? ShadowHandEntity.getTotalAnimationTicks() : ShadowHandEntity.getGraspTicks();
        HandGrabEntity grabEntity = HandGrabEntity.spawn(serverLevel, serverPlayer, target, 1.0F, duration, mode);
        if (grabEntity == null) {
            handEntity.discard();
            return false;
        }

        handEntity.setGrabEntity(grabEntity);
        serverPlayer.swing(hand, true);
        return true;
    }

    private boolean tryReleaseExistingThrow(ServerPlayer player) {
        HandGrabEntity awaiting = HandGrabEntity.findAwaitingThrow(player.serverLevel(), player);
        if (awaiting != null) {
            awaiting.requestManualRelease();
            return true;
        }
        return false;
    }

    @Nullable
    private LivingEntity findTarget(Player player, double range) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 reachVector = eyePosition.add(look.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult result = ProjectileUtil.getEntityHitResult(player.level(), player, eyePosition, reachVector, searchBox,
                entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player && entity.isPickable());
        if (result != null && result.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
