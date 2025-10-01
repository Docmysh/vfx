package Vfx.vfx.entity;

import Vfx.vfx.VfxEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class SingularityCoreEntity extends Entity implements GeoEntity {
    private static final String TAG_LIFE_TICKS = "LifeTicks";

    private static final EntityDataAccessor<Integer> LIFE_TICKS =
            SynchedEntityData.defineId(SingularityCoreEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lifeTicks;

    public SingularityCoreEntity(EntityType<? extends SingularityCoreEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static SingularityCoreEntity summon(ServerLevel level, Vec3 position) {
        SingularityCoreEntity entity = VfxEntities.SINGULARITY_CORE.get().create(level);
        if (entity == null) {
            return null;
        }

        entity.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        level.addFreshEntity(entity);
        entity.playSummonEffects(level);
        return entity;
    }

    private void playSummonEffects(ServerLevel serverLevel) {
        serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY(), getZ(), 40, 0.5, 0.5, 0.5, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFE_TICKS, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LIFE_TICKS.equals(key)) {
            this.lifeTicks = this.entityData.get(LIFE_TICKS);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TAG_LIFE_TICKS)) {
            setLifeTicks(tag.getInt(TAG_LIFE_TICKS));
        } else {
            setLifeTicks(0);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(TAG_LIFE_TICKS, getLifeTicks());
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }

        int ticks = getLifeTicks() + 1;
        setLifeTicks(ticks);

        if (ticks >= MAX_LIFE_TICKS) {
            explode();
            discard();
            return;
        }

        if (ticks % 2 == 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 8, 0.4, 0.4, 0.4, 0.02);
            serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY(), getZ(), 10, 0.6, 0.6, 0.6, 0.01);
        }

        pullNearbyEntities();
    }

    private void spawnClientParticles() {
        for (int i = 0; i < 4; i++) {
            double dx = (random.nextDouble() - 0.5) * 0.6;
            double dy = (random.nextDouble() - 0.5) * 0.6;
            double dz = (random.nextDouble() - 0.5) * 0.6;
            level().addParticle(ParticleTypes.PORTAL, getX(), getY(), getZ(), dx, dy, dz);
        }
    }

    private void pullNearbyEntities() {
        Level level = level();
        Vec3 center = position();
        AABB area = getBoundingBox().inflate(PULL_RADIUS);
        List<Entity> targets = level.getEntities(this, area, this::canAffect);
        for (Entity target : targets) {
            Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            Vec3 pullVector = center.subtract(targetCenter);
            double distanceSqr = pullVector.lengthSqr();
            if (distanceSqr < 1.0E-6) {
                continue;
            }
            double distance = Math.max(0.1, Math.sqrt(distanceSqr));
            double strengthFactor = 1.0 - Mth.clamp(distance / PULL_RADIUS, 0.0, 1.0);
            double tickScale = (getLifeTicks() >= DETONATION_WARNING_TICKS) ? 1.5 : 1.0;
            double force = (BASE_PULL_FORCE + strengthFactor * ADDITIONAL_PULL_FORCE) * tickScale;
            Vec3 motion = pullVector.normalize().scale(force);

            Vec3 deltaMovement = target.getDeltaMovement().add(motion);

            if (target instanceof Projectile) {
                deltaMovement = deltaMovement.scale(0.9).add(motion.scale(0.6));
            }

            if (target instanceof ItemEntity item) {
                item.setPickUpDelay(0);
            }

            target.setDeltaMovement(deltaMovement);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }
    }

    private boolean canAffect(Entity entity) {
        if (!entity.isAlive() || entity == this || entity.isSpectator()) {
            return false;
        }

        return !entity.getType().equals(VfxEntities.SINGULARITY_CORE.get());
    }

    private void explode() {
        level().explode(this, getX(), getY(), getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void setLifeTicks(int ticks) {
        this.lifeTicks = ticks;
        this.entityData.set(LIFE_TICKS, ticks);
    }

    public int getLifeTicks() {
        return this.lifeTicks;
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spin", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.model.new"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static final int MAX_LIFE_TICKS = 100;
    private static final int DETONATION_WARNING_TICKS = MAX_LIFE_TICKS - 20;
    private static final double PULL_RADIUS = 8.0D;
    private static final double BASE_PULL_FORCE = 0.05D;
    private static final double ADDITIONAL_PULL_FORCE = 0.25D;
    private static final float EXPLOSION_POWER = 4.0F;
}
