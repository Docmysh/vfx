package Vfx.vfx.item;

import Vfx.vfx.Vfx;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Vfx.MODID)
public class GravitationalStepCharmEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player == null) {
            return;
        }

        ItemStack charm = GravitationalStepCharmItem.findCharm(player);
        if (charm.isEmpty()) {
            GravitationalStepCharmItem.releaseGravity(player);
            return;
        }

        Direction orientation = GravitationalStepCharmItem.getOrientation(charm);
        GravitationalStepCharmItem.tickCharm(player, orientation);
    }
}
