package Vfx.vfx.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class ShadowDotParticle extends TextureSheetParticle {
    private ShadowDotParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprite) {
        super(level, x, y, z, xd, yd, zd);
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 40 + this.random.nextInt(20);
        this.quadSize = 0.3F;
        this.rCol = 0.1F;
        this.gCol = 0.1F;
        this.bCol = 0.1F;
        this.setAlpha(0.85F);
        this.setSprite(sprite.get(0, 0));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            float progress = (float) this.age / (float) this.lifetime;
            progress = Math.min(progress, 1.0F);
            this.alpha = 0.85F * (1.0F - progress) + 0.2F;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            ShadowDotParticle particle = new ShadowDotParticle(level, x, y, z, 0.0D, 0.0D, 0.0D, this.sprite);
            return particle;
        }
    }
}
