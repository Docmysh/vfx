package Vfx.vfx.client;

import Vfx.vfx.gravity.GravityOrientation;
import Vfx.vfx.item.GravitationalStepCharmItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GravityClientHandler {

    /** Smooth camera roll (optional). */
    private static float currentRoll = 0f;

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        Direction orientation = GravitationalStepCharmItem.getActiveOrientation(player);
        if (orientation == Direction.DOWN) {
            currentRoll = lerp(currentRoll, 0f, 0.3f);
            return;
        }

        float targetRoll = GravityOrientation.computeCameraRoll(orientation);
        currentRoll = lerp(currentRoll, targetRoll, 0.35f);
        event.setRoll(event.getRoll() + currentRoll);

        if (orientation == Direction.UP) {
            event.setPitch(clamp(event.getPitch(), -70f, 70f));
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        Direction orientation = GravitationalStepCharmItem.getActiveOrientation(player);
        if (orientation == Direction.DOWN) {
            return;
        }

        Vec3 normal = GravityOrientation.surfaceNormal(orientation);
        Vec3 forward = playerForwardOnSurface(player, normal);
        Vec3 right = normal.cross(forward).normalize();

        boolean forwardKey = minecraft.options.keyUp.isDown();
        boolean backwardKey = minecraft.options.keyDown.isDown();
        boolean leftKey = minecraft.options.keyLeft.isDown();
        boolean rightKey = minecraft.options.keyRight.isDown();
        boolean jumpKey = minecraft.options.keyJump.isDown();
        boolean sneakKey = minecraft.options.keyShift.isDown();

        double speed = player.isSprinting() ? 0.12D : 0.08D;

        Vec3 desired = Vec3.ZERO;
        if (forwardKey) {
            desired = desired.add(forward);
        }
        if (backwardKey) {
            desired = desired.subtract(forward);
        }
        if (rightKey) {
            desired = desired.add(right);
        }
        if (leftKey) {
            desired = desired.subtract(right);
        }
        if (desired.lengthSqr() > 0) {
            desired = desired.normalize().scale(speed);
        }

        Vec3 toSurface = normal.scale(-0.08D);
        if (jumpKey) {
            toSurface = normal.scale(0.12D);
        }
        if (sneakKey) {
            toSurface = normal.scale(-0.12D);
        }

        Vec3 velocity = desired.add(toSurface);

        Vec3 currentVelocity = player.getDeltaMovement();
        Vec3 tangential = currentVelocity.subtract(normal.scale(currentVelocity.dot(normal)));
        velocity = velocity.add(tangential.scale(0.15D));

        player.setDeltaMovement(velocity);
        player.fallDistance = 0f;
        player.setOnGround(true);
        player.setJumping(false);
        player.setPos(player.getX() - normal.x * 0.02D, player.getY() - normal.y * 0.02D, player.getZ() - normal.z * 0.02D);
    }

    private static float lerp(float from, float to, double delta) {
        return (float) (from + (to - from) * delta);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Vec3 playerForwardOnSurface(LocalPlayer player, Vec3 normal) {
        float yawRad = (float) Math.toRadians(player.getYRot());
        float pitchRad = (float) Math.toRadians(player.getXRot());
        Vec3 forward = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        );
        Vec3 projected = forward.subtract(normal.scale(forward.dot(normal)));
        double lengthSquared = projected.lengthSqr();
        if (lengthSquared < 1.0E-6D) {
            projected = anyPerpendicular(normal);
        }
        return projected.normalize();
    }

    private static Vec3 anyPerpendicular(Vec3 vector) {
        if (Math.abs(vector.x) < 0.9D) {
            return vector.cross(new Vec3(1, 0, 0)).normalize();
        }
        return vector.cross(new Vec3(0, 1, 0)).normalize();
    }
}
