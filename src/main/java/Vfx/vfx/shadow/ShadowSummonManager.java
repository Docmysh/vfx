package Vfx.vfx.shadow;

import Vfx.vfx.Vfx;
import Vfx.vfx.item.ShadowCollectorItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = Vfx.MODID)
public class ShadowSummonManager {
    public static final String SHADOW_TAG = "vfx_shadow_entity";
    public static final String SHADOW_TEAM = "vfx_shadow_team";
    private static final String BEHAVIOR_TAG = "vfx_shadow_behavior";
    private static final String OWNER_TAG = "vfx_shadow_owner";
    private static final String COMBAT_READY_TAG = "vfx_shadow_combat_ready";

    private static final Map<UUID, ShadowData> SHADOWS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> SHADOWS_BY_OWNER = new HashMap<>();

    public static void registerShadow(Mob mob, ServerPlayer owner, ShadowCollectorItem.ShadowBehavior behavior) {
        mob.getPersistentData().putBoolean(SHADOW_TAG, true);
        mob.getPersistentData().putString(BEHAVIOR_TAG, behavior.name());
        mob.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        mob.setPersistenceRequired();
        mob.setNoAi(false);

        if (!mob.getPersistentData().getBoolean(COMBAT_READY_TAG)) {
            prepareShadowForCombat(mob);
            mob.getPersistentData().putBoolean(COMBAT_READY_TAG, true);
        }

        ShadowData data = new ShadowData(mob.level().dimension(), owner.getUUID());
        SHADOWS.put(mob.getUUID(), data);
        SHADOWS_BY_OWNER.computeIfAbsent(owner.getUUID(), id -> new HashSet<>()).add(mob.getUUID());
    }

    public static boolean isShadowEntity(Entity entity) {
        if (!(entity instanceof Mob mob)) {
            return false;
        }
        if (mob.getPersistentData().getBoolean(SHADOW_TAG)) {
            return true;
        }

        Team team = mob.getTeam();
        return team != null && SHADOW_TEAM.equals(team.getName());
    }

    public static int getActiveShadowCount(ServerPlayer owner) {
        return getActiveShadowCount(owner, mob -> true);
    }

    public static int getActiveShadowCount(ServerPlayer owner, Predicate<Mob> filter) {
        cleanupOwner(owner.getUUID(), owner.server);
        Set<UUID> ids = SHADOWS_BY_OWNER.get(owner.getUUID());
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (UUID id : ids) {
            ShadowData data = SHADOWS.get(id);
            if (data == null) {
                continue;
            }
            ServerLevel level = owner.server.getLevel(data.levelKey());
            if (level == null) {
                continue;
            }
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive() || !mob.getPersistentData().getBoolean(SHADOW_TAG)) {
                continue;
            }
            if (filter.test(mob)) {
                count++;
            }
        }
        return count;
    }

    public static void updateOwnerBehavior(ServerPlayer owner, ShadowCollectorItem.ShadowBehavior behavior) {
        Set<UUID> ids = SHADOWS_BY_OWNER.get(owner.getUUID());
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (UUID id : new HashSet<>(ids)) {
            ShadowData data = SHADOWS.get(id);
            if (data == null) {
                continue;
            }
            ServerLevel level = owner.server.getLevel(data.levelKey());
            if (level == null) {
                continue;
            }
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob) {
                mob.getPersistentData().putString(BEHAVIOR_TAG, behavior.name());
            }
        }
    }

    @SubscribeEvent
    public static void handleServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, ShadowData>> iterator = SHADOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ShadowData> entry = iterator.next();
            UUID shadowId = entry.getKey();
            ShadowData data = entry.getValue();
            ServerLevel level = server.getLevel(data.levelKey());
            if (level == null) {
                iterator.remove();
                removeFromOwner(data.ownerId(), shadowId);
                continue;
            }
            Entity entity = level.getEntity(shadowId);
            if (!(entity instanceof Mob mob) || !mob.isAlive() || !mob.getPersistentData().getBoolean(SHADOW_TAG)) {
                iterator.remove();
                removeFromOwner(data.ownerId(), shadowId);
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerId());
            if (owner == null || owner.isDeadOrDying()) {
                mob.discard();
                iterator.remove();
                removeFromOwner(data.ownerId(), shadowId);
                continue;
            }
            ShadowCollectorItem.ShadowBehavior behavior = ShadowCollectorItem.ShadowBehavior.byName(mob.getPersistentData().getString(BEHAVIOR_TAG));
            updateShadow(mob, owner, behavior);
        }
    }

    @SubscribeEvent
    public static void onShadowDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!mob.getPersistentData().getBoolean(SHADOW_TAG)) {
            return;
        }
        unregister(mob);
    }

    private static void unregister(Mob mob) {
        UUID id = mob.getUUID();
        ShadowData data = SHADOWS.remove(id);
        if (data != null) {
            removeFromOwner(data.ownerId(), id);
        }
    }

    private static void cleanupOwner(UUID ownerId, MinecraftServer server) {
        Set<UUID> ids = SHADOWS_BY_OWNER.get(ownerId);
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Iterator<UUID> iterator = ids.iterator();
        while (iterator.hasNext()) {
            UUID shadowId = iterator.next();
            ShadowData data = SHADOWS.get(shadowId);
            if (data == null) {
                iterator.remove();
                continue;
            }
            ServerLevel level = server.getLevel(data.levelKey());
            if (level == null) {
                iterator.remove();
                SHADOWS.remove(shadowId);
                continue;
            }
            Entity entity = level.getEntity(shadowId);
            if (!(entity instanceof Mob mob) || !mob.isAlive() || !mob.getPersistentData().getBoolean(SHADOW_TAG)) {
                iterator.remove();
                SHADOWS.remove(shadowId);
            }
        }
        if (ids.isEmpty()) {
            SHADOWS_BY_OWNER.remove(ownerId);
        }
    }

    private static void updateShadow(Mob mob, ServerPlayer owner, ShadowCollectorItem.ShadowBehavior behavior) {
        mob.setPersistenceRequired();
        mob.setNoAi(false);
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && !isValidTarget(currentTarget, owner)) {
            mob.setTarget(null);
            currentTarget = null;
        }

        switch (behavior) {
            case PASSIVE -> handlePassive(mob, owner);
            case NEUTRAL -> handleNeutral(mob, owner);
            case HOSTILE -> handleHostile(mob, owner);
        }
    }

    private static void handlePassive(Mob mob, ServerPlayer owner) {
        mob.setAggressive(false);
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        followOwner(mob, owner, 1.0D);
    }

    private static void handleNeutral(Mob mob, ServerPlayer owner) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            Monster monster = findNearestMonster(mob, owner, 16.0D);
            if (monster != null) {
                mob.setTarget(monster);
                mob.setAggressive(true);
                return;
            }
        }
        mob.setAggressive(false);
        followOwner(mob, owner, 1.1D);
    }

    private static void handleHostile(Mob mob, ServerPlayer owner) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            LivingEntity nextTarget = findHostileTarget(mob, owner, 20.0D);
            if (nextTarget != null) {
                mob.setTarget(nextTarget);
                target = nextTarget;
            }
        }
        if (target != null) {
            mob.setAggressive(true);
        } else {
            mob.setAggressive(false);
            followOwner(mob, owner, 1.2D);
        }
    }

    private static void followOwner(Mob mob, ServerPlayer owner, double speed) {
        double distanceSqr = mob.distanceToSqr(owner);
        if (distanceSqr > 1024.0D) {
            mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            mob.getNavigation().stop();
            return;
        }
        if (distanceSqr > 36.0D) {
            mob.getNavigation().moveTo(owner, speed);
        } else if (distanceSqr > 9.0D) {
            mob.getNavigation().moveTo(owner, Math.max(speed * 0.75D, 0.6D));
        } else if (distanceSqr < 4.0D) {
            mob.getNavigation().stop();
        }
        mob.getLookControl().setLookAt(owner, 30.0F, 30.0F);
    }

    private static Monster findNearestMonster(Mob mob, ServerPlayer owner, double range) {
        AABB area = mob.getBoundingBox().inflate(range);
        List<Monster> monsters = mob.level().getEntitiesOfClass(Monster.class, area, candidate ->
                candidate.isAlive()
                        && candidate != mob
                        && !ShadowSummonManager.isShadowEntity(candidate)
                        && !candidate.isAlliedTo(owner));
        return monsters.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(owner)))
                .orElse(null);
    }

    private static LivingEntity findHostileTarget(Mob mob, ServerPlayer owner, double range) {
        AABB area = mob.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = mob.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != mob && isValidHostileTarget(entity, owner));
        return candidates.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(mob)))
                .orElse(null);
    }

    private static boolean isValidTarget(LivingEntity target, ServerPlayer owner) {
        if (!target.isAlive()) {
            return false;
        }
        if (target.getUUID().equals(owner.getUUID())) {
            return false;
        }
        if (target instanceof Mob shadowMob && shadowMob.getPersistentData().getBoolean(SHADOW_TAG)) {
            return false;
        }
        return !owner.isAlliedTo(target);
    }

    private static boolean isValidHostileTarget(LivingEntity target, ServerPlayer owner) {
        if (!isValidTarget(target, owner)) {
            return false;
        }
        return true;
    }

    private static void removeFromOwner(UUID ownerId, UUID shadowId) {
        Set<UUID> ids = SHADOWS_BY_OWNER.get(ownerId);
        if (ids != null) {
            ids.remove(shadowId);
            if (ids.isEmpty()) {
                SHADOWS_BY_OWNER.remove(ownerId);
            }
        }
    }

    private static void prepareShadowForCombat(Mob mob) {
        ensureAttackAttribute(mob);

        if (mob instanceof Monster) {
            return;
        }
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }
        if (!hasGoalOfType(mob.targetSelector, HurtByTargetGoal.class)) {
            mob.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinderMob).setAlertOthers());
        }
        if (mob instanceof Slime) {
            return;
        }

        if (!hasGoalOfType(mob.goalSelector, MeleeAttackGoal.class)) {
            mob.goalSelector.addGoal(2, new MeleeAttackGoal(pathfinderMob, 1.2D, true));
        }
    }

    private static boolean hasGoalOfType(GoalSelector selector, Class<?> goalClass) {
        for (WrappedGoal wrappedGoal : selector.getAvailableGoals()) {
            if (goalClass.isInstance(wrappedGoal.getGoal())) {
                return true;
            }
        }
        return false;
    }

    private static void ensureAttackAttribute(Mob mob) {
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && attack.getBaseValue() < 2.0D) {
            attack.setBaseValue(2.0D);
        }
    }

    private record ShadowData(ResourceKey<Level> levelKey, UUID ownerId) {
    }
}
