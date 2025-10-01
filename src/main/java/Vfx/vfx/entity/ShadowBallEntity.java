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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShadowBallEntity extends Entity implements GeoEntity {
    private static final String TAG_LIFE_TICKS = "LifeTicks";
    public static final int MAX_LIFE_TICKS = 200;

    private static final EntityDataAccessor<Integer> LIFE_TICKS =
            SynchedEntityData.defineId(ShadowBallEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lifeTicks;

    public ShadowBallEntity(EntityType<? extends ShadowBallEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static ShadowBallEntity summon(ServerLevel level, Vec3 position) {
        return summon(level, position, 0.0F, 0.0F);
    }

    public static ShadowBallEntity summon(ServerLevel level, Vec3 position, float yRot, float xRot) {
        ShadowBallEntity entity = VfxEntities.SHADOW_BALL.get().create(level);
        if (entity == null) {
            return null;
        }

        entity.moveTo(position.x, position.y, position.z, yRot, xRot);
        level.addFreshEntity(entity);
        return entity;
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

        if (!level().isClientSide) {
            int ticks = getLifeTicks() + 1;
            if (ticks >= MAX_LIFE_TICKS) {
                discard();
            } else {
                setLifeTicks(ticks);
            }
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
}
