package Vfx.vfx.gravity;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public enum GravityOrientation {
    DOWN(Direction.DOWN,    new Vec3(0, 1, 0)),
    UP(Direction.UP,        new Vec3(0,-1, 0)),
    NORTH(Direction.NORTH,  new Vec3(0, 0, 1)),
    SOUTH(Direction.SOUTH,  new Vec3(0, 0,-1)),
    EAST(Direction.EAST,    new Vec3(1, 0, 0)),
    WEST(Direction.WEST,    new Vec3(-1,0, 0));

    public final Direction dir;
    /** World-space "gravity" direction (what counts as down for the player). */
    public final Vec3 gravityDir;

    GravityOrientation(Direction d, Vec3 g) {
        this.dir = d;
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

    /**
     * Quaternion that rotates vanilla "down" (0,-1,0) into the provided orientation while keeping the
     * player's forward axis consistent. This is used to remap the camera so mouse input still feels natural.
     */
    public static Quaternionf cameraQuaternion(Direction direction) {
        return switch (direction) {
            case DOWN -> new Quaternionf();
            case UP -> Axis.ZP.rotationDegrees(180f);
            case NORTH -> Axis.XP.rotationDegrees(90f);
            case SOUTH -> Axis.XP.rotationDegrees(-90f);
            case EAST -> Axis.ZP.rotationDegrees(-90f);
            case WEST -> Axis.ZP.rotationDegrees(90f);
        };
    }
}
