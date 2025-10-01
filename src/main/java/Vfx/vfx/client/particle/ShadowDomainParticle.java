package Vfx.vfx.client.particle;

import Vfx.vfx.VfxParticles;
import Vfx.vfx.particle.ShadowDomainParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ShadowDomainParticle extends NoRenderParticle {
    private static final float MIN_RADIUS = 0.5F;
    private static final float MIN_LENGTH = 1.0F;
    private static final float DOT_SPACING = 0.85F;
    private static final float RANDOM_OFFSET = 0.12F;

    private final float radius;
    private final float length;
    private final Vec3 direction;
    private final Vec3 right;
    private final Vec3 up;

    private ShadowDomainParticle(ClientLevel level, double x, double y, double z, ShadowDomainParticleOptions options) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.radius = Math.max(options.radius(), MIN_RADIUS);
        this.length = Math.max(options.length(), MIN_LENGTH);
        Vec3 normalizedDirection = new Vec3(options.directionX(), options.directionY(), options.directionZ());
        if (normalizedDirection.lengthSqr() < 1.0E-4D) {
            normalizedDirection = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            normalizedDirection = normalizedDirection.normalize();
        }
        this.direction = normalizedDirection;
        Vec3 reference = Math.abs(this.direction.y) < 0.99D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 computedRight = this.direction.cross(reference);
        if (computedRight.lengthSqr() < 1.0E-4D) {
            reference = new Vec3(0.0D, 0.0D, 1.0D);
            computedRight = this.direction.cross(reference);
        }
        this.right = computedRight.normalize();
        Vec3 computedUp = this.right.cross(this.direction);
        if (computedUp.lengthSqr() < 1.0E-4D) {
            computedUp = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            computedUp = computedUp.normalize();
        }
        this.up = computedUp;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = Math.max(options.lifetime(), 1);
        spawnBeam();
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    private void spawnBeam() {
        Vec3 start = new Vec3(this.x, this.y, this.z);
        int segments = Math.max(3, Mth.ceil(this.length / DOT_SPACING));
        for (int segment = 0; segment <= segments; ++segment) {
            float progress = (float) segment / (float) segments;
            Vec3 center = start.add(this.direction.scale(progress * this.length));
            spawnRing(center, this.radius);
        }
    }

    private void spawnRing(Vec3 center, float ringRadius) {
        if (ringRadius <= 0.05F) {
            this.level.addParticle(VfxParticles.SHADOW_DOT.get(), center.x, center.y, center.z, 0.0D, 0.0D, 0.0D);
            return;
        }

        float circumference = (float) (Mth.TWO_PI * ringRadius);
        int points = Math.max(8, Mth.ceil(circumference / DOT_SPACING));
        for (int i = 0; i < points; ++i) {
            float baseAngle = (float) i / (float) points * Mth.TWO_PI;
            float jitter = (this.random.nextFloat() - 0.5F) * (Mth.TWO_PI / points) * RANDOM_OFFSET;
            float angle = baseAngle + jitter;
            double cos = Mth.cos(angle);
            double sin = Mth.sin(angle);
            Vec3 radialOffset = this.right.scale(ringRadius * cos).add(this.up.scale(ringRadius * sin));
            double jitterAlong = (this.random.nextFloat() - 0.5D) * RANDOM_OFFSET;
            Vec3 jitterVector = this.direction.scale(jitterAlong);
            Vec3 position = center.add(radialOffset).add(jitterVector);
            this.level.addParticle(
                    VfxParticles.SHADOW_DOT.get(),
                    position.x,
                    position.y,
                    position.z,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    public static class Provider implements ParticleProvider<ShadowDomainParticleOptions> {
        public Provider(SpriteSet sprite) {
        }

        @Override
        public Particle createParticle(ShadowDomainParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new ShadowDomainParticle(level, x, y, z, options);
        }
    }
}
