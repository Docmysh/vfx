package Vfx.vfx.entity;

import Vfx.vfx.VfxEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SphereOfDoomEntity extends Entity implements GeoEntity {
    private static final String TAG_LIFE_TICKS = "LifeTicks";
    private static final String TAG_DIRECTION_X = "DirectionX";
    private static final String TAG_DIRECTION_Y = "DirectionY";
    private static final String TAG_DIRECTION_Z = "DirectionZ";

    private static final EntityDataAccessor<Integer> LIFE_TICKS =
            SynchedEntityData.defineId(SphereOfDoomEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> DIRECTION_X =
            SynchedEntityData.defineId(SphereOfDoomEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Y =
            SynchedEntityData.defineId(SphereOfDoomEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Z =
            SynchedEntityData.defineId(SphereOfDoomEntity.class, EntityDataSerializers.FLOAT);

    private static final int DELAY_TICKS = 200;
    private static final int ACTIVE_TICKS = 120;
    private static final float MOVE_SPEED = 1.2F;
    private static final double DESTRUCTION_RADIUS = 1.75D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private Vec3 travelDirection = Vec3.ZERO;
    private int lifeTicks;

    public SphereOfDoomEntity(EntityType<? extends SphereOfDoomEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static SphereOfDoomEntity spawn(ServerLevel level, Vec3 position, Vec3 direction) {
        SphereOfDoomEntity entity = VfxEntities.SPHERE_OF_DOOM.get().create(level);
        if (entity == null) {
            return null;
        }

        entity.setTravelDirection(direction);
        entity.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFE_TICKS, 0);
        this.entityData.define(DIRECTION_X, 0.0F);
        this.entityData.define(DIRECTION_Y, 0.0F);
        this.entityData.define(DIRECTION_Z, 0.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LIFE_TICKS.equals(key)) {
            this.lifeTicks = this.entityData.get(LIFE_TICKS);
        } else if (DIRECTION_X.equals(key) || DIRECTION_Y.equals(key) || DIRECTION_Z.equals(key)) {
            updateTravelDirectionFromSyncedData();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setLifeTicks(tag.getInt(TAG_LIFE_TICKS));
        double x = tag.getDouble(TAG_DIRECTION_X);
        double y = tag.getDouble(TAG_DIRECTION_Y);
        double z = tag.getDouble(TAG_DIRECTION_Z);
        setTravelDirection(new Vec3(x, y, z));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(TAG_LIFE_TICKS, getLifeTicks());
        tag.putDouble(TAG_DIRECTION_X, this.travelDirection.x);
        tag.putDouble(TAG_DIRECTION_Y, this.travelDirection.y);
        tag.putDouble(TAG_DIRECTION_Z, this.travelDirection.z);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            int ticks = getLifeTicks() + 1;
            setLifeTicks(ticks);

            if (ticks >= DELAY_TICKS + ACTIVE_TICKS) {
                discard();
                return;
            }
        }

        if (isActive()) {
            Vec3 movement = this.travelDirection.scale(MOVE_SPEED);
            setDeltaMovement(movement);
            setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
            if (!level().isClientSide) {
                destroyBlocksInPath();
            }
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private void setLifeTicks(int ticks) {
        this.lifeTicks = ticks;
        this.entityData.set(LIFE_TICKS, ticks);
    }

    public int getLifeTicks() {
        return this.lifeTicks;
    }

    private boolean isActive() {
        return getLifeTicks() >= DELAY_TICKS && getLifeTicks() < DELAY_TICKS + ACTIVE_TICKS;
    }

    private void setTravelDirection(Vec3 direction) {
        Vec3 normalized = direction.normalize();
        if (normalized.lengthSqr() <= 0.0D) {
            normalized = Vec3.ZERO;
        }

        this.travelDirection = normalized;

        if (!level().isClientSide) {
            this.entityData.set(DIRECTION_X, (float) normalized.x);
            this.entityData.set(DIRECTION_Y, (float) normalized.y);
            this.entityData.set(DIRECTION_Z, (float) normalized.z);
        }
    }

    private void destroyBlocksInPath() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB area = getBoundingBox().inflate(DESTRUCTION_RADIUS);
        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.isAir()) {
                return;
            }

            if (state.getDestroySpeed(serverLevel, pos) < 0.0F) {
                return;
            }

            serverLevel.destroyBlock(pos, false);
        });
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
        controllers.add(new AnimationController<>(this, "main", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("energyball.model.new"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private void updateTravelDirectionFromSyncedData() {
        Vec3 direction = new Vec3(this.entityData.get(DIRECTION_X), this.entityData.get(DIRECTION_Y),
                this.entityData.get(DIRECTION_Z));
        if (direction.lengthSqr() <= 0.0D) {
            this.travelDirection = Vec3.ZERO;
        } else {
            this.travelDirection = direction.normalize();
        }
    }
}
