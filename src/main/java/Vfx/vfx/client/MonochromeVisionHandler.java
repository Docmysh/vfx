package Vfx.vfx.client;

import Vfx.vfx.Vfx;
import Vfx.vfx.item.UmbralLensItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Vfx.MODID, value = Dist.CLIENT)
public class MonochromeVisionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MONOCHROME_SHADER = new ResourceLocation("minecraft", "shaders/post/desaturate.json");
    private static boolean shaderActive = false;

    @SubscribeEvent
    public static void handleClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clearShader(minecraft);
            return;
        }

        Player player = minecraft.player;
        if (UmbralLensItem.hasMonochromeVision(player)) {
            applyShader(minecraft);
        } else {
            clearShader(minecraft);
        }
    }

    private static void applyShader(Minecraft minecraft) {
        if (shaderActive) {
            return;
        }

        GameRenderer renderer = minecraft.gameRenderer;
        try {
            renderer.loadEffect(MONOCHROME_SHADER);
            shaderActive = true;
        } catch (IOException exception) {
            LOGGER.error("Failed to apply monochrome vision shader", exception);
        }
    }

    private static void clearShader(Minecraft minecraft) {
        if (!shaderActive) {
            return;
        }

        GameRenderer renderer = minecraft.gameRenderer;
        renderer.shutdownEffect();
        shaderActive = false;
    }
}
