package Vfx.vfx.entity.shadow;

import net.minecraft.network.chat.Component;

/**
 * Represents the behavior that the shadow hand should follow when a player uses the relic.
 */
public enum ShadowHandMode {
    CRUSH(0, "mode.vfx.shadow_hand_relic.crush"),
    THROW(1, "mode.vfx.shadow_hand_relic.throw");

    private final int id;
    private final String translationKey;

    ShadowHandMode(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public int getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public Component getDisplayName() {
        return Component.translatable(this.translationKey);
    }

    public ShadowHandMode cycle() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public static ShadowHandMode byId(int id) {
        for (ShadowHandMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return CRUSH;
    }
}
