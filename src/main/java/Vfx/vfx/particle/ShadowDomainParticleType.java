package Vfx.vfx.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public class ShadowDomainParticleType extends ParticleType<ShadowDomainParticleOptions> {
    public ShadowDomainParticleType() {
        super(false, ShadowDomainParticleOptions.DESERIALIZER);
    }

    @Override
    public Codec<ShadowDomainParticleOptions> codec() {
        return ShadowDomainParticleOptions.CODEC;
    }
}
