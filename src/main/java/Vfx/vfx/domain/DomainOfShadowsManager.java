package Vfx.vfx.domain;

import Vfx.vfx.Vfx;
import Vfx.vfx.VfxParticles;
import Vfx.vfx.particle.ShadowDomainParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Vfx.MODID)
public class DomainOfShadowsManager {
    private static final Map<ResourceKey<Level>, DomainOfShadowsManager> MANAGERS = new HashMap<>();
    private static int cooldownReductionTicks = 0;

    public static DomainOfShadowsManager get(ServerLevel level) {
        return MANAGERS.compute(level.dimension(), (dimension, existing) -> {
            if (existing == null || existing.level != level) {
                return new DomainOfShadowsManager(level);
            }
            return existing;
        });
    }

    public static int getCooldownReductionTicks() {
        return cooldownReductionTicks;
    }

    public static void setCooldownReductionTicks(int reductionTicks) {
        cooldownReductionTicks = Math.max(0, reductionTicks);
    }

    private final ServerLevel level;
    private final Map<UUID, ShadowDomain> activeDomains = new HashMap<>();

    private DomainOfShadowsManager(ServerLevel level) {
        this.level = level;
    }

    public void activateDomain(ServerPlayer owner, BlockPos center, int radius, int durationTicks) {
        deactivateDomain(owner, false);

        Vec3 origin = owner.position();
        ShadowDomain domain = new ShadowDomain(level, owner.getUUID(), origin, radius, durationTicks);
        domain.apply();
        activeDomains.put(owner.getUUID(), domain);
    }

    public boolean deactivateDomain(ServerPlayer owner) {
        return deactivateDomain(owner, true);
    }

    public boolean deactivateDomain(ServerPlayer owner, boolean applyCooldown) {
        ShadowDomain existing = activeDomains.remove(owner.getUUID());
        if (existing != null) {
            existing.expire(applyCooldown);
            return true;
        }
        return false;
    }

    public boolean hasActiveDomain(ServerPlayer owner) {
        return activeDomains.containsKey(owner.getUUID());
    }

    public boolean isInsideDomain(BlockPos pos) {
        for (ShadowDomain domain : activeDomains.values()) {
            if (domain.containsPosition(pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInsideAnyDomain(ServerLevel level, BlockPos pos) {
        DomainOfShadowsManager manager = MANAGERS.get(level.dimension());
        if (manager == null || manager.level != level) {
            return false;
        }
        return manager.isInsideDomain(pos);
    }

    private void tick() {
        Iterator<Map.Entry<UUID, ShadowDomain>> iterator = activeDomains.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ShadowDomain> entry = iterator.next();
            ShadowDomain domain = entry.getValue();
            if (domain.tickAndCheckExpired()) {
                domain.expire(true);
                iterator.remove();
            }
        }
    }

    private void clear() {
        activeDomains.values().forEach(domain -> domain.expire(false));
        activeDomains.clear();
    }

    @SubscribeEvent
    public static void handleServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (Iterator<Map.Entry<ResourceKey<Level>, DomainOfShadowsManager>> iterator = MANAGERS.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ResourceKey<Level>, DomainOfShadowsManager> entry = iterator.next();
            ServerLevel serverLevel = event.getServer().getLevel(entry.getKey());
            if (serverLevel == null) {
                iterator.remove();
                continue;
            }
            entry.getValue().tick();
        }
    }

    @SubscribeEvent
    public static void handleLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        DomainOfShadowsManager manager = MANAGERS.remove(serverLevel.dimension());
        if (manager != null && manager.level == serverLevel) {
            manager.clear();
        }
    }

    private static class ShadowDomain {
        private static final int TEXTURE_REFRESH_INTERVAL_TICKS = 5;
        private static final int MAX_TEXTURE_LIFETIME_TICKS = TEXTURE_REFRESH_INTERVAL_TICKS * 3;
        private static final int EFFECT_REFRESH_INTERVAL_TICKS = 10;
        private static final int MIN_EFFECT_DURATION_TICKS = 80;
        private static final int MAX_EFFECT_DURATION_TICKS = 200;
        private static final int STASIS_REFRESH_INTERVAL_TICKS = 4;
        private static final int STASIS_EFFECT_DURATION_TICKS = 10;
        private static final double POSITION_TOLERANCE = 0.75D;
        private static final double MIN_START_RADIUS = 0.75D;
        private static final double DOMAIN_HEIGHT = 3.0D;

        private final ServerLevel level;
        private final Vec3 center;
        private final UUID ownerId;
        private final double maxRadius;
        private final int durationTicks;
        private final int growthDurationTicks;
        private final AABB bounds;
        private long expiryGameTime;
        private int ticksActive;
        private boolean expired;

        private ShadowDomain(ServerLevel level, UUID ownerId, Vec3 center, int radius, int durationTicks) {
            this.level = level;
            this.ownerId = ownerId;
            this.center = center;
            this.maxRadius = Math.max(2.5D, radius);
            this.durationTicks = Math.max(1, durationTicks);
            this.growthDurationTicks = Math.max(1, Math.min(this.durationTicks / 2, 40));
            this.expiryGameTime = level.getGameTime() + this.durationTicks;
            double padding = Math.max(this.maxRadius, MIN_START_RADIUS) + POSITION_TOLERANCE;
            this.bounds = new AABB(
                    center.x - padding,
                    center.y - POSITION_TOLERANCE,
                    center.z - padding,
                    center.x + padding,
                    center.y + DOMAIN_HEIGHT + POSITION_TOLERANCE,
                    center.z + padding
            );
        }

        private void apply() {
            spawnDomainTexture();
            applyDarknessEffect();
            applyStasisEffect();
        }

        private boolean tickAndCheckExpired() {
            ticksActive++;
            if (expired || level.getGameTime() >= expiryGameTime) {
                return true;
            }

            if (ticksActive % TEXTURE_REFRESH_INTERVAL_TICKS == 0) {
                spawnDomainTexture();
            }

            if (ticksActive % EFFECT_REFRESH_INTERVAL_TICKS == 0) {
                applyDarknessEffect();
            }

            if (ticksActive % STASIS_REFRESH_INTERVAL_TICKS == 0) {
                applyStasisEffect();
            }
            return false;
        }

        private void expire(boolean shouldApplyCooldown) {
            if (expired) {
                if (shouldApplyCooldown) {
                    applyCooldown();
                }
                return;
            }
            expired = true;
            expiryGameTime = level.getGameTime();
            if (shouldApplyCooldown) {
                applyCooldown();
            }
        }

        private boolean containsPosition(BlockPos pos) {
            return containsPoint(Vec3.atCenterOf(pos));
        }

        private boolean containsPoint(Vec3 point) {
            double horizontalX = point.x - center.x;
            double horizontalZ = point.z - center.z;
            double radius = getCurrentRadius();
            double allowed = radius + POSITION_TOLERANCE;
            if (horizontalX * horizontalX + horizontalZ * horizontalZ > allowed * allowed) {
                return false;
            }

            double verticalOffset = point.y - center.y;
            return verticalOffset >= -POSITION_TOLERANCE && verticalOffset <= DOMAIN_HEIGHT + POSITION_TOLERANCE;
        }

        private double getCurrentRadius() {
            return getRadiusAtTick(ticksActive);
        }

        private double getRadiusAtTick(int tick) {
            if (growthDurationTicks <= 0) {
                return Math.max(MIN_START_RADIUS, maxRadius);
            }
            double progress = Math.min(1.0D, Math.max(0.0D, (double) tick / (double) growthDurationTicks));
            double target = Mth.lerp(progress, MIN_START_RADIUS, maxRadius);
            return Math.max(MIN_START_RADIUS, target);
        }

        private void spawnDomainTexture() {
            if (expired) {
                return;
            }

            int remainingLifetime = Math.max(durationTicks - ticksActive, 1);
            int textureLifetime = remainingLifetime <= TEXTURE_REFRESH_INTERVAL_TICKS
                    ? remainingLifetime
                    : Math.min(remainingLifetime, MAX_TEXTURE_LIFETIME_TICKS);
            double currentRadius = getCurrentRadius();
            double projectedRadius = getRadiusAtTick(ticksActive + textureLifetime);
            ShadowDomainParticleOptions options = VfxParticles.domainOptions(
                    (float) currentRadius,
                    (float) projectedRadius,
                    (float) DOMAIN_HEIGHT,
                    textureLifetime
            );
            level.sendParticles(options, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        private void applyCooldown() {
            Player owner = level.getPlayerByUUID(ownerId);
            if (owner != null) {
                int cooldown = Math.max(0, durationTicks - DomainOfShadowsManager.getCooldownReductionTicks());
                owner.getCooldowns().addCooldown(Vfx.DOMAIN_OF_SHADOWS_RELIC.get(), cooldown);
            }
        }

        private void applyDarknessEffect() {
            double radius = getCurrentRadius();
            double allowed = radius + 0.5D;
            double allowedSq = allowed * allowed;
            int effectDuration = Math.max(MIN_EFFECT_DURATION_TICKS, Math.min(durationTicks, MAX_EFFECT_DURATION_TICKS));
            effectDuration = Math.max(effectDuration, EFFECT_REFRESH_INTERVAL_TICKS * 3);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, bounds, p -> !p.isSpectator())) {
                Vec3 playerFeet = player.position();
                double offsetX = playerFeet.x - center.x;
                double offsetZ = playerFeet.z - center.z;
                if (offsetX * offsetX + offsetZ * offsetZ > allowedSq) {
                    continue;
                }
                MobEffectInstance existing = player.getEffect(MobEffects.DARKNESS);
                if (existing == null || existing.getDuration() <= 20) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, effectDuration, 0, false, false, true));
                }
            }
        }

        private void applyStasisEffect() {
            double radius = getCurrentRadius();
            double allowedSq = radius * radius;
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds, e -> !e.isSpectator())) {
                if (entity.getUUID().equals(ownerId)) {
                    continue;
                }
                Vec3 position = entity.position();
                double offsetX = position.x - center.x;
                double offsetZ = position.z - center.z;
                if (offsetX * offsetX + offsetZ * offsetZ > allowedSq) {
                    continue;
                }
                entity.setDeltaMovement(Vec3.ZERO);
                entity.hurtMarked = true;
                entity.fallDistance = 0.0F;
                MobEffectInstance existing = entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                if (existing == null || existing.getAmplifier() < 10 || existing.getDuration() <= 5) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STASIS_EFFECT_DURATION_TICKS, 10, false, false, true));
                }
            }
        }
    }

}
