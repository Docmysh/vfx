package Vfx.vfx.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class ShadowDotParticle extends TextureSheetParticle {
    private static final float BASE_SIZE = 0.18F;
    private static final float SIZE_VARIATION = 0.07F;
    private static final float MAX_ALPHA = 0.85F;
    private static final float MIN_ALPHA = 0.2F;
    private static final int DOT_LIFETIME_TICKS = 26;

    private final SpriteSet sprites;

    private ShadowDotParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = DOT_LIFETIME_TICKS + this.random.nextInt(6);
        this.quadSize = BASE_SIZE + this.random.nextFloat() * SIZE_VARIATION;
        this.setAlpha(MAX_ALPHA);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && this.lifetime > 0) {
            float progress = (float) this.age / (float) this.lifetime;
            progress = Mth.clamp(progress, 0.0F, 1.0F);
            float alpha = MAX_ALPHA * (1.0F - progress) + MIN_ALPHA * progress;
            this.setAlpha(alpha);
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new ShadowDotParticle(level, x, y, z, this.sprites);
        }
    }
}
