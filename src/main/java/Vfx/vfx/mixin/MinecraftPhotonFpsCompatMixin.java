package Vfx.vfx.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides a compatibility bridge for Photon which expects the legacy obfuscated
 * FPS field name (f_91021_) to exist on {@link Minecraft}. The field was renamed
 * in official mappings, so we mirror its value each client tick.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftPhotonFpsCompatMixin {

    @Shadow
    private int fps;

    /**
     * Backwards-compatibility mirror for Photon. The name intentionally matches
     * the legacy SRG identifier Photon requests via an accessor mixin.
     */
    private int f_91021_;

    @Unique
    private void vfx$syncPhotonFpsField() {
        this.f_91021_ = this.fps;
    }

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void vfx$initPhotonCompat(Minecraft.RunArgs config, CallbackInfo ci) {
        vfx$syncPhotonFpsField();
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void vfx$mirrorFpsForPhoton(CallbackInfo ci) {
        vfx$syncPhotonFpsField();
    }

    @Inject(method = "runTick", at = @At("TAIL"), require = 0)
    private void vfx$mirrorFpsForPhoton(boolean unused, CallbackInfo ci) {
        vfx$syncPhotonFpsField();
    }
}
