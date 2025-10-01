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

    public static DomainOfShadowsManager get(ServerLevel level) {
        return MANAGERS.compute(level.dimension(), (dimension, existing) -> {
            if (existing == null || existing.level != level) {
                return new DomainOfShadowsManager(level);
            }
            return existing;
        });
    }

    private final ServerLevel level;
    private final Map<UUID, ShadowDomain> activeDomains = new HashMap<>();

    private DomainOfShadowsManager(ServerLevel level) {
        this.level = level;
    }

    public void activateDomain(ServerPlayer owner, BlockPos center, int radius, int durationTicks) {
        deactivateDomain(owner, false);

        Vec3 origin = owner.getEyePosition();
        Vec3 direction = owner.getLookAngle();
        ShadowDomain domain = new ShadowDomain(level, owner.getUUID(), origin, direction, radius, durationTicks);
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
        private static final int TEXTURE_REFRESH_INTERVAL_TICKS = 20;
        private static final int MAX_TEXTURE_LIFETIME_TICKS = TEXTURE_REFRESH_INTERVAL_TICKS * 2;
        private static final int EFFECT_REFRESH_INTERVAL_TICKS = 10;
        private static final int MIN_EFFECT_DURATION_TICKS = 80;
        private static final int MAX_EFFECT_DURATION_TICKS = 200;
        private static final double POSITION_TOLERANCE = 0.5D;

        private final ServerLevel level;
        private final Vec3 origin;
        private final Vec3 direction;
        private final Vec3 endPoint;
        private final UUID ownerId;
        private final double beamHalfWidth;
        private final double length;
        private long expiryGameTime;
        private final int durationTicks;
        private final AABB bounds;
        private int ticksActive;
        private boolean expired;

        private ShadowDomain(ServerLevel level, UUID ownerId, Vec3 origin, Vec3 direction, int length, int durationTicks) {
            this.level = level;
            this.ownerId = ownerId;
            Vec3 normalizedDirection = direction.lengthSqr() < 1.0E-4D
                    ? new Vec3(0.0D, 0.0D, 1.0D)
                    : direction.normalize();
            this.origin = origin;
            this.direction = normalizedDirection;
            this.length = Math.max(1, length);
            this.beamHalfWidth = Mth.clamp(this.length / 8.0D, 1.5D, 4.0D);
            this.endPoint = this.origin.add(this.direction.scale(this.length));
            this.expiryGameTime = level.getGameTime() + durationTicks;
            this.durationTicks = durationTicks;
            double minX = Math.min(origin.x, endPoint.x);
            double minY = Math.min(origin.y, endPoint.y);
            double minZ = Math.min(origin.z, endPoint.z);
            double maxX = Math.max(origin.x, endPoint.x);
            double maxY = Math.max(origin.y, endPoint.y);
            double maxZ = Math.max(origin.z, endPoint.z);
            this.bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ)
                    .inflate(this.beamHalfWidth, this.beamHalfWidth + 1.5D, this.beamHalfWidth);
        }

        private void apply() {
            spawnDomainTexture();
            applyDarknessEffect();
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
            Vec3 toPoint = point.subtract(origin);
            double projection = toPoint.dot(direction);
            if (projection < -POSITION_TOLERANCE || projection > length + POSITION_TOLERANCE) {
                return false;
            }

            double clampedProjection = Mth.clamp(projection, 0.0D, length);
            Vec3 closestPoint = origin.add(direction.scale(clampedProjection));
            double allowed = beamHalfWidth + 0.5D;
            double distanceSq = point.distanceToSqr(closestPoint);
            return distanceSq <= allowed * allowed;
        }

        private void spawnDomainTexture() {
            if (expired) {
                return;
            }

            int remainingLifetime = Math.max(durationTicks - ticksActive, 1);
            int textureLifetime = remainingLifetime <= TEXTURE_REFRESH_INTERVAL_TICKS
                    ? remainingLifetime
                    : Math.min(remainingLifetime, MAX_TEXTURE_LIFETIME_TICKS);
            float visualRadius = (float) Math.max(beamHalfWidth, 1.0D);
            ShadowDomainParticleOptions options = VfxParticles.domainOptions(visualRadius, (float) length, direction, textureLifetime);
            level.sendParticles(options, origin.x, origin.y, origin.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        private void applyCooldown() {
            Player owner = level.getPlayerByUUID(ownerId);
            if (owner != null) {
                owner.getCooldowns().addCooldown(Vfx.DOMAIN_OF_SHADOWS_RELIC.get(), durationTicks);
            }
        }

        private void applyDarknessEffect() {
            int effectDuration = Math.max(MIN_EFFECT_DURATION_TICKS, Math.min(durationTicks, MAX_EFFECT_DURATION_TICKS));
            effectDuration = Math.max(effectDuration, EFFECT_REFRESH_INTERVAL_TICKS * 3);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, bounds, p -> !p.isSpectator())) {
                Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
                if (!containsPoint(playerCenter)) {
                    continue;
                }
                MobEffectInstance existing = player.getEffect(MobEffects.DARKNESS);
                if (existing == null || existing.getDuration() <= 20) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, effectDuration, 0, false, false, true));
                }
            }
        }
    }

}
