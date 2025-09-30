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
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

        ShadowDomain domain = new ShadowDomain(level, owner.getUUID(), center, radius, durationTicks);
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
        private static final float RADIUS_PADDING = 1.0F;

        private final ServerLevel level;
        private final BlockPos center;
        private final UUID ownerId;
        private final int radius;
        private long expiryGameTime;
        private final int durationTicks;
        private final AABB bounds;
        private int ticksActive;
        private boolean expired;

        private ShadowDomain(ServerLevel level, UUID ownerId, BlockPos center, int radius, int durationTicks) {
            this.level = level;
            this.ownerId = ownerId;
            this.center = center;
            this.radius = Math.max(1, radius);
            this.expiryGameTime = level.getGameTime() + durationTicks;
            this.durationTicks = durationTicks;
            Vec3 min = new Vec3(center.getX() - radius, Math.max(level.getMinBuildHeight(), center.getY() - radius), center.getZ() - radius);
            Vec3 max = new Vec3(center.getX() + radius + 1, Math.min(level.getMaxBuildHeight(), center.getY() + radius + 1), center.getZ() + radius + 1);
            this.bounds = new AABB(min, max);
        }

        private void apply() {
            spawnDarknessCloud();
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
            double dx = pos.getX() + 0.5 - (center.getX() + 0.5);
            double dy = pos.getY() + 0.5 - (center.getY() + 0.5);
            double dz = pos.getZ() + 0.5 - (center.getZ() + 0.5);
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }

        private void spawnDomainTexture() {
            if (expired) {
                return;
            }

            int remainingLifetime = Math.max(durationTicks - ticksActive, 1);
            int textureLifetime = remainingLifetime <= TEXTURE_REFRESH_INTERVAL_TICKS
                    ? remainingLifetime
                    : Math.min(remainingLifetime, MAX_TEXTURE_LIFETIME_TICKS);
            float visualRadius = Math.max(radius + RADIUS_PADDING, 1.0F);
            ShadowDomainParticleOptions options = VfxParticles.domainOptions(visualRadius, textureLifetime);
            level.sendParticles(options, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        }

        private void applyCooldown() {
            Player owner = level.getPlayerByUUID(ownerId);
            if (owner != null) {
                owner.getCooldowns().addCooldown(Vfx.DOMAIN_OF_SHADOWS_RELIC.get(), durationTicks);
            }
        }

        private void spawnDarknessCloud() {
            AreaEffectCloud cloud = new AreaEffectCloud(level, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
            cloud.setRadius(Math.max(radius, 1));
            cloud.setDuration(1);
            cloud.setWaitTime(0);
            cloud.setRadiusPerTick(0);
            cloud.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Math.max(durationTicks, 1)));
            level.addFreshEntity(cloud);
        }

        private void applyDarknessEffect() {
            int effectDuration = Math.max(MIN_EFFECT_DURATION_TICKS, Math.min(durationTicks, MAX_EFFECT_DURATION_TICKS));
            effectDuration = Math.max(effectDuration, EFFECT_REFRESH_INTERVAL_TICKS * 3);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, bounds, p -> !p.isSpectator())) {
                if (!containsPosition(player.blockPosition())) {
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
