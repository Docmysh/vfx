package Vfx.vfx.client.particle;

import Vfx.vfx.VfxParticles;
import Vfx.vfx.particle.ShadowDomainParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;

public class ShadowDomainParticle extends NoRenderParticle {
    private static final float MIN_RADIUS = 1.0F;
    private static final float DOT_SPACING = 0.85F;
    private static final float RANDOM_OFFSET = 0.12F;

    private final float radius;

    private ShadowDomainParticle(ClientLevel level, double x, double y, double z, ShadowDomainParticleOptions options) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.radius = Math.max(options.radius(), MIN_RADIUS);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = Math.max(options.lifetime(), 1);
        spawnDome();
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    private void spawnDome() {
        double centerX = this.x;
        double centerY = this.y;
        double centerZ = this.z;
        float radius = this.radius;

        int verticalLayers = Math.max(4, Mth.ceil((radius * 2.0F) / DOT_SPACING));
        for (int layer = 0; layer <= verticalLayers; ++layer) {
            float progress = (float) layer / (float) verticalLayers;
            float theta = progress * Mth.HALF_PI;
            float sinTheta = Mth.sin(theta);
            float cosTheta = Mth.cos(theta);
            float ringRadius = radius * sinTheta;
            double ringYOffset = radius * cosTheta;

            spawnRing(centerX, centerY + ringYOffset, centerZ, ringRadius);

            if (layer != 0 && layer != verticalLayers) {
                spawnRing(centerX, centerY - ringYOffset, centerZ, ringRadius);
            }
        }
    }

    private void spawnRing(double centerX, double centerY, double centerZ, float ringRadius) {
        if (ringRadius <= 0.05F) {
            this.level.addParticle(VfxParticles.SHADOW_DOT.get(), centerX, centerY, centerZ, 0.0D, 0.0D, 0.0D);
            return;
        }

        float circumference = (float) (Mth.TWO_PI * ringRadius);
        int points = Math.max(8, Mth.ceil(circumference / DOT_SPACING));
        for (int i = 0; i < points; ++i) {
            float baseAngle = (float) i / (float) points * Mth.TWO_PI;
            float jitter = (this.random.nextFloat() - 0.5F) * (Mth.TWO_PI / points) * RANDOM_OFFSET;
            float angle = baseAngle + jitter;
            double offsetX = ringRadius * Mth.cos(angle);
            double offsetZ = ringRadius * Mth.sin(angle);
            double jitterY = (this.random.nextFloat() - 0.5D) * RANDOM_OFFSET;
            this.level.addParticle(
                    VfxParticles.SHADOW_DOT.get(),
                    centerX + offsetX,
                    centerY + jitterY,
                    centerZ + offsetZ,
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
