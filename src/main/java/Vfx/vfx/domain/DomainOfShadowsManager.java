package Vfx.vfx.domain;

import Vfx.vfx.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private final List<ShadowDomain> activeDomains = new ArrayList<>();

    private DomainOfShadowsManager(ServerLevel level) {
        this.level = level;
    }

    public void activateDomain(BlockPos center, int radius, int durationTicks) {
        ShadowDomain domain = new ShadowDomain(level, center, radius, durationTicks);
        domain.apply();
        activeDomains.add(domain);
    }

    public boolean isInsideDomain(BlockPos pos) {
        for (ShadowDomain domain : activeDomains) {
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
        Iterator<ShadowDomain> iterator = activeDomains.iterator();
        while (iterator.hasNext()) {
            ShadowDomain domain = iterator.next();
            if (domain.tickAndCheckExpired()) {
                iterator.remove();
            }
        }
    }

    private void clear() {
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
        private static final double MIN_PARTICLE_STEP_RADIANS = Math.PI / 120.0;
        private static final double MAX_PARTICLE_STEP_RADIANS = Math.PI / 8.0;
        private static final double TARGET_ARC_LENGTH = 0.9;
        private static final int PARTICLE_SPAWN_INTERVAL_TICKS = 2;
        private static final double EQUATOR_BAND_MULTIPLIER = 1.5;
        private static final double EQUATOR_STEP_MULTIPLIER = 0.5;
        private static final double MIN_OUTER_RING_STEP_RADIANS = Math.PI / 160.0;
        private static final double LOOP_EPSILON = 1e-6;

        private final ServerLevel level;
        private final BlockPos center;
        private final int radius;
        private final long expiryGameTime;
        private final int durationTicks;
        private final AABB bounds;
        private int ticksActive;
        private final List<Vec3> particlePositions;

        private ShadowDomain(ServerLevel level, BlockPos center, int radius, int durationTicks) {
            this.level = level;
            this.center = center;
            this.radius = Math.max(1, radius);
            this.expiryGameTime = level.getGameTime() + durationTicks;
            this.durationTicks = durationTicks;
            Vec3 min = new Vec3(center.getX() - radius, Math.max(level.getMinBuildHeight(), center.getY() - radius), center.getZ() - radius);
            Vec3 max = new Vec3(center.getX() + radius + 1, Math.min(level.getMaxBuildHeight(), center.getY() + radius + 1), center.getZ() + radius + 1);
            this.bounds = new AABB(min, max);
            this.particlePositions = buildParticlePositions();
        }

        private void apply() {
            spawnDarknessCloud();
            spawnParticleDome();
            applyDarknessEffect();
        }

        private boolean tickAndCheckExpired() {
            ticksActive++;
            if (level.getGameTime() >= expiryGameTime) {
                return true;
            }

            if (ticksActive % PARTICLE_SPAWN_INTERVAL_TICKS == 0) {
                spawnParticleDome();
            }

            applyDarknessEffect();
            return false;
        }

        private boolean containsPosition(BlockPos pos) {
            double dx = pos.getX() + 0.5 - (center.getX() + 0.5);
            double dy = pos.getY() + 0.5 - (center.getY() + 0.5);
            double dz = pos.getZ() + 0.5 - (center.getZ() + 0.5);
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }

        private void spawnParticleDome() {
            for (Vec3 position : particlePositions) {
                level.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        private List<Vec3> buildParticlePositions() {
            List<Vec3> positions = new ArrayList<>();
            double originX = center.getX() + 0.5;
            double originY = center.getY() + 0.5;
            double originZ = center.getZ() + 0.5;

            int clampedRadius = Math.max(radius, 1);
            double angularStep = TARGET_ARC_LENGTH / clampedRadius;
            angularStep = Mth.clamp(angularStep, MIN_PARTICLE_STEP_RADIANS, MAX_PARTICLE_STEP_RADIANS);

            for (double theta = 0; theta <= Math.PI + LOOP_EPSILON; theta += angularStep) {
                double sinTheta = Math.sin(theta);
                double cosTheta = Math.cos(theta);
                double ringStep = angularStep;
                if (Math.abs(theta - Math.PI / 2.0) <= angularStep * EQUATOR_BAND_MULTIPLIER) {
                    ringStep = Math.max(MIN_OUTER_RING_STEP_RADIANS, angularStep * EQUATOR_STEP_MULTIPLIER);
                }

                for (double phi = 0; phi < Math.PI * 2 - LOOP_EPSILON; phi += ringStep) {
                    double cosPhi = Math.cos(phi);
                    double sinPhi = Math.sin(phi);
                    double x = originX + clampedRadius * sinTheta * cosPhi;
                    double y = originY + clampedRadius * cosTheta;
                    double z = originZ + clampedRadius * sinTheta * sinPhi;
                    positions.add(new Vec3(x, y, z));
                }
            }

            addOuterRing(positions, originX, originY, originZ, clampedRadius, angularStep);
            return positions;
        }

        private void addOuterRing(List<Vec3> positions, double originX, double originY, double originZ, int clampedRadius, double baseStep) {
            double outerStep = Math.max(MIN_OUTER_RING_STEP_RADIANS, baseStep * EQUATOR_STEP_MULTIPLIER);
            for (double phi = 0; phi < Math.PI * 2 - LOOP_EPSILON; phi += outerStep) {
                double cosPhi = Math.cos(phi);
                double sinPhi = Math.sin(phi);
                double x = originX + clampedRadius * cosPhi;
                double z = originZ + clampedRadius * sinPhi;
                positions.add(new Vec3(x, originY, z));
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
            int effectDuration = Math.max(20, Math.min(durationTicks, 60));
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, bounds, p -> !p.isSpectator())) {
                if (!containsPosition(player.blockPosition())) {
                    continue;
                }
                MobEffectInstance existing = player.getEffect(MobEffects.DARKNESS);
                if (existing == null || existing.getDuration() <= effectDuration / 2) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, effectDuration, 0, false, false, true));
                }
            }
        }
    }
}
