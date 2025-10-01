package min01.gravityapi.event;

import min01.gravityapi.GravityAPI;
import com.min01.gravityapi.api.GravityChangerAPI;
import min01.gravityapi.config.GravityConfig;
import com.min01.gravityapi.util.GCUtil;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = GravityAPI.MODID)
public class CommonEvents
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
    	//GravityCommand.register(event.getDispatcher());
    }
    
	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		Entity entity = event.getEntity();
		GCUtil.ENTITY_MAP.put(entity.getClass().hashCode(), entity);
		GCUtil.ENTITY_MAP2.put(entity.getClass().getSuperclass().hashCode(), entity);
	}
	
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
    	if(event.isWasDeath() && !GravityConfig.resetGravityOnRespawn.get())
    	{
        	Player original = event.getOriginal();
        	original.revive();
        	Player player = event.getEntity();
        	GravityChangerAPI.setBaseGravityDirection(player, GravityChangerAPI.getBaseGravityDirection(original));
    	}
    }
}
