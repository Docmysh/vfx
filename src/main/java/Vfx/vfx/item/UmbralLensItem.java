package Vfx.vfx.item;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class UmbralLensItem extends Item {
    private static final int NIGHT_VISION_DURATION_TICKS = 220;
    /**
     * Night vision begins to flash once its remaining duration dips below 200 ticks.
     * Refresh slightly above that threshold so the player never experiences the flicker.
     */
    private static final int NIGHT_VISION_REFRESH_THRESHOLD_TICKS = 205;

    public UmbralLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        if (player.hasEffect(MobEffects.DARKNESS)) {
            player.removeEffect(MobEffects.DARKNESS);
        }

        MobEffectInstance existing = player.getEffect(MobEffects.NIGHT_VISION);
        if (existing == null || existing.getDuration() <= NIGHT_VISION_REFRESH_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, false, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vfx.umbral_lens"));
    }

    public static boolean hasMonochromeVision(Player player) {
        if (player == null || player.isSpectator()) {
            return false;
        }

        return containsLens(player.getInventory().items)
                || containsLens(player.getInventory().offhand)
                || containsLens(player.getInventory().armor);
    }

    private static boolean containsLens(NonNullList<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof UmbralLensItem) {
                return true;
            }
        }
        return false;
    }
}
