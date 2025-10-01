package Vfx.vfx;

import Vfx.vfx.particle.ShadowDomainParticleOptions;
import Vfx.vfx.particle.ShadowDomainParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VfxParticles {
    private VfxParticles() {
    }

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Vfx.MODID);

    public static final RegistryObject<ShadowDomainParticleType> SHADOW_DOMAIN =
            PARTICLES.register("shadow_domain", ShadowDomainParticleType::new);

    public static void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }

    public static ShadowDomainParticleOptions domainOptions(float currentRadius, float projectedRadius, float height, int lifetime) {
        float safeCurrent = Math.max(currentRadius, 0.1F);
        float safeProjected = Math.max(projectedRadius, safeCurrent);
        float safeHeight = Math.max(height, 0.1F);
        int safeLifetime = Math.max(lifetime, 1);
        return new ShadowDomainParticleOptions(safeCurrent, safeProjected, safeHeight, safeLifetime);
    }
}
