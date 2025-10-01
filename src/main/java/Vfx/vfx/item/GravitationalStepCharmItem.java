package Vfx.vfx.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class GravitationalStepCharmItem extends Item {
    private static final String TAG_ORIENTATION = "GravityOrientation";
    private static final String PLAYER_TAG = "VfxGravityCharmActive";
    private static final String PLAYER_POSE_TAG = "VfxGravityCharmPose";
    private static final Direction[] CYCLE_ORDER = new Direction[]{
            Direction.DOWN,
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.UP
    };

    public GravitationalStepCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Direction previous = getOrientation(stack);
        Direction next;
        if (player.isShiftKeyDown()) {
            next = cycleOrientation(previous);
        } else {
            Vec3 look = player.getLookAngle();
            next = Direction.getNearest(look.x, look.y, look.z);
        }

        if (next == null) {
            next = Direction.DOWN;
        }

        setOrientation(stack, next);

        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("Gravity orientation: " + describeDirection(next)), true);
        }

        if (next != previous && !level.isClientSide) {
            player.getCooldowns().addCooldown(this, 10);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.vfx.gravitational_step_charm.description")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Orientation: " + describeDirection(getOrientation(stack)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Use: align to gaze").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Sneak + Use: cycle orientations").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static ItemStack findCharm(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GravitationalStepCharmItem) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof GravitationalStepCharmItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static Direction getOrientation(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(TAG_ORIENTATION)) {
                Direction direction = Direction.byName(tag.getString(TAG_ORIENTATION));
                if (direction != null) {
                    return direction;
                }
            }
        }
        return Direction.DOWN;
    }

    public static void setOrientation(ItemStack stack, Direction direction) {
        stack.getOrCreateTag().putString(TAG_ORIENTATION, direction.getName());
    }

    public static void tickCharm(Player player, Direction orientation) {
        if (player.isSpectator() || player.isSleeping() || player.isPassenger() || player.isFallFlying() ||
                player.isCreative() && player.getAbilities().flying) {
            releaseGravity(player);
            return;
        }

        if (orientation == Direction.DOWN) {
            releaseGravity(player);
            return;
        }

        Vec3 gravity = Vec3.atLowerCornerOf(orientation.getNormal()).normalize();
        Vec3 delta = player.getDeltaMovement();
        double component = delta.dot(gravity);
        double maxSpeed = 0.6D;
        if (component > maxSpeed) {
            delta = delta.subtract(gravity.scale(component - maxSpeed));
        }
        if (component < -0.2D) {
            delta = delta.add(gravity.scale(-component * 0.2D));
        }

        Vec3 surfaceMotion = adjustSurfaceMotion(player, gravity, orientation, delta.subtract(gravity.scale(component)));
        delta = gravity.scale(component).add(surfaceMotion).add(gravity.scale(0.08D));
        player.setDeltaMovement(delta);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        if (!player.isNoGravity()) {
            player.setNoGravity(true);
        }

        if (hasSurface(player, orientation)) {
            player.setOnGround(true);
        }

        player.getPersistentData().putBoolean(PLAYER_TAG, true);
        updatePlayerPose(player, orientation);
    }

    public static void releaseGravity(Player player) {
        if (player.getPersistentData().contains(PLAYER_TAG)) {
            player.getPersistentData().remove(PLAYER_TAG);
            if (player.isNoGravity()) {
                player.setNoGravity(false);
            }
            player.fallDistance = 0.0F;
        }
        restorePlayerPose(player);
    }

    private static boolean hasSurface(Player player, Direction orientation) {
        Level level = player.level();
        Vec3 gravity = Vec3.atLowerCornerOf(orientation.getNormal()).normalize();
        Vec3 checkPoint = player.position().add(gravity.scale(0.6D));
        BlockPos blockPos = BlockPos.containing(checkPoint);
        BlockState state = level.getBlockState(blockPos);
        return !state.getCollisionShape(level, blockPos).isEmpty();
    }

    private static Direction cycleOrientation(Direction current) {
        for (int i = 0; i < CYCLE_ORDER.length; i++) {
            if (CYCLE_ORDER[i] == current) {
                return CYCLE_ORDER[(i + 1) % CYCLE_ORDER.length];
            }
        }
        return CYCLE_ORDER[0];
    }

    private static Vec3 adjustSurfaceMotion(Player player, Vec3 gravity, Direction orientation, Vec3 currentSurface) {
        double forwardInput = player.zza;
        double strafeInput = player.xxa;
        if (Math.abs(forwardInput) < 1.0E-3D && Math.abs(strafeInput) < 1.0E-3D) {
            return currentSurface.scale(0.91D);
        }

        Vec3 reference = Math.abs(gravity.y) < 0.999D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 right = gravity.cross(reference).normalize();
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 forward = right.cross(gravity).normalize();

        Vec3 desired = forward.scale(forwardInput).add(right.scale(strafeInput));
        if (desired.lengthSqr() < 1.0E-6D) {
            return currentSurface;
        }

        double speed = player.getSpeed();
        if (player.isSprinting()) {
            speed *= 1.3D;
        }
        if (player.isCrouching()) {
            speed *= 0.3D;
        }

        desired = desired.normalize().scale(speed);
        Vec3 blended = currentSurface.scale(0.5D).add(desired.scale(0.5D));

        if (orientation.getAxis().isHorizontal()) {
            double clamp = Math.max(Math.abs(forwardInput), Math.abs(strafeInput));
            blended = desired.scale(clamp);
        }

        return blended;
    }

    private static void updatePlayerPose(Player player, Direction orientation) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PLAYER_POSE_TAG)) {
            data.putString(PLAYER_POSE_TAG, player.getPose().name());
        }

        Pose targetPose = orientation == Direction.DOWN ? Pose.STANDING : Pose.SWIMMING;
        if (player.getPose() != targetPose) {
            player.setPose(targetPose);
            player.refreshDimensions();
        }
    }

    private static void restorePlayerPose(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(PLAYER_POSE_TAG)) {
            Pose pose;
            try {
                pose = Pose.valueOf(data.getString(PLAYER_POSE_TAG));
            } catch (IllegalArgumentException ignored) {
                pose = Pose.STANDING;
            }
            if (player.getPose() != pose) {
                player.setPose(pose);
                player.refreshDimensions();
            }
            data.remove(PLAYER_POSE_TAG);
        } else if (player.getPose() != Pose.STANDING) {
            player.setPose(Pose.STANDING);
            player.refreshDimensions();
        }
    }

    private static String describeDirection(Direction direction) {
        return switch (direction) {
            case UP -> "Up";
            case DOWN -> "Down";
            case NORTH -> "North";
            case SOUTH -> "South";
            case EAST -> "East";
            case WEST -> "West";
        };
    }
}
