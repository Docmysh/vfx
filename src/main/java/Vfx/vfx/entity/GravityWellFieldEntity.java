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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GravityWellFieldEntity extends Entity {
    private static final String TAG_LIFE = "Life";
    private static final String TAG_DURATION = "Duration";
    private static final String TAG_OWNER = "Owner";

    private static final EntityDataAccessor<Integer> LIFE_TICKS =
            SynchedEntityData.defineId(GravityWellFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(GravityWellFieldEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final float RADIUS = 6.0F;
    private static final double DOWN_FORCE = 0.08D;
    private static final double PROJECTILE_DOWN_FORCE = 0.3D;
    private static final int DURATION_DEFAULT = 20 * 12;

    private int duration = DURATION_DEFAULT;
    @Nullable
    private UUID ownerUuid;

    public GravityWellFieldEntity(EntityType<? extends GravityWellFieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static GravityWellFieldEntity spawn(ServerLevel level, Vec3 position, int duration, @Nullable LivingEntity owner) {
        GravityWellFieldEntity entity = VfxEntities.GRAVITY_WELL.get().create(level);
        if (entity == null) {
            return null;
        }

        entity.setPos(position.x, position.y, position.z);
        entity.setDuration(duration);
        if (owner != null) {
            entity.setOwner(owner);
        }
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFE_TICKS, 0);
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();

        int life = this.entityData.get(LIFE_TICKS);
        if (!this.level().isClientSide) {
            applyGravityEffects();
        } else {
            spawnClientParticles();
        }

        life++;
        this.entityData.set(LIFE_TICKS, life);
        if (life >= this.duration) {
            discard();
        }
    }

    private void applyGravityEffects() {
        AABB bounds = new AABB(
                this.getX() - RADIUS,
                this.getY() - 1.5D,
                this.getZ() - RADIUS,
                this.getX() + RADIUS,
                this.getY() + 3.5D,
                this.getZ() + RADIUS
        );

        List<Entity> entities = this.level().getEntities(this, bounds, entity -> entity.isAlive() && entity != this);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                applyToLiving(living);
            } else if (entity instanceof Projectile projectile) {
                applyToProjectile(projectile);
            } else {
                applyToGeneric(entity);
            }
        }
    }

    private void applyToLiving(LivingEntity living) {
        Vec3 motion = living.getDeltaMovement();
        double horizontalScale = 0.45D;
        double newX = motion.x * horizontalScale;
        double newZ = motion.z * horizontalScale;
        double newY = motion.y;

        if (motion.y > 0.0D) {
            newY = Math.min(0.0D, motion.y * 0.2D - DOWN_FORCE);
        } else {
            newY -= DOWN_FORCE;
        }

        living.setDeltaMovement(newX, newY, newZ);
        living.hasImpulse = true;
        living.hurtMarked = true;
        living.setJumping(false);
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false, true));
    }

    private void applyToProjectile(Projectile projectile) {
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 adjusted = new Vec3(motion.x * 0.8D, motion.y - PROJECTILE_DOWN_FORCE, motion.z * 0.8D);
        projectile.setDeltaMovement(adjusted);
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
    }

    private void applyToGeneric(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        Vec3 adjusted = new Vec3(motion.x * 0.7D, motion.y - DOWN_FORCE, motion.z * 0.7D);
        entity.setDeltaMovement(adjusted);
        entity.hasImpulse = true;
        entity.hurtMarked = true;
    }

    private void spawnClientParticles() {
        if (this.level().random.nextInt(4) != 0) {
            return;
        }
        double angle = this.level().random.nextDouble() * (Math.PI * 2);
        double distance = RADIUS * Math.sqrt(this.level().random.nextDouble());
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;
        double offsetY = this.level().random.nextDouble() * 2.0D - 0.5D;
        this.level().addParticle(
                ParticleTypes.PORTAL,
                this.getX() + offsetX,
                this.getY() + offsetY,
                this.getZ() + offsetZ,
                0.0D,
                -0.15D,
                0.0D
        );
    }

    public void setOwner(LivingEntity owner) {
        UUID uuid = owner.getUUID();
        this.ownerUuid = uuid;
        this.entityData.set(OWNER_UUID, Optional.of(uuid));
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public void setDuration(int duration) {
        this.duration = Mth.clamp(duration, 20, 20 * 60);
    }

    public int getDuration() {
        return this.duration;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(TAG_LIFE, this.entityData.get(LIFE_TICKS));
        tag.putInt(TAG_DURATION, this.duration);
        if (this.ownerUuid != null) {
            tag.putUUID(TAG_OWNER, this.ownerUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(LIFE_TICKS, tag.getInt(TAG_LIFE));
        if (tag.contains(TAG_DURATION)) {
            setDuration(tag.getInt(TAG_DURATION));
        } else {
            this.duration = DURATION_DEFAULT;
        }
        if (tag.hasUUID(TAG_OWNER)) {
            this.ownerUuid = tag.getUUID(TAG_OWNER);
            this.entityData.set(OWNER_UUID, Optional.of(this.ownerUuid));
        } else {
            this.ownerUuid = null;
            this.entityData.set(OWNER_UUID, Optional.empty());
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
