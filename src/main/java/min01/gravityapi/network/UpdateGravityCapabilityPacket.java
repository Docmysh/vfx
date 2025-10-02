package min01.gravityapi.network;

import java.util.UUID;
import java.util.function.Supplier;

import min01.gravityapi.capabilities.GravityCapabilities;
import min01.gravityapi.util.GCUtil;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

public class UpdateGravityCapabilityPacket 
{
	private final UUID entityUUID;
	private final Direction baseGravityDirection;
	private final Direction currentGravityDirection;
	private final double baseGravityStrength;
	private final double currentGravityStrength;
	
	public UpdateGravityCapabilityPacket(UUID entityUUID, Direction baseGravityDirection, Direction currentGravityDirection, double baseGravityStrength, double currentGravityStrength) 
	{
		this.entityUUID = entityUUID;
		this.baseGravityDirection = baseGravityDirection;
		this.currentGravityDirection = currentGravityDirection;
		this.baseGravityStrength = baseGravityStrength;
		this.currentGravityStrength = currentGravityStrength;
	}

	public UpdateGravityCapabilityPacket(FriendlyByteBuf buf)
	{
		this.entityUUID = buf.readUUID();
		this.baseGravityDirection = buf.readEnum(Direction.class);
		this.currentGravityDirection = buf.readEnum(Direction.class);
		this.baseGravityStrength = buf.readDouble();
		this.currentGravityStrength = buf.readDouble();
	}

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeUUID(this.entityUUID);
		buf.writeEnum(this.baseGravityDirection);
		buf.writeEnum(this.currentGravityDirection);
		buf.writeDouble(this.baseGravityStrength);
		buf.writeDouble(this.currentGravityStrength);
	}
	
	public static class Handler 
	{
		public static boolean onMessage(UpdateGravityCapabilityPacket message, Supplier<NetworkEvent.Context> ctx) 
		{
			ctx.get().enqueueWork(() ->
			{
				if(ctx.get().getDirection().getReceptionSide().isClient())
				{
					GCUtil.getClientLevel(level -> 
					{
						Entity entity = GCUtil.getEntityByUUID(level, message.entityUUID);
						entity.getCapability(GravityCapabilities.GRAVITY).ifPresent(cap -> 
						{
							cap.sync(message.baseGravityDirection, message.currentGravityDirection, message.baseGravityStrength, message.currentGravityStrength);
							cap.applyGravityChange();
						});
					});
				}
			});

			ctx.get().setPacketHandled(true);
			return true;
		}
	}
}
