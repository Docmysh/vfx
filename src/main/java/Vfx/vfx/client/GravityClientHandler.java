package Vfx.vfx.client;

import Vfx.vfx.gravity.GravityOrientation;
import Vfx.vfx.item.GravitationalStepCharmItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GravityClientHandler {

    private static float currentRoll = 0f;

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        Direction orientation = GravitationalStepCharmItem.getActiveOrientation(player);
        ViewFrame frame = buildFrame(player, orientation);
        if (frame == null) {
            currentRoll = lerp(currentRoll, 0f, 0.35f);
            return;
        }

        Vec3 forward = frame.forward;
        Vec3 up = frame.up;
        Vec3 right = frame.right;

        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-forward.y, -1.0, 1.0)));

        Vec3 projectedWorldUp = projectOntoPlane(new Vec3(0D, 1D, 0D), forward);
        if (projectedWorldUp.lengthSqr() < 1.0E-6D) {
            projectedWorldUp = up;
        } else {
            projectedWorldUp = projectedWorldUp.normalize();
        }

        double rollRad = Math.atan2(right.dot(projectedWorldUp), up.dot(projectedWorldUp));
        float targetRoll = (float) Math.toDegrees(rollRad);
        currentRoll = lerp(currentRoll, targetRoll, 0.35f);

        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(currentRoll);
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
            currentRoll = lerp(currentRoll, 0f, 0.35f);
            return;
        }

        ViewFrame frame = buildFrame(player, orientation);
        if (frame == null) {
            return;
        }

        Vec3 normal = GravityOrientation.surfaceNormal(orientation).normalize();
        Vec3 forward = frame.forward.subtract(normal.scale(frame.forward.dot(normal)));
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = frame.right;
        } else {
            forward = forward.normalize();
        }
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

        Vec3 newVelocity = desired.add(toSurface);
        Vec3 currentVelocity = player.getDeltaMovement();
        Vec3 tangential = currentVelocity.subtract(normal.scale(currentVelocity.dot(normal)));
        newVelocity = newVelocity.add(tangential.scale(0.15D));

        player.setDeltaMovement(newVelocity);
        player.fallDistance = 0f;
        player.setOnGround(true);
        player.setJumping(false);
        player.setPos(
                player.getX() - normal.x * 0.02D,
                player.getY() - normal.y * 0.02D,
                player.getZ() - normal.z * 0.02D
        );
    }

    private static float lerp(float from, float to, double delta) {
        return (float) (from + (to - from) * delta);
    }

    private static Vec3 projectOntoPlane(Vec3 vector, Vec3 planeNormal) {
        return vector.subtract(planeNormal.scale(vector.dot(planeNormal)));
    }

    private static ViewFrame buildFrame(LocalPlayer player, Direction orientation) {
        Quaternionf orientationRotation = GravityOrientation.cameraQuaternion(orientation);
        Quaternionf viewRotation = new Quaternionf(orientationRotation);
        Quaternionf lookRotation = new Quaternionf();
        lookRotation.rotationYXZ((float) Math.toRadians(-player.getYRot()), (float) Math.toRadians(-player.getXRot()), 0f);
        viewRotation.mul(lookRotation);

        Vector3f forward = new Vector3f(0f, 0f, 1f);
        viewRotation.transform(forward);
        if (forward.lengthSquared() < 1.0E-6f) {
            return null;
        }
        forward.normalize();

        Vector3f up = new Vector3f(0f, 1f, 0f);
        viewRotation.transform(up);
        if (up.lengthSquared() < 1.0E-6f) {
            Vec3 normal = GravityOrientation.surfaceNormal(orientation);
            up.set((float) normal.x, (float) normal.y, (float) normal.z);
        }
        up.normalize();

        Vector3f right = forward.cross(up, new Vector3f());
        if (right.lengthSquared() < 1.0E-6f) {
            right = up.cross(forward, new Vector3f());
        }
        if (right.lengthSquared() < 1.0E-6f) {
            return null;
        }
        right.normalize();
        up = right.cross(forward, new Vector3f()).normalize();

        return new ViewFrame(
                new Vec3(forward.x(), forward.y(), forward.z()),
                new Vec3(up.x(), up.y(), up.z()),
                new Vec3(right.x(), right.y(), right.z())
        );
    }

    private record ViewFrame(Vec3 forward, Vec3 up, Vec3 right) { }
}
