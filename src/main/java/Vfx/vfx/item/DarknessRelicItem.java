package Vfx.vfx.item;

import Vfx.vfx.domain.DarknessDomainManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DarknessRelicItem extends Item {
    private static final int DOMAIN_SIZE = 25;
    private static final int DOMAIN_DURATION_TICKS = 20 * 30; // 30 seconds

    public DarknessRelicItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockPos center = player.blockPosition();
            DarknessDomainManager.get(serverLevel).activateDomain(center, DOMAIN_SIZE, DOMAIN_DURATION_TICKS);
        }

        player.getCooldowns().addCooldown(this, DOMAIN_DURATION_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}