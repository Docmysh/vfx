package Vfx.vfx.gravity;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public enum GravityOrientation {
    DOWN(Direction.DOWN,    0f,   new Vec3(0, 1, 0)),
    UP(Direction.UP,      180f,   new Vec3(0,-1, 0)),
    NORTH(Direction.NORTH, 90f,   new Vec3(0, 0, 1)),
    SOUTH(Direction.SOUTH,-90f,   new Vec3(0, 0,-1)),
    EAST(Direction.EAST,    0f,   new Vec3(1, 0, 0)),
    WEST(Direction.WEST,    0f,   new Vec3(-1,0, 0));

    public final Direction dir;
    public final float baseRollDeg;
    /** World-space "gravity" direction (what counts as down for the player). */
    public final Vec3 gravityDir;

    GravityOrientation(Direction d, float roll, Vec3 g) {
        this.dir = d;
        this.baseRollDeg = roll;
        this.gravityDir = g;
    }

    public static GravityOrientation from(Direction d) {
        for (GravityOrientation go : values()) {
            if (go.dir == d) {
                return go;
            }
        }
        return DOWN;
    }

    /** Return a roll angle that looks right for walls/ceilings. */
    public static float computeCameraRoll(Direction d) {
        return switch (d) {
            case DOWN -> 0f;
            case UP -> 180f;
            case NORTH -> 90f;
            case SOUTH -> -90f;
            case EAST -> 0f;
            case WEST -> 180f;
        };
    }

    /** A surface normal for collisions (points toward the player). */
    public static Vec3 surfaceNormal(Direction d) {
        return switch (d) {
            case DOWN -> new Vec3(0, 1, 0);
            case UP -> new Vec3(0,-1, 0);
            case NORTH -> new Vec3(0, 0, 1);
            case SOUTH -> new Vec3(0, 0,-1);
            case EAST -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(-1,0, 0);
        };
    }
}
