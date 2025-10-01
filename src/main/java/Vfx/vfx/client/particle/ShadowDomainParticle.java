package Vfx.vfx.client.particle;

import Vfx.vfx.particle.ShadowDomainParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ShadowDomainParticle extends TextureSheetParticle {
    private static final float MIN_RADIUS = 0.5F;
    private static final float MIN_LENGTH = 1.0F;
    private static final float INNER_SCALE = 0.55F;
    private static final float DIAGONAL_SCALE = 0.75F;
    private static final float PULSE_SPEED = 0.12F;
    private static final float MIN_ALPHA = 0.25F;

    private final SpriteSet sprites;

    private final float radius;
    private final float length;
    private final Vec3 direction;
    private final Vec3 right;
    private final Vec3 up;
    private final Vec3[] beamAxes;

    private ShadowDomainParticle(ClientLevel level, double x, double y, double z,
                                 ShadowDomainParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
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
        this.beamAxes = new Vec3[] {
                this.right,
                this.up,
                this.right.add(this.up).normalize(),
                this.right.subtract(this.up).normalize()
        };
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = Math.max(options.lifetime(), 1);
        this.setSpriteFromAge(this.sprites);
        this.setColor(0.05F, 0.0F, 0.08F);
        this.setAlpha(0.9F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && this.lifetime > 0) {
            float progress = (float) this.age / (float) this.lifetime;
            float fade = 1.0F - progress;
            float pulse = 0.85F + 0.15F * Mth.sin((this.age + 0.5F) * PULSE_SPEED * Mth.TWO_PI);
            this.setAlpha(Mth.clamp(fade * pulse, MIN_ALPHA, 1.0F));
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (this.alpha <= 0.0F) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        Vec3 start = this.getPosition(partialTicks).subtract(cameraPos);
        Vec3 end = start.add(this.direction.scale(this.length));
        int light = this.getLightColor(partialTicks);
        float baseRadius = getCurrentRadius(partialTicks);
        float innerRadius = baseRadius * INNER_SCALE;
        float diagonalRadius = baseRadius * DIAGONAL_SCALE;
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        for (Vec3 axis : this.beamAxes) {
            float radiusScale = axis == this.right || axis == this.up ? baseRadius : diagonalRadius;
            renderStrip(buffer, start, end, axis.scale(radiusScale), u0, u1, v0, v1, light);
        }

        for (Vec3 axis : this.beamAxes) {
            renderStrip(buffer, start, end, axis.scale(innerRadius), u0, u1, v0, v1, light);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private float getCurrentRadius(float partialTicks) {
        float progress = ((float) this.age + partialTicks) / (float) this.lifetime;
        progress = Mth.clamp(progress, 0.0F, 1.0F);
        float pulse = 0.9F + 0.1F * Mth.sin((this.age + partialTicks) * PULSE_SPEED * Mth.TWO_PI);
        return this.radius * (0.85F + (1.0F - progress) * 0.15F) * pulse;
    }

    private void renderStrip(VertexConsumer buffer, Vec3 start, Vec3 end, Vec3 offset,
                              float u0, float u1, float v0, float v1, int light) {
        if (offset.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 offsetScaled = offset;
        Vec3 startA = start.subtract(offsetScaled);
        Vec3 startB = start.add(offsetScaled);
        Vec3 endB = end.add(offsetScaled);
        Vec3 endA = end.subtract(offsetScaled);

        float r = this.rCol;
        float g = this.gCol;
        float b = this.bCol;
        float a = this.alpha;

        buffer.vertex(startA.x, startA.y, startA.z)
                .uv(u1, v0)
                .color(r, g, b, a)
                .uv2(light)
                .endVertex();
        buffer.vertex(startB.x, startB.y, startB.z)
                .uv(u0, v0)
                .color(r, g, b, a)
                .uv2(light)
                .endVertex();
        buffer.vertex(endB.x, endB.y, endB.z)
                .uv(u0, v1)
                .color(r, g, b, a)
                .uv2(light)
                .endVertex();
        buffer.vertex(endA.x, endA.y, endA.z)
                .uv(u1, v1)
                .color(r, g, b, a)
                .uv2(light)
                .endVertex();
    }

    public static class Provider implements ParticleProvider<ShadowDomainParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprite) {
            this.sprites = sprite;
        }

        @Override
        public Particle createParticle(ShadowDomainParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new ShadowDomainParticle(level, x, y, z, options, this.sprites);
        }
    }
}
