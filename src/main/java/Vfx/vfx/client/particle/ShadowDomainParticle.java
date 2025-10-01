package Vfx.vfx.client.particle;

import Vfx.vfx.particle.ShadowDomainParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class ShadowDomainParticle extends TextureSheetParticle {
    private static final float MIN_ALPHA = 0.3F;
    private static final float BASE_RADIUS = 0.5F;
    private static final int SEGMENTS = 24;
    private static final float INNER_SHADE = 0.15F;

    private final SpriteSet sprites;
    private final float initialRadius;
    private final float targetRadius;
    private final float maxRadius;

    private ShadowDomainParticle(ClientLevel level, double x, double y, double z,
                                 ShadowDomainParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.initialRadius = Math.max(options.initialRadius(), BASE_RADIUS);
        this.targetRadius = Math.max(options.targetRadius(), this.initialRadius);
        this.maxRadius = Math.max(this.initialRadius, this.targetRadius);
        this.lifetime = Math.max(options.lifetime(), 1);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
        this.setColor(0.02F, 0.02F, 0.02F);
        this.setAlpha(0.85F);

        double minX = x - this.maxRadius;
        double minZ = z - this.maxRadius;
        double maxX = x + this.maxRadius;
        double maxZ = z + this.maxRadius;
        double maxY = y + options.height();
        this.setBoundingBox(new AABB(minX, y, minZ, maxX, maxY, maxZ));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && this.lifetime > 0) {
            float fade = 1.0F - ((float) this.age / (float) this.lifetime);
            this.setAlpha(Mth.clamp(0.25F + fade * 0.75F, MIN_ALPHA, 1.0F));
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (this.alpha <= 0.0F) {
            return;
        }

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        double x = Mth.lerp(partialTicks, this.xo, this.x) - camX;
        double y = Mth.lerp(partialTicks, this.yo, this.y) - camY + 0.01D;
        double z = Mth.lerp(partialTicks, this.zo, this.z) - camZ;

        float radius = getCurrentRadius(partialTicks);
        renderDisc(buffer, x, y, z, radius, partialTicks);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    private float getCurrentRadius(float partialTicks) {
        float progress = ((float) this.age + partialTicks) / (float) this.lifetime;
        progress = Mth.clamp(progress, 0.0F, 1.0F);
        return Mth.lerp(progress, this.initialRadius, this.targetRadius);
    }

    private void renderDisc(VertexConsumer buffer, double centerX, double centerY, double centerZ, float radius, float partialTicks) {
        int light = this.getLightColor(partialTicks);
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        float uCenter = (u0 + u1) * 0.5F;
        float vCenter = (v0 + v1) * 0.5F;
        float uScale = (u1 - u0) * 0.5F;
        float vScale = (v1 - v0) * 0.5F;
        float innerRadius = radius * INNER_SHADE;

        for (int i = 0; i < SEGMENTS; ++i) {
            float angle0 = (float) i / (float) SEGMENTS * Mth.TWO_PI;
            float angle1 = (float) (i + 1) / (float) SEGMENTS * Mth.TWO_PI;
            double outerX0 = centerX + Mth.cos(angle0) * radius;
            double outerZ0 = centerZ + Mth.sin(angle0) * radius;
            double outerX1 = centerX + Mth.cos(angle1) * radius;
            double outerZ1 = centerZ + Mth.sin(angle1) * radius;
            double innerX0 = centerX + Mth.cos(angle0) * innerRadius;
            double innerZ0 = centerZ + Mth.sin(angle0) * innerRadius;
            double innerX1 = centerX + Mth.cos(angle1) * innerRadius;
            double innerZ1 = centerZ + Mth.sin(angle1) * innerRadius;

            float uOuter0 = uCenter + Mth.cos(angle0) * uScale;
            float vOuter0 = vCenter + Mth.sin(angle0) * vScale;
            float uOuter1 = uCenter + Mth.cos(angle1) * uScale;
            float vOuter1 = vCenter + Mth.sin(angle1) * vScale;
            float uInner0 = uCenter + Mth.cos(angle0) * uScale * INNER_SHADE;
            float vInner0 = vCenter + Mth.sin(angle0) * vScale * INNER_SHADE;
            float uInner1 = uCenter + Mth.cos(angle1) * uScale * INNER_SHADE;
            float vInner1 = vCenter + Mth.sin(angle1) * vScale * INNER_SHADE;

            renderQuad(buffer, centerY, innerX0, innerZ0, innerX1, innerZ1, outerX1, outerZ1, outerX0, outerZ0,
                    uInner0, vInner0, uInner1, vInner1, uOuter1, vOuter1, uOuter0, vOuter0, light);
        }
    }

    private void renderQuad(VertexConsumer buffer, double y,
                             double innerX0, double innerZ0,
                             double innerX1, double innerZ1,
                             double outerX1, double outerZ1,
                             double outerX0, double outerZ0,
                             float uInner0, float vInner0,
                             float uInner1, float vInner1,
                             float uOuter1, float vOuter1,
                             float uOuter0, float vOuter0,
                             int light) {
        float r = this.rCol;
        float g = this.gCol;
        float b = this.bCol;
        float a = this.alpha;

        buffer.vertex(innerX0, y, innerZ0).uv(uInner0, vInner0).color(r, g, b, a).uv2(light).endVertex();
        buffer.vertex(innerX1, y, innerZ1).uv(uInner1, vInner1).color(r, g, b, a).uv2(light).endVertex();
        buffer.vertex(outerX1, y, outerZ1).uv(uOuter1, vOuter1).color(r, g, b, a).uv2(light).endVertex();
        buffer.vertex(outerX0, y, outerZ0).uv(uOuter0, vOuter0).color(r, g, b, a).uv2(light).endVertex();
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
