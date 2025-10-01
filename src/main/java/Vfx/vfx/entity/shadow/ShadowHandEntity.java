package Vfx.vfx.entity.shadow;

import Vfx.vfx.entity.HandGrabEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.MovementEmission;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class ShadowHandEntity extends Entity {
    private static final String TAG_TICK_COUNT = "Age";
    private static final String TAG_GRASPED = "Grasped";
    private static final String TAG_CRUSHED = "Crushed";
    private static final String TAG_TARGET = "Target";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_MODE = "Mode";
    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID =
            SynchedEntityData.defineId(ShadowHandEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(ShadowHandEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(ShadowHandEntity.class, EntityDataSerializers.INT);
    private static final int APPEAR_TICKS = 20;
    private static final int GRASP_TICKS = 40;

    private boolean appliedGrasp;
    private boolean crushedTarget;
    private LivingEntity cachedTarget;
    private Entity cachedOwner;
    private ShadowHandMode mode = ShadowHandMode.CRUSH;
    @Nullable
    private HandGrabEntity grabEntity;

    @OnlyIn(Dist.CLIENT)
    private final AnimationState appearAnimationState = new AnimationState();
    @OnlyIn(Dist.CLIENT)
    private final AnimationState graspAnimationState = new AnimationState();

    public ShadowHandEntity(EntityType<? extends ShadowHandEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_UUID, Optional.empty());
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(MODE, ShadowHandMode.CRUSH.getId());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(MODE)) {
            this.mode = ShadowHandMode.byId(this.entityData.get(MODE));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID(TAG_TARGET)) {
            this.entityData.set(TARGET_UUID, Optional.of(tag.getUUID(TAG_TARGET)));
        }
        if (tag.hasUUID(TAG_OWNER)) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID(TAG_OWNER)));
        }
        this.tickCount = tag.getInt(TAG_TICK_COUNT);
        this.appliedGrasp = tag.getBoolean(TAG_GRASPED);
        this.crushedTarget = tag.getBoolean(TAG_CRUSHED);
        if (tag.contains(TAG_MODE)) {
            setMode(ShadowHandMode.byId(tag.getInt(TAG_MODE)));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        this.entityData.get(TARGET_UUID).ifPresent(uuid -> tag.putUUID(TAG_TARGET, uuid));
        this.entityData.get(OWNER_UUID).ifPresent(uuid -> tag.putUUID(TAG_OWNER, uuid));
        tag.putInt(TAG_TICK_COUNT, this.tickCount);
        tag.putBoolean(TAG_GRASPED, this.appliedGrasp);
        tag.putBoolean(TAG_CRUSHED, this.crushedTarget);
        tag.putInt(TAG_MODE, getMode().getId());
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (!level().isClientSide) {
                this.discard();
            }
            return;
        }

        if (!level().isClientSide) {
            if (this.grabEntity != null && !this.grabEntity.isAlive()) {
                this.grabEntity = null;
            }
            if (this.grabEntity == null) {
                Entity owner = getOwner();
                if (owner instanceof LivingEntity livingOwner && level() instanceof ServerLevel serverLevel) {
                    this.grabEntity = HandGrabEntity.findActive(serverLevel, livingOwner, target);
                }
            }
        }

        this.setPos(target.getX(), target.getY(), target.getZ());
        if (level().isClientSide) {
            updateClientAnimations();
            spawnClientParticles(target);
            return;
        }

        ShadowHandMode currentMode = getMode();
        if (!this.appliedGrasp && this.tickCount >= APPEAR_TICKS) {
            applyGrasp(target, currentMode);
            this.appliedGrasp = true;
        }

        if (!this.crushedTarget && this.tickCount >= APPEAR_TICKS + GRASP_TICKS) {
            if (currentMode == ShadowHandMode.THROW) {
                prepareThrowRelease(target);
            } else {
                crushTarget(target);
            }
            this.crushedTarget = true;
            this.discard();
        }
    }

    private void spawnClientParticles(LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < 4; i++) {
            double angle = (this.tickCount + i) * 0.6D;
            double radius = 0.6D;
            double offsetX = Mth.cos((float) angle) * radius;
            double offsetZ = Mth.sin((float) angle) * radius;
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    center.x + offsetX,
                    center.y + 0.2D,
                    center.z + offsetZ,
                    0.0D,
                    0.01D,
                    0.0D);
        }
    }

    private void applyGrasp(LivingEntity target, ShadowHandMode currentMode) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GRASP_TICKS + 20, 6, false, false, true));
        if (currentMode == ShadowHandMode.CRUSH) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, GRASP_TICKS + 20, 2, false, false, true));
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.hasImpulse = true;
        target.fallDistance = 0.0F;
    }

    private void crushTarget(LivingEntity target) {
        DamageSource source = createDamageSource();
        float damage = Math.max(target.getMaxHealth(), 1.0F) * 100.0F;
        target.hurt(source, damage);
        if (target.isAlive()) {
            target.kill();
        }
    }

    private void prepareThrowRelease(LivingEntity target) {
        target.hasImpulse = true;
        target.hurtMarked = true;
        target.fallDistance = 0.0F;
        if (level().isClientSide) {
            return;
        }
        if (this.grabEntity != null && this.grabEntity.isAlive()) {
            this.grabEntity.enablePlayerReleaseControl();
        } else if (level() instanceof ServerLevel serverLevel) {
            Entity owner = getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                HandGrabEntity found = HandGrabEntity.findActive(serverLevel, livingOwner, target);
                if (found != null) {
                    this.grabEntity = found;
                    found.enablePlayerReleaseControl();
                }
            }
        }
    }

    private DamageSource createDamageSource() {
        Entity owner = getOwner();
        if (owner instanceof ServerPlayer serverPlayer) {
            return level().damageSources().playerAttack(serverPlayer);
        }
        if (owner instanceof LivingEntity livingOwner) {
            return level().damageSources().mobAttack(livingOwner);
        }
        return level().damageSources().magic();
    }

    @Nullable
    private LivingEntity getTarget() {
        if (this.cachedTarget != null && this.cachedTarget.isAlive()) {
            return this.cachedTarget;
        }

        Optional<UUID> id = this.entityData.get(TARGET_UUID);
        if (id.isPresent()) {
            if (level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(id.get());
                if (entity instanceof LivingEntity livingEntity) {
                    this.cachedTarget = livingEntity;
                    return livingEntity;
                }
            } else {
                for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(8.0D))) {
                    if (living.getUUID().equals(id.get())) {
                        this.cachedTarget = living;
                        return living;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private Entity getOwner() {
        if (this.cachedOwner != null && this.cachedOwner.isAlive()) {
            return this.cachedOwner;
        }

        Optional<UUID> id = this.entityData.get(OWNER_UUID);
        if (id.isPresent()) {
            if (level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(id.get());
                if (entity != null) {
                    this.cachedOwner = entity;
                    return entity;
                }
            } else {
                for (Entity entity : level().getEntities(this, this.getBoundingBox().inflate(16.0D), e -> e.getUUID().equals(id.get()))) {
                    this.cachedOwner = entity;
                    return entity;
                }
            }
        }
        return null;
    }

    public void setTarget(LivingEntity target) {
        this.cachedTarget = target;
        this.entityData.set(TARGET_UUID, Optional.ofNullable(target != null ? target.getUUID() : null));
    }

    public void setOwner(@Nullable Entity owner) {
        this.cachedOwner = owner;
        this.entityData.set(OWNER_UUID, Optional.ofNullable(owner != null ? owner.getUUID() : null));
    }

    public void setMode(ShadowHandMode mode) {
        this.mode = mode;
        this.entityData.set(MODE, mode.getId());
    }

    public void setGrabEntity(@Nullable HandGrabEntity grabEntity) {
        this.grabEntity = grabEntity;
    }

    public ShadowHandMode getMode() {
        if (level().isClientSide) {
            this.mode = ShadowHandMode.byId(this.entityData.get(MODE));
        }
        return this.mode;
    }

    public boolean isTracking(LivingEntity potentialTarget) {
        Optional<UUID> id = this.entityData.get(TARGET_UUID);
        return id.isPresent() && id.get().equals(potentialTarget.getUUID());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @OnlyIn(Dist.CLIENT)
    private void updateClientAnimations() {
        this.appearAnimationState.animateWhen(this.tickCount < APPEAR_TICKS, this.tickCount);
        this.graspAnimationState.animateWhen(this.tickCount >= APPEAR_TICKS && this.tickCount < APPEAR_TICKS + GRASP_TICKS,
                Math.max(0, this.tickCount - APPEAR_TICKS));
    }

    @OnlyIn(Dist.CLIENT)
    public AnimationState getAppearAnimationState() {
        return this.appearAnimationState;
    }

    @OnlyIn(Dist.CLIENT)
    public AnimationState getGraspAnimationState() {
        return this.graspAnimationState;
    }

    public static boolean hasActiveHand(Level level, LivingEntity target) {
        return !level.getEntitiesOfClass(ShadowHandEntity.class, target.getBoundingBox().inflate(2.0D),
                entity -> entity.isTracking(target)).isEmpty();
    }

    public static int getAppearTicks() {
        return APPEAR_TICKS;
    }

    public static int getGraspTicks() {
        return GRASP_TICKS;
    }

    public static int getTotalAnimationTicks() {
        return APPEAR_TICKS + GRASP_TICKS;
    }
}
