package min01.gravityapi;

import min01.gravityapi.capabilities.GravityCapabilities;
import min01.gravityapi.config.GravityConfig;
import min01.gravityapi.init.GravityBlocks;
import min01.gravityapi.init.GravityCreativeTabs;
import min01.gravityapi.init.GravityItems;
import min01.gravityapi.init.GravityMobEffects;
import min01.gravityapi.network.GravityNetwork;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GravityAPI.MODID)
public class GravityAPI
{
	public static final String MODID = "gravityapi";
	
	public GravityAPI() 
	{
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		ModLoadingContext ctx = ModLoadingContext.get();
		
		GravityItems.ITEMS.register(bus);
		GravityBlocks.BLOCKS.register(bus);
		GravityBlocks.BLOCK_ENTITIES.register(bus);
		GravityMobEffects.EFFECTS.register(bus);
		GravityMobEffects.POTIONS.register(bus);
		GravityCreativeTabs.CREATIVE_MODE_TAB.register(bus);

		GravityNetwork.registerMessages();
		MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, GravityCapabilities::attachEntityCapability);
		ctx.registerConfig(Type.COMMON, GravityConfig.CONFIG_SPEC, "gravity-api.toml");
	}
}
