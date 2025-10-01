package Vfx.vfx.entity;

import Vfx.vfx.VfxEntities;
import Vfx.vfx.entity.shadow.ShadowHandMode;
import net.minecraft.client.multiplayer.ClientLevel;
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
import net.minecraft.world.entity.LivingEntity;
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

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class HandGrabEntity extends Entity implements GeoEntity {
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_TARGET = "Target";
    private static final String TAG_LIFE = "Life";
    private static final String TAG_STRENGTH = "Strength";
    private static final String TAG_MODE = "Mode";
    private static final String TAG_HOLD_DISTANCE = "HoldDistance";
    private static final String TAG_AWAITING_RELEASE = "AwaitingRelease";
    private static final String TAG_RELEASE_REQUESTED = "ReleaseRequested";

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(HandGrabEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID =
            SynchedEntityData.defineId(HandGrabEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> LIFE_TICKS =
            SynchedEntityData.defineId(HandGrabEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(HandGrabEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(HandGrabEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(HandGrabEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private int life;
    private float strength = 1.0F;
    private ShadowHandMode mode = ShadowHandMode.CRUSH;
    private double holdDistance;
    private Vec3 lastHoldPosition = Vec3.ZERO;
    private Vec3 previousHoldPosition = Vec3.ZERO;
    private boolean hasHoldPosition;
    private boolean awaitingManualRelease;
    private boolean releaseRequested;

    private int ownerId = -1;
    private int targetId = -1;

    @Nullable
    private Entity cachedOwner;
    @Nullable
    private LivingEntity cachedTarget;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HandGrabEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Nullable
    public static HandGrabEntity spawn(ServerLevel level, LivingEntity owner, LivingEntity target,
                                       float strength, int duration) {
        return spawn(level, owner, target, strength, duration, ShadowHandMode.CRUSH);
    }

    public static HandGrabEntity spawn(ServerLevel level, LivingEntity owner, LivingEntity target,
                                       float strength, int duration, ShadowHandMode mode) {
        HandGrabEntity entity = VfxEntities.HAND_GRAB.get().create(level);
        if (entity == null) {
            return null;
        }

        entity.setOwner(owner);
        entity.setTarget(target);
        entity.setStrength(strength);
        entity.setLifetime(duration);
        entity.setMode(mode);

        AABB boundingBox = target.getBoundingBox();
        Vec3 center = new Vec3(
                (boundingBox.minX + boundingBox.maxX) / 2.0D,
                boundingBox.minY + target.getBbHeight() * 0.6D,
                (boundingBox.minZ + boundingBox.maxZ) / 2.0D
        );
        entity.initializeHoldParameters(owner, center);
        entity.moveTo(center);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(TARGET_UUID, Optional.empty());
        this.entityData.define(LIFE_TICKS, 0);
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(MODE, ShadowHandMode.CRUSH.getId());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(OWNER_UUID)) {
            this.ownerUuid = this.entityData.get(OWNER_UUID).orElse(null);
        } else if (key.equals(TARGET_UUID)) {
            this.targetUuid = this.entityData.get(TARGET_UUID).orElse(null);
        } else if (key.equals(LIFE_TICKS)) {
            this.life = this.entityData.get(LIFE_TICKS);
        } else if (key.equals(OWNER_ID)) {
            this.ownerId = this.entityData.get(OWNER_ID);
        } else if (key.equals(TARGET_ID)) {
            this.targetId = this.entityData.get(TARGET_ID);
        } else if (key.equals(MODE)) {
            this.mode = ShadowHandMode.byId(this.entityData.get(MODE));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID(TAG_OWNER)) {
            this.ownerUuid = tag.getUUID(TAG_OWNER);
            this.entityData.set(OWNER_UUID, Optional.of(this.ownerUuid));
        }
        if (tag.hasUUID(TAG_TARGET)) {
            this.targetUuid = tag.getUUID(TAG_TARGET);
            this.entityData.set(TARGET_UUID, Optional.of(this.targetUuid));
        }
        this.life = tag.getInt(TAG_LIFE);
        this.strength = tag.getFloat(TAG_STRENGTH);
        this.entityData.set(LIFE_TICKS, this.life);
        this.entityData.set(OWNER_ID, -1);
        this.entityData.set(TARGET_ID, -1);
        this.ownerId = -1;
        this.targetId = -1;
        if (tag.contains(TAG_MODE)) {
            setMode(ShadowHandMode.byId(tag.getInt(TAG_MODE)));
        }
        if (tag.contains(TAG_HOLD_DISTANCE)) {
            this.holdDistance = tag.getDouble(TAG_HOLD_DISTANCE);
        }
        if (tag.contains(TAG_AWAITING_RELEASE)) {
            this.awaitingManualRelease = tag.getBoolean(TAG_AWAITING_RELEASE);
        } else {
            this.awaitingManualRelease = false;
        }
        this.releaseRequested = tag.getBoolean(TAG_RELEASE_REQUESTED);
        this.hasHoldPosition = false;
        this.previousHoldPosition = Vec3.ZERO;
        this.lastHoldPosition = Vec3.ZERO;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID(TAG_OWNER, this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID(TAG_TARGET, this.targetUuid);
        }
        tag.putInt(TAG_LIFE, this.life);
        tag.putFloat(TAG_STRENGTH, this.strength);
        tag.putInt(TAG_MODE, getMode().getId());
        tag.putDouble(TAG_HOLD_DISTANCE, this.holdDistance);
        tag.putBoolean(TAG_AWAITING_RELEASE, this.awaitingManualRelease);
        tag.putBoolean(TAG_RELEASE_REQUESTED, this.releaseRequested);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        Vec3 center;
        if (!level().isClientSide && getMode() == ShadowHandMode.THROW) {
            center = updateHoldPosition(target);
        } else if (level().isClientSide && getMode() == ShadowHandMode.THROW) {
            center = calculateDynamicHoldPosition(target, false);
        } else {
            center = getAnchorPosition(target);
            this.hasHoldPosition = false;
        }
        setPos(center);

        if (!level().isClientSide) {
            boolean throwMode = getMode() == ShadowHandMode.THROW;
            if (throwMode) {
                holdTarget(target, center);
                if (!this.awaitingManualRelease) {
                    if (this.life <= 0) {
                        enableManualReleasePhase();
                    } else {
                        this.life--;
                        this.entityData.set(LIFE_TICKS, Math.max(this.life, 0));
                    }
                } else {
                    this.entityData.set(LIFE_TICKS, 1);
                    if (this.releaseRequested) {
                        throwTarget(target);
                        discard();
                        return;
                    }
                }
            } else {
                if (this.life <= 0) {
                    discard();
                    return;
                }
                this.life--;
                this.entityData.set(LIFE_TICKS, Math.max(this.life, 0));
                target.setDeltaMovement(target.getDeltaMovement().scale(0.2D));
                if (tickCount % 10 == 0) {
                    target.hurt(createDamageSource(), 1.0F * this.strength);
                }
            }
        } else {
            this.life = this.entityData.get(LIFE_TICKS);
        }
    }

    private Vec3 getAnchorPosition(LivingEntity target) {
        AABB boundingBox = target.getBoundingBox();
        return new Vec3(
                (boundingBox.minX + boundingBox.maxX) / 2.0D,
                boundingBox.minY + target.getBbHeight() * 0.6D,
                (boundingBox.minZ + boundingBox.maxZ) / 2.0D
        );
    }

    @Nullable
    private LivingEntity getTarget() {
        if (this.targetUuid == null) {
            return null;
        }
        if (this.cachedTarget != null && this.cachedTarget.isAlive() && this.cachedTarget.getUUID().equals(this.targetUuid)) {
            return this.cachedTarget;
        }

        Entity entity = null;
        if (level() instanceof ServerLevel serverLevel) {
            entity = serverLevel.getEntity(this.targetUuid);
            if (entity != null) {
                this.targetId = entity.getId();
                this.entityData.set(TARGET_ID, this.targetId);
            }
        } else if (level() instanceof ClientLevel clientLevel) {
            if (this.targetId != -1) {
                entity = clientLevel.getEntity(this.targetId);
            }
        }

        if (entity instanceof LivingEntity livingEntity) {
            this.cachedTarget = livingEntity;
            return livingEntity;
        }
        return null;
    }

    @Nullable
    private Entity getOwnerEntity() {
        if (this.ownerUuid == null) {
            return null;
        }
        if (this.cachedOwner != null && this.cachedOwner.isAlive() && this.cachedOwner.getUUID().equals(this.ownerUuid)) {
            return this.cachedOwner;
        }

        Entity entity = null;
        if (level() instanceof ServerLevel serverLevel) {
            entity = serverLevel.getEntity(this.ownerUuid);
            if (entity != null) {
                this.ownerId = entity.getId();
                this.entityData.set(OWNER_ID, this.ownerId);
            }
        } else if (level() instanceof ClientLevel clientLevel) {
            if (this.ownerId != -1) {
                entity = clientLevel.getEntity(this.ownerId);
            }
        }

        if (entity != null) {
            this.cachedOwner = entity;
        }
        return entity;
    }

    private DamageSource createDamageSource() {
        Entity owner = getOwnerEntity();
        if (owner instanceof LivingEntity livingEntity) {
            return level().damageSources().indirectMagic(this, livingEntity);
        }
        return level().damageSources().magic();
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.cachedOwner = owner;
        this.entityData.set(OWNER_UUID, Optional.of(this.ownerUuid));
        this.ownerId = owner.getId();
        this.entityData.set(OWNER_ID, this.ownerId);
    }

    private void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.cachedTarget = target;
        this.entityData.set(TARGET_UUID, Optional.of(this.targetUuid));
        this.targetId = target.getId();
        this.entityData.set(TARGET_ID, this.targetId);
    }

    private void setLifetime(int duration) {
        this.life = duration;
        this.entityData.set(LIFE_TICKS, duration);
    }

    private void setStrength(float strength) {
        this.strength = Mth.clamp(strength, 0.0F, Float.MAX_VALUE);
    }

    private void setMode(ShadowHandMode mode) {
        this.mode = mode;
        this.entityData.set(MODE, mode.getId());
    }

    public ShadowHandMode getMode() {
        if (level().isClientSide) {
            this.mode = ShadowHandMode.byId(this.entityData.get(MODE));
        }
        return this.mode;
    }

    private void holdTarget(LivingEntity target, Vec3 center) {
        target.teleportTo(center.x, center.y, center.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = true;
        target.hurtMarked = true;
        target.fallDistance = 0.0F;
    }

    private void throwTarget(LivingEntity target) {
        Vec3 velocity = getThrowVelocity();
        target.setDeltaMovement(velocity);
        target.hasImpulse = true;
        target.hurtMarked = true;
        target.fallDistance = 0.0F;
    }

    private Vec3 getThrowVelocity() {
        Vec3 swing = this.lastHoldPosition.subtract(this.previousHoldPosition);
        double swingLength = swing.length();
        if (swingLength > 0.05D) {
            Vec3 direction = swing.normalize();
            double speed = Mth.clamp(swingLength * 4.0D, 1.2D, 4.5D);
            double vertical = Math.max(direction.y * speed + 0.4D, 0.3D);
            return new Vec3(direction.x * speed, vertical, direction.z * speed);
        }

        Entity owner = getOwnerEntity();
        if (owner instanceof LivingEntity livingEntity) {
            Vec3 lookAngle = livingEntity.getLookAngle();
            if (lookAngle.lengthSqr() > 1.0E-4D) {
                Vec3 normalized = lookAngle.normalize();
                double speed = 1.6D;
                return new Vec3(normalized.x * speed,
                        Math.max(normalized.y * speed + 0.6D, 0.7D),
                        normalized.z * speed);
            }
        }
        return new Vec3(0.0D, 0.7D, 0.0D);
    }

    public int getLifeTicks() {
        return this.entityData.get(LIFE_TICKS);
    }

    private void initializeHoldParameters(LivingEntity owner, Vec3 center) {
        this.awaitingManualRelease = false;
        this.releaseRequested = false;
        if (this.mode != ShadowHandMode.THROW) {
            this.holdDistance = 0.0D;
            this.previousHoldPosition = Vec3.ZERO;
            this.lastHoldPosition = Vec3.ZERO;
            this.hasHoldPosition = false;
            return;
        }
        Vec3 eyePosition = owner.getEyePosition();
        double distance = eyePosition.distanceTo(center);
        this.holdDistance = Mth.clamp(distance, 1.5D, 7.0D);
        this.previousHoldPosition = center;
        this.lastHoldPosition = center;
        this.hasHoldPosition = true;
    }

    private void enableManualReleasePhase() {
        if (this.mode != ShadowHandMode.THROW) {
            return;
        }
        this.awaitingManualRelease = true;
        this.releaseRequested = false;
        this.life = Math.max(this.life, 1);
        this.entityData.set(LIFE_TICKS, Math.max(this.life, 1));
    }

    public void requestManualRelease() {
        if (this.mode == ShadowHandMode.THROW) {
            this.releaseRequested = true;
        }
    }

    public boolean isAwaitingManualRelease() {
        return this.awaitingManualRelease;
    }

    public boolean isOwnedBy(@Nullable LivingEntity owner) {
        return owner != null && this.ownerUuid != null && owner.getUUID().equals(this.ownerUuid);
    }

    public boolean isHoldingTarget(@Nullable LivingEntity target) {
        return target != null && this.targetUuid != null && target.getUUID().equals(this.targetUuid);
    }

    public void enablePlayerReleaseControl() {
        enableManualReleasePhase();
    }

    @Nullable
    public static HandGrabEntity findActive(ServerLevel level, LivingEntity owner, LivingEntity target) {
        return level.getEntitiesOfClass(HandGrabEntity.class, target.getBoundingBox().inflate(3.0D),
                entity -> entity.isOwnedBy(owner) && entity.isHoldingTarget(target)).stream().findFirst().orElse(null);
    }

    @Nullable
    public static HandGrabEntity findAwaitingThrow(ServerLevel level, LivingEntity owner) {
        return level.getEntitiesOfClass(HandGrabEntity.class, owner.getBoundingBox().inflate(32.0D),
                entity -> entity.isOwnedBy(owner) && entity.getMode() == ShadowHandMode.THROW)
                .stream()
                .filter(HandGrabEntity::isAwaitingManualRelease)
                .findFirst()
                .orElse(null);
    }

    private Vec3 updateHoldPosition(LivingEntity target) {
        return calculateDynamicHoldPosition(target, true);
    }

    private Vec3 calculateDynamicHoldPosition(LivingEntity target, boolean updateHistory) {
        Entity ownerEntity = getOwnerEntity();
        if (ownerEntity instanceof LivingEntity owner) {
            Vec3 eyePosition = owner.getEyePosition();
            Vec3 lookAngle = owner.getLookAngle();
            if (lookAngle.lengthSqr() < 1.0E-4D) {
                lookAngle = owner.getForward();
            }
            if (lookAngle.lengthSqr() < 1.0E-4D) {
                lookAngle = new Vec3(0.0D, 0.0D, 1.0D);
            }

            if (this.holdDistance <= 0.0D) {
                Vec3 currentCenter = getAnchorPosition(target);
                this.holdDistance = Mth.clamp(eyePosition.distanceTo(currentCenter), 1.5D, 7.0D);
            }

            Vec3 normalized = lookAngle.normalize();
            Vec3 desiredCenter = eyePosition.add(normalized.scale(this.holdDistance));

            if (updateHistory) {
                if (!this.hasHoldPosition) {
                    this.previousHoldPosition = desiredCenter;
                    this.lastHoldPosition = desiredCenter;
                    this.hasHoldPosition = true;
                } else {
                    this.previousHoldPosition = this.lastHoldPosition;
                    this.lastHoldPosition = desiredCenter;
                }
            }

            return desiredCenter;
        }

        this.hasHoldPosition = false;
        return getAnchorPosition(target);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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
    protected void doWaterSplashEffect() {
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "grab", 0, state -> {
            if (state.getAnimatable().tickCount < 6) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("appear"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("squeeze"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
