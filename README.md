# VFX Development Environment Guide

## Base Development Environment

Add the Photon and LDLib repository to your Gradle `repositories` block:

```groovy
repositories {
    maven { url = "https://maven.firstdarkdev.xyz/snapshots" } // LDLib, Photon
}
```

### Fabric

```groovy
dependencies {
    modImplementation("com.lowdragmc.photon:photon-fabric-1.20.1:{latest_version}") { transitive = false }
    modImplementation("com.lowdragmc.ldlib:ldlib-fabric-1.20.1:{latest_version}") { transitive = false }
}
```

### Forge

```groovy
dependencies {
    implementation fg.deobf("com.lowdragmc.photon:photon-forge-1.20.1:{latest_version}") { transitive = false }
    implementation fg.deobf("com.lowdragmc.ldlib:ldlib-forge-1.20.1:{latest_version}") { transitive = false }
}
```

### Architectury (Common)

```groovy
dependencies {
    implementation fg.deobf("com.lowdragmc.photon:photon-common-1.20.1:{latest_version}") { transitive = false }
    implementation fg.deobf("com.lowdragmc.ldlib:ldlib-common-1.20.1:{latest_version}") { transitive = false }
}
```

## Loading and Using Photon Effect Files

```java
FX fx = FXHelper.getFX(new ResourceLocation("photon:fire"));
// Bind it to a block
new BlockEffect(fx, level, pos).start();
// Bind it to an entity
new EntityEffect(fx, level, entity).start();
```

## Implementing Custom `IFXEffect`

Implement your own `IFXEffect` when you need to control the lifecycle of Photon effects directly. Review the source of `BlockEffect` and `EntityEffect` for reference implementations.

```java
public interface IFXEffect {
    /**
     * get all emitters included in this effect.
     */
    FX getFx();
    /**
     * set effect offset
     */
    void setOffset(double x, double y, double z);

    /**
     * set effect delay
     */
    void setDelay(int delay);

    /**
     * Whether to remove particles directly when the bound object invalid.
     * <br>
     * default - wait for particles death.
     */
    void setForcedDeath(boolean forcedDeath);

    /**
     * Allows multiple identical effects to be bound to a same object。
     */
    void setAllowMulti(boolean allowMulti);

    /**
     * get all emitters included in this effect.
     */
    List<IParticleEmitter> getEmitters();

    /**
     * update each emitter during their duration,
     * @param emitter emitter
     * @return true - block emitter origin tick logic.
     */
    boolean updateEmitter(IParticleEmitter emitter);

    /**
     * start effect。
     */
    void start();
}
```

Use these hooks to manage offsets, delays, cleanup behavior, and other lifecycle details for your custom effects.
