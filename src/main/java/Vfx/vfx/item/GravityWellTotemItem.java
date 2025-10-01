package Vfx.vfx.item;

import Vfx.vfx.entity.GravityWellFieldEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class GravityWellTotemItem extends Item {
    private static final int DURATION_TICKS = 20 * 12;
    private static final int COOLDOWN_TICKS = 20 * 25;
    private static final double SPAWN_OFFSET = 3.0D;

    public GravityWellTotemItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vfx.gravity_well_totem"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(SPAWN_OFFSET));
        BlockPos.MutableBlockPos mutable = BlockPos.containing(spawnPos).mutable();
        double spawnY = spawnPos.y;

        while (mutable.getY() > serverLevel.getMinBuildHeight() && serverLevel.isEmptyBlock(mutable)) {
            mutable.move(Direction.DOWN);
        }

        if (!serverLevel.isEmptyBlock(mutable)) {
            spawnY = mutable.getY() + 1.0D;
        }

        Vec3 adjustedPos = new Vec3(spawnPos.x, spawnY, spawnPos.z);

        GravityWellFieldEntity entity = GravityWellFieldEntity.spawn(serverLevel, adjustedPos, DURATION_TICKS, serverPlayer);
        if (entity == null) {
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
