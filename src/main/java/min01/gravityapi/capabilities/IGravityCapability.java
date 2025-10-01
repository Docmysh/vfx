package min01.gravityapi.capabilities;

import min01.gravityapi.GravityAPI;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

@AutoRegisterCapability
public interface IGravityCapability extends INBTSerializable<CompoundTag>
{
	ResourceLocation ID = new ResourceLocation(GravityAPI.MODID, "gravity");

	void setEntity(Entity entity);
	
	void tick();
	
	void applyGravityChange();
	
	void sync(Direction baseGravityDirection, Direction currentGravityDirection, double baseGravityStrength, double currentGravityStrength);
}
