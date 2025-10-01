package Vfx.vfx;

import Vfx.vfx.particle.ShadowDomainParticleOptions;
import Vfx.vfx.particle.ShadowDomainParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.phys.Vec3;
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

    public static ShadowDomainParticleOptions domainOptions(float radius, float length, Vec3 direction, int lifetime) {
        Vec3 normalized = direction;
        if (normalized.lengthSqr() < 1.0E-4D) {
            normalized = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            normalized = normalized.normalize();
        }
        return new ShadowDomainParticleOptions(
                radius,
                length,
                (float) normalized.x,
                (float) normalized.y,
                (float) normalized.z,
                lifetime
        );
    }
}
