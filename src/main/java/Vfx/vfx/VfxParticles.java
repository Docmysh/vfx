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

    public static ShadowDomainParticleOptions domainOptions(float radius, int lifetime) {
        return new ShadowDomainParticleOptions(radius, lifetime);
    }
}
