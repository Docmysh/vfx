package Vfx.vfx.item;

import Vfx.vfx.domain.DarknessDomainManager;
import Vfx.vfx.shadow.ShadowSummonManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import Vfx.vfx.menu.ShadowSelectionMenu;
import net.minecraft.world.entity.LivingEntity;


public class ShadowCollectorItem extends Item {
    private static final String TAG_SHADOWS = "Shadows";
    private static final String TAG_BEHAVIOR = "ShadowBehavior";
    private static final int MAX_OUTSIDE_SHADOWS = 3;

    public enum ShadowBehavior {
        PASSIVE("behavior.vfx.shadow.passive"),
        NEUTRAL("behavior.vfx.shadow.neutral"),
        HOSTILE("behavior.vfx.shadow.hostile");

        private final String translationKey;

        ShadowBehavior(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayName() {
            return Component.translatable(this.translationKey);
        }

        public ShadowBehavior next() {
            ShadowBehavior[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static ShadowBehavior byName(String name) {
            for (ShadowBehavior behavior : values()) {
                if (behavior.name().equalsIgnoreCase(name)) {
                    return behavior;
                }
            }
            return PASSIVE;
        }
    }


    public ShadowCollectorItem(Properties properties) {
        super(properties);
    }

    public static ShadowBehavior getBehavior(ItemStack stack) {
        if (!(stack.getItem() instanceof ShadowCollectorItem)) {
            return ShadowBehavior.PASSIVE;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_BEHAVIOR, Tag.TAG_STRING)) {
            return ShadowBehavior.byName(tag.getString(TAG_BEHAVIOR));
        }
        return ShadowBehavior.PASSIVE;
    }

    public static void setBehavior(ItemStack stack, ShadowBehavior behavior) {
        if (!(stack.getItem() instanceof ShadowCollectorItem)) {
            return;
        }
        stack.getOrCreateTag().putString(TAG_BEHAVIOR, behavior.name());
    }

    public static ShadowBehavior cycleBehavior(ItemStack stack) {
        ShadowBehavior next = getBehavior(stack).next();
        setBehavior(stack, next);
        return next;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            ShadowBehavior behavior = cycleBehavior(stack);
            Component message = Component.translatable("message.vfx.shadow_collector.behavior", behavior.getDisplayName());
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(message, true);
                ShadowSummonManager.updateOwnerBehavior(serverPlayer, behavior);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        List<ResourceLocation> stored = getStoredShadows(stack);
        if (stored.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.vfx.shadow_collector.empty"), true);
            return InteractionResultHolder.success(stack);
        }

        ServerLevel serverLevel = serverPlayer.serverLevel();
        DarknessDomainManager manager = DarknessDomainManager.get(serverLevel);
        BlockPos pos = serverPlayer.blockPosition();
        if (manager.isInsideDomain(pos)) {
            summonAllShadows(serverPlayer, stack, stored);
            return InteractionResultHolder.success(stack);
        }

        NetworkHooks.openScreen(serverPlayer,
                new SimpleMenuProvider((id, inventory, playerEntity) ->
                        new ShadowSelectionMenu(id, inventory, new ArrayList<>(stored), hand),
                        Component.translatable("screen.vfx.shadow_collector")),
                buf -> ShadowSelectionMenu.writeData(buf, stored, hand));

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vfx.shadow_collector.count", getShadowCount(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.vfx.shadow_collector.behavior", getBehavior(stack).getDisplayName())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    public static boolean storeShadow(ItemStack stack, Entity entity) {
        if (!(stack.getItem() instanceof ShadowCollectorItem)) {
            return false;
        }
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeId == null) {
            return false;
        }

        CompoundTag tag = stack.getOrCreateTag();
        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        for (Tag element : listTag) {
            if (element instanceof StringTag stringTag && Objects.equals(stringTag.getAsString(), typeId.toString())) {
                return false;
            }
        }

        listTag.add(StringTag.valueOf(typeId.toString()));
        tag.put(TAG_SHADOWS, listTag);
        return true;
    }

    public static List<ResourceLocation> getStoredShadows(ItemStack stack) {
        List<ResourceLocation> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SHADOWS, Tag.TAG_LIST)) {
            return result;
        }

        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        for (Tag element : listTag) {
            if (element instanceof StringTag stringTag) {
                ResourceLocation id = ResourceLocation.tryParse(stringTag.getAsString());
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    public static int getShadowCount(ItemStack stack) {
        return getStoredShadows(stack).size();
    }

    public static void consumeShadow(ItemStack stack, ResourceLocation typeId) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SHADOWS, Tag.TAG_LIST)) {
            return;
        }

        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        ListTag updated = new ListTag();
        for (Tag element : listTag) {
            if (element instanceof StringTag stringTag && stringTag.getAsString().equals(typeId.toString())) {
                continue;
            }
            updated.add(element.copy());
        }
        tag.put(TAG_SHADOWS, updated);
    }

    private static void summonAllShadows(ServerPlayer player, ItemStack stack, List<ResourceLocation> stored) {
        Set<ResourceLocation> summoned = new HashSet<>();
        ShadowBehavior behavior = getBehavior(stack);
        for (ResourceLocation typeId : stored) {
            if (summonShadow(player, typeId, behavior, false, true)) {
                summoned.add(typeId);
            }
        }
        if (!summoned.isEmpty()) {
            clearShadows(stack, summoned);
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.summon_all", summoned.size()), true);
        } else {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
        }
    }

    public static boolean summonShadow(ServerPlayer player, ResourceLocation typeId, ShadowBehavior behavior) {
        return summonShadow(player, typeId, behavior, true, false);
    }

    public static boolean summonShadow(ServerPlayer player, ResourceLocation typeId, ShadowBehavior behavior, boolean notifyPlayer) {
        return summonShadow(player, typeId, behavior, notifyPlayer, false);
    }

    private static boolean summonShadow(ServerPlayer player, ResourceLocation typeId, ShadowBehavior behavior, boolean notifyPlayer, boolean ignoreLimit) {
        ServerLevel level = player.serverLevel();
        boolean insideDomain = DarknessDomainManager.get(level).isInsideDomain(player.blockPosition());
        if (!ignoreLimit && !insideDomain) {
            int outsideShadows = ShadowSummonManager.getActiveShadowCount(player, mob -> {
                if (!(mob.level() instanceof ServerLevel mobLevel)) {
                    return true;
                }
                return !DarknessDomainManager.isInsideAnyDomain(mobLevel, mob.blockPosition());
            });
            if (outsideShadows >= MAX_OUTSIDE_SHADOWS) {
                if (notifyPlayer) {
                    player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.limit", MAX_OUTSIDE_SHADOWS), true);
                }
                return false;
            }
        }

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(typeId);
        if (entityType == null) {
            if (notifyPlayer) {
                player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            }
            return false;
        }

        Entity entity = entityType.create(level);
        if (!(entity instanceof Mob mob)) {
            if (notifyPlayer) {
                player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            }
            return false;
        }

        mob.moveTo(player.getX(), player.getY(), player.getZ(), level.random.nextFloat() * 360.0F, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        applyShadowAppearance(mob);
        ShadowSummonManager.registerShadow(mob, player, behavior);
        level.addFreshEntity(mob);
        if (notifyPlayer) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.summoned", mob.getDisplayName()), true);
        }
        return true;
    }

    private static void applyShadowAppearance(Mob mob) {
        mob.setCustomName(Component.translatable("entity.vfx.shadow", mob.getType().getDescription()).withStyle(ChatFormatting.DARK_GRAY));
        mob.setCustomNameVisible(true);
        mob.setSilent(true);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
            if (!mob.getItemBySlot(slot).isEmpty()) {
                mob.setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        Level level = mob.level();
        if (level instanceof ServerLevel serverLevel) {
            Scoreboard scoreboard = serverLevel.getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam(ShadowSummonManager.SHADOW_TEAM);
            if (team == null) {
                team = scoreboard.addPlayerTeam(ShadowSummonManager.SHADOW_TEAM);
                team.setColor(ChatFormatting.BLACK);
                team.setAllowFriendlyFire(false);
                team.setSeeFriendlyInvisibles(true);
                team.setCollisionRule(Team.CollisionRule.NEVER);
                team.setDeathMessageVisibility(Team.Visibility.NEVER);
            }
            scoreboard.addPlayerToTeam(mob.getStringUUID(), team);
        }
    }

    private static void clearShadows(ItemStack stack, Set<ResourceLocation> summoned) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SHADOWS, Tag.TAG_LIST)) {
            return;
        }

        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        ListTag updated = new ListTag();
        for (Tag element : listTag) {
            if (element instanceof StringTag stringTag) {
                ResourceLocation id = ResourceLocation.tryParse(stringTag.getAsString());
                if (id != null && summoned.contains(id)) {
                    continue;
                }
            }
            updated.add(element.copy());
        }
        tag.put(TAG_SHADOWS, updated);
    }

    public static void clearAll(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_SHADOWS);
        }
    }
}