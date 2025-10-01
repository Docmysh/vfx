package min01.gravityapi.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public class GravityBlockPos extends BlockPos
{
	private final Direction direction;
	
	public GravityBlockPos(Vec3i p_121877_, Direction direction)
	{
		super(p_121877_);
		this.direction = direction;
	}
	
	@Override
	public BlockPos relative(Direction p_121946_, int p_121949_) 
	{
	    return switch (this.direction) 
	    {
	        case UP -> switch (p_121946_) 
	        {
	            case UP -> super.relative(Direction.DOWN, p_121949_);
	            case DOWN -> super.relative(Direction.UP, p_121949_);
	            case WEST -> super.relative(Direction.WEST, p_121949_);
	            case EAST -> super.relative(Direction.EAST, p_121949_);
	            default -> super.relative(p_121946_, p_121949_);
	        };
	        case NORTH -> switch (p_121946_)
	        {
	            case UP -> super.relative(Direction.SOUTH, p_121949_);
	            case DOWN -> super.relative(Direction.NORTH, p_121949_);
	            case NORTH -> super.relative(Direction.UP, p_121949_);
	            case SOUTH -> super.relative(Direction.DOWN, p_121949_);
	            default -> super.relative(p_121946_, p_121949_);
	        };
	        case SOUTH -> switch (p_121946_)
	        {
	            case UP -> super.relative(Direction.NORTH, p_121949_);
	            case DOWN -> super.relative(Direction.SOUTH, p_121949_);
	            case NORTH -> super.relative(Direction.DOWN, p_121949_);
	            case SOUTH -> super.relative(Direction.UP, p_121949_);
	            default -> super.relative(p_121946_, p_121949_);
	        };
	        case WEST -> switch (p_121946_) 
	        {
	            case UP -> super.relative(Direction.EAST, p_121949_);
	            case DOWN -> super.relative(Direction.WEST, p_121949_);
	            case WEST -> super.relative(Direction.DOWN, p_121949_);
	            case EAST -> super.relative(Direction.UP, p_121949_);
	            default -> super.relative(p_121946_, p_121949_);
	        };
	        case EAST -> switch (p_121946_) 
	        {
	            case UP -> super.relative(Direction.WEST, p_121949_);
	            case DOWN -> super.relative(Direction.EAST, p_121949_);
	            case WEST -> super.relative(Direction.UP, p_121949_);
	            case EAST -> super.relative(Direction.DOWN, p_121949_);
	            default -> super.relative(p_121946_, p_121949_);
	        };
	        default -> super.relative(p_121946_, p_121949_);
	    };
	}

	@Override
	public BlockPos relative(Direction p_121946_)
	{
	    return switch (this.direction)
	    {
	        case UP -> switch (p_121946_) 
	        {
	            case UP -> super.relative(Direction.DOWN);
	            case DOWN -> super.relative(Direction.UP);
	            case WEST -> super.relative(Direction.WEST);
	            case EAST -> super.relative(Direction.EAST);
	            default -> super.relative(p_121946_);
	        };
	        case NORTH -> switch (p_121946_) 
	        {
	            case UP -> super.relative(Direction.SOUTH);
	            case DOWN -> super.relative(Direction.NORTH);
	            case NORTH -> super.relative(Direction.UP);
	            case SOUTH -> super.relative(Direction.DOWN);
	            default -> super.relative(p_121946_);
	        };
	        case SOUTH -> switch (p_121946_)
	        {
	            case UP -> super.relative(Direction.NORTH);
	            case DOWN -> super.relative(Direction.SOUTH);
	            case NORTH -> super.relative(Direction.DOWN);
	            case SOUTH -> super.relative(Direction.UP);
	            default -> super.relative(p_121946_);
	        };
	        case WEST -> switch (p_121946_)
	        {
	            case UP -> super.relative(Direction.EAST);
	            case DOWN -> super.relative(Direction.WEST);
	            case WEST -> super.relative(Direction.DOWN);
	            case EAST -> super.relative(Direction.UP);
	            default -> super.relative(p_121946_);
	        };
	        case EAST -> switch (p_121946_)
	        {
	            case UP -> super.relative(Direction.WEST);
	            case DOWN -> super.relative(Direction.EAST);
	            case WEST -> super.relative(Direction.UP);
	            case EAST -> super.relative(Direction.DOWN);
	            default -> super.relative(p_121946_);
	        };
	        default -> super.relative(p_121946_);
	    };
	}
}
