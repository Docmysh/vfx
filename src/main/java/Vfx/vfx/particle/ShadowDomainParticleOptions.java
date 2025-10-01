package Vfx.vfx.particle;

import Vfx.vfx.VfxParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

public record ShadowDomainParticleOptions(float radius, float length, float directionX, float directionY, float directionZ, int lifetime) implements ParticleOptions {
    public static final Codec<ShadowDomainParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("radius").forGetter(ShadowDomainParticleOptions::radius),
                    Codec.FLOAT.fieldOf("length").forGetter(ShadowDomainParticleOptions::length),
                    Codec.FLOAT.fieldOf("directionX").forGetter(ShadowDomainParticleOptions::directionX),
                    Codec.FLOAT.fieldOf("directionY").forGetter(ShadowDomainParticleOptions::directionY),
                    Codec.FLOAT.fieldOf("directionZ").forGetter(ShadowDomainParticleOptions::directionZ),
                    Codec.INT.fieldOf("lifetime").forGetter(ShadowDomainParticleOptions::lifetime)
            ).apply(instance, ShadowDomainParticleOptions::new)
    );

    public static final Deserializer<ShadowDomainParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public ShadowDomainParticleOptions fromCommand(ParticleType<ShadowDomainParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float radius = reader.readFloat();
            reader.expect(' ');
            float length = reader.readFloat();
            reader.expect(' ');
            float directionX = reader.readFloat();
            reader.expect(' ');
            float directionY = reader.readFloat();
            reader.expect(' ');
            float directionZ = reader.readFloat();
            reader.expect(' ');
            int lifetime = reader.readInt();
            return new ShadowDomainParticleOptions(radius, length, directionX, directionY, directionZ, lifetime);
        }

        @Override
        public ShadowDomainParticleOptions fromNetwork(ParticleType<ShadowDomainParticleOptions> type, FriendlyByteBuf buffer) {
            float radius = buffer.readFloat();
            float length = buffer.readFloat();
            float directionX = buffer.readFloat();
            float directionY = buffer.readFloat();
            float directionZ = buffer.readFloat();
            int lifetime = buffer.readVarInt();
            return new ShadowDomainParticleOptions(radius, length, directionX, directionY, directionZ, lifetime);
        }
    };

    @Override
    public ParticleType<ShadowDomainParticleOptions> getType() {
        return VfxParticles.SHADOW_DOMAIN.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(radius);
        buffer.writeFloat(length);
        buffer.writeFloat(directionX);
        buffer.writeFloat(directionY);
        buffer.writeFloat(directionZ);
        buffer.writeVarInt(lifetime);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f %.2f %d",
                BuiltInRegistries.PARTICLE_TYPE.getKey(getType()),
                radius,
                length,
                directionX,
                directionY,
                directionZ,
                lifetime);
    }
}
