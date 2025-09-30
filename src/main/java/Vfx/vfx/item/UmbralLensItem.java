package Vfx.vfx.item;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    private static final String ENABLED_TAG = "Enabled";

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

        if (!isEnabled(stack)) {
            return;
        }

        if (player.hasEffect(MobEffects.DARKNESS)) {
            player.removeEffect(MobEffects.DARKNESS);
        }

        refreshNightVision(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vfx.umbral_lens"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean currentlyEnabled = isEnabled(stack);
        boolean newState = !currentlyEnabled;

        if (!level.isClientSide) {
            setEnabled(stack, newState);
            if (newState) {
                if (player.hasEffect(MobEffects.DARKNESS)) {
                    player.removeEffect(MobEffects.DARKNESS);
                }
                refreshNightVision(player);
            } else {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
            if (stack.getItem() instanceof UmbralLensItem && isEnabled(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnabled(ItemStack stack) {
        if (!stack.hasTag()) {
            return true;
        }

        if (!stack.getTag().contains(ENABLED_TAG)) {
            return true;
        }

        return stack.getTag().getBoolean(ENABLED_TAG);
    }

    private static void setEnabled(ItemStack stack, boolean enabled) {
        stack.getOrCreateTag().putBoolean(ENABLED_TAG, enabled);
    }

    private static void refreshNightVision(Player player) {
        MobEffectInstance existing = player.getEffect(MobEffects.NIGHT_VISION);
        if (existing == null || existing.getDuration() <= NIGHT_VISION_REFRESH_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, false, false, true));
        }
    }
}
