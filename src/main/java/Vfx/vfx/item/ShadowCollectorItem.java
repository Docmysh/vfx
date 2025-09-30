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
import net.minecraft.world.InteractionResult;
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
import java.util.List;

import Vfx.vfx.menu.ShadowSelectionMenu;
import net.minecraft.world.entity.LivingEntity;


public class ShadowCollectorItem extends Item {
    private static final String TAG_SHADOWS = "Shadows";
    private static final String TAG_FAVORITE_SHADOWS = "FavoriteShadows";
    private static final String KEY_FAVORITE_NAME = "Name";
    private static final String KEY_FAVORITE_TYPE = "EntityType";
    private static final String KEY_FAVORITE_DATA = "EntityData";
    private static final String TAG_BEHAVIOR = "ShadowBehavior";
    private static final int MAX_OUTSIDE_SHADOWS = 3;
    private static final int MAX_FAVORITE_SHADOWS = 2;

    public record ShadowEntry(ResourceLocation typeId, boolean favorite, int index, String name) {
    }

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

        List<ShadowEntry> entries = getShadowEntries(stack);
        if (entries.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.vfx.shadow_collector.empty"), true);
            return InteractionResultHolder.success(stack);
        }

        ShadowBehavior behavior = getBehavior(stack);
        List<ResourceLocation> stored = getStoredShadows(stack);
        ServerLevel serverLevel = serverPlayer.serverLevel();
        DarknessDomainManager manager = DarknessDomainManager.get(serverLevel);
        BlockPos pos = serverPlayer.blockPosition();
        if (manager.isInsideDomain(pos) && !stored.isEmpty()) {
            if (summonAllShadows(serverPlayer, stack, behavior)) {
                return InteractionResultHolder.success(stack);
            }
        }

        NetworkHooks.openScreen(serverPlayer,
                new SimpleMenuProvider((id, inventory, playerEntity) ->
                        new ShadowSelectionMenu(id, inventory, new ArrayList<>(entries), hand),
                        Component.translatable("screen.vfx.shadow_collector")),
                buf -> ShadowSelectionMenu.writeData(buf, entries, hand));

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.vfx.shadow_collector.count", getShadowCount(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.vfx.shadow_collector.behavior", getBehavior(stack).getDisplayName())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!ShadowSummonManager.isShadowEntity(mob) || !ShadowSummonManager.isOwnedBy(mob, serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (storeShadow(stack, mob)) {
            ShadowSummonManager.unregisterShadow(mob);
            mob.discard();
            serverPlayer.displayClientMessage(Component.translatable("message.vfx.shadow_collector.captured", mob.getDisplayName()), true);
            return InteractionResult.CONSUME;
        }

        serverPlayer.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
        return InteractionResult.CONSUME;
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

        if (entity instanceof Mob mob && mob.hasCustomName()) {
            return storeFavoriteShadow(stack, mob, typeId);
        }

        return storeStandardShadow(stack, typeId);
    }

    private static boolean storeStandardShadow(ItemStack stack, ResourceLocation typeId) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        listTag.add(StringTag.valueOf(typeId.toString()));
        tag.put(TAG_SHADOWS, listTag);
        return true;
    }

    private static boolean storeFavoriteShadow(ItemStack stack, Mob mob, ResourceLocation typeId) {
        Component customName = mob.getCustomName();
        if (customName == null) {
            return false;
        }

        String name = customName.getString().trim();
        if (name.isEmpty()) {
            return false;
        }

        CompoundTag tag = stack.getOrCreateTag();
        ListTag favorites = tag.getList(TAG_FAVORITE_SHADOWS, Tag.TAG_COMPOUND);

        int existingIndex = -1;
        for (int i = 0; i < favorites.size(); i++) {
            CompoundTag favoriteTag = favorites.getCompound(i);
            if (name.equals(favoriteTag.getString(KEY_FAVORITE_NAME))) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex == -1 && favorites.size() >= MAX_FAVORITE_SHADOWS) {
            return false;
        }

        CompoundTag entityData = new CompoundTag();
        mob.saveWithoutId(entityData);

        CompoundTag favoriteTag = new CompoundTag();
        favoriteTag.putString(KEY_FAVORITE_NAME, name);
        favoriteTag.putString(KEY_FAVORITE_TYPE, typeId.toString());
        favoriteTag.put(KEY_FAVORITE_DATA, entityData);

        if (existingIndex >= 0) {
            favorites.set(existingIndex, favoriteTag);
        } else {
            favorites.add(favoriteTag);
        }

        tag.put(TAG_FAVORITE_SHADOWS, favorites);
        return true;
    }

    public static List<ResourceLocation> getStoredShadows(ItemStack stack) {
        List<ResourceLocation> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SHADOWS, Tag.TAG_LIST)) {
            return result;
        }

        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        for (int i = 0; i < listTag.size(); i++) {
            Tag element = listTag.get(i);
            if (element instanceof StringTag stringTag) {
                ResourceLocation id = ResourceLocation.tryParse(stringTag.getAsString());
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    public static List<ShadowEntry> getShadowEntries(ItemStack stack) {
        List<ShadowEntry> entries = new ArrayList<>();
        entries.addAll(getFavoriteEntries(stack));
        entries.addAll(getStandardEntries(stack));
        return entries;
    }

    private static List<ShadowEntry> getFavoriteEntries(ItemStack stack) {
        List<ShadowEntry> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_FAVORITE_SHADOWS, Tag.TAG_LIST)) {
            return result;
        }

        ListTag listTag = tag.getList(TAG_FAVORITE_SHADOWS, Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag favoriteTag = listTag.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(favoriteTag.getString(KEY_FAVORITE_TYPE));
            if (id != null) {
                String name = favoriteTag.getString(KEY_FAVORITE_NAME);
                result.add(new ShadowEntry(id, true, i, name));
            }
        }
        return result;
    }

    private static List<ShadowEntry> getStandardEntries(ItemStack stack) {
        List<ShadowEntry> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SHADOWS, Tag.TAG_LIST)) {
            return result;
        }

        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        for (int i = 0; i < listTag.size(); i++) {
            Tag element = listTag.get(i);
            if (element instanceof StringTag stringTag) {
                ResourceLocation id = ResourceLocation.tryParse(stringTag.getAsString());
                if (id != null) {
                    result.add(new ShadowEntry(id, false, i, ""));
                }
            }
        }
        return result;
    }

    public static int getShadowCount(ItemStack stack) {
        return getStoredShadows(stack).size() + getFavoriteEntries(stack).size();
    }

    private static void removeStoredShadow(ItemStack stack, int index) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SHADOWS, Tag.TAG_LIST)) {
            return;
        }

        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        if (index >= 0 && index < listTag.size()) {
            listTag.remove(index);
        }

        if (listTag.isEmpty()) {
            tag.remove(TAG_SHADOWS);
        } else {
            tag.put(TAG_SHADOWS, listTag);
        }
    }

    private static void removeFavoriteShadow(ItemStack stack, int index) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_FAVORITE_SHADOWS, Tag.TAG_LIST)) {
            return;
        }

        ListTag listTag = tag.getList(TAG_FAVORITE_SHADOWS, Tag.TAG_COMPOUND);
        if (index >= 0 && index < listTag.size()) {
            listTag.remove(index);
        }

        if (listTag.isEmpty()) {
            tag.remove(TAG_FAVORITE_SHADOWS);
        } else {
            tag.put(TAG_FAVORITE_SHADOWS, listTag);
        }
    }

    private static boolean summonAllShadows(ServerPlayer player, ItemStack stack, ShadowBehavior behavior) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SHADOWS, Tag.TAG_LIST)) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            return false;
        }

        ListTag listTag = tag.getList(TAG_SHADOWS, Tag.TAG_STRING);
        if (listTag.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            return false;
        }

        List<Integer> summonedIndices = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            Tag element = listTag.get(i);
            if (element instanceof StringTag stringTag) {
                ResourceLocation typeId = ResourceLocation.tryParse(stringTag.getAsString());
                if (typeId != null && summonShadow(player, typeId, behavior, false, true)) {
                    summonedIndices.add(i);
                }
            }
        }

        if (!summonedIndices.isEmpty()) {
            for (int i = summonedIndices.size() - 1; i >= 0; i--) {
                int index = summonedIndices.get(i);
                listTag.remove(index);
            }

            if (listTag.isEmpty()) {
                tag.remove(TAG_SHADOWS);
            } else {
                tag.put(TAG_SHADOWS, listTag);
            }

            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.summon_all", summonedIndices.size()), true);
            return true;
        }

        player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
        return false;
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
        applyShadowAppearance(mob, false);
        ShadowSummonManager.registerShadow(mob, player, behavior);
        level.addFreshEntity(mob);
        if (notifyPlayer) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.summoned", mob.getDisplayName()), true);
        }
        return true;
    }

    public static boolean summonStoredEntry(ServerPlayer player, ItemStack stack, ShadowEntry entry, ShadowBehavior behavior) {
        if (entry.favorite()) {
            boolean summoned = summonFavoriteShadow(player, stack, entry.index(), behavior);
            if (summoned) {
                removeFavoriteShadow(stack, entry.index());
            }
            return summoned;
        }

        if (summonShadow(player, entry.typeId(), behavior)) {
            removeStoredShadow(stack, entry.index());
            return true;
        }
        return false;
    }

    private static boolean summonFavoriteShadow(ServerPlayer player, ItemStack stack, int index, ShadowBehavior behavior) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_FAVORITE_SHADOWS, Tag.TAG_LIST)) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            return false;
        }

        ListTag favorites = tag.getList(TAG_FAVORITE_SHADOWS, Tag.TAG_COMPOUND);
        if (index < 0 || index >= favorites.size()) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            return false;
        }

        CompoundTag favoriteTag = favorites.getCompound(index);
        ResourceLocation typeId = ResourceLocation.tryParse(favoriteTag.getString(KEY_FAVORITE_TYPE));
        if (typeId == null) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            return false;
        }

        ServerLevel level = player.serverLevel();
        boolean insideDomain = DarknessDomainManager.get(level).isInsideDomain(player.blockPosition());
        if (!insideDomain) {
            int outsideShadows = ShadowSummonManager.getActiveShadowCount(player, mob -> {
                if (!(mob.level() instanceof ServerLevel mobLevel)) {
                    return true;
                }
                return !DarknessDomainManager.isInsideAnyDomain(mobLevel, mob.blockPosition());
            });
            if (outsideShadows >= MAX_OUTSIDE_SHADOWS) {
                player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.limit", MAX_OUTSIDE_SHADOWS), true);
                return false;
            }
        }

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(typeId);
        if (entityType == null) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            return false;
        }

        Entity entity = entityType.create(level);
        if (!(entity instanceof Mob mob)) {
            player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.failed"), true);
            return false;
        }

        CompoundTag entityData = favoriteTag.getCompound(KEY_FAVORITE_DATA).copy();
        mob.load(entityData);
        mob.moveTo(player.getX(), player.getY(), player.getZ(), level.random.nextFloat() * 360.0F, 0);
        mob.setYHeadRot(mob.getYRot());
        mob.setDeltaMovement(0, 0, 0);

        applyShadowAppearance(mob, true);
        ShadowSummonManager.registerShadow(mob, player, behavior);
        level.addFreshEntity(mob);
        player.displayClientMessage(Component.translatable("message.vfx.shadow_collector.summoned", mob.getDisplayName()), true);
        return true;
    }

    private static void applyShadowAppearance(Mob mob, boolean preserveEquipmentAndName) {
        if (!preserveEquipmentAndName) {
            mob.setCustomName(Component.translatable("entity.vfx.shadow", mob.getType().getDescription()).withStyle(ChatFormatting.DARK_GRAY));
        }
        mob.setCustomNameVisible(true);
        mob.setSilent(true);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
            if (!preserveEquipmentAndName && !mob.getItemBySlot(slot).isEmpty()) {
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

    public static void clearAll(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_SHADOWS);
            tag.remove(TAG_FAVORITE_SHADOWS);
        }
    }
}