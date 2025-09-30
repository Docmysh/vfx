package Vfx.vfx.client.particle;

import Vfx.vfx.particle.ShadowDomainParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class ShadowDomainParticle extends TextureSheetParticle {
    private static final float BASE_ALPHA = 0.65F;
    private static final float MIN_ALPHA = 0.2F;
    private static final float SIZE_PADDING = 1.5F;

    private final SpriteSet sprites;

    private ShadowDomainParticle(ClientLevel level, double x, double y, double z, SpriteSet sprite, ShadowDomainParticleOptions options) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = Math.max(options.lifetime(), 1);
        this.quadSize = Math.max(options.radius() * 2.0F + SIZE_PADDING, 0.5F);
        this.sprites = sprite;
        this.setSpriteFromAge(this.sprites);
        this.setAlpha(BASE_ALPHA);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && this.lifetime > 0) {
            float progress = (float) this.age / (float) this.lifetime;
            progress = Math.min(Math.max(progress, 0.0F), 1.0F);
            float alpha = BASE_ALPHA * (1.0F - progress) + MIN_ALPHA * progress;
            this.setAlpha(alpha);
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<ShadowDomainParticleOptions> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(ShadowDomainParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new ShadowDomainParticle(level, x, y, z, this.sprite, options);
        }
    }
}
