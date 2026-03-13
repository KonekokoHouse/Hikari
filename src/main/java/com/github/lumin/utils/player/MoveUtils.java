package com.github.lumin.utils.player;

import com.github.lumin.utils.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

public class MoveUtils {

    static Minecraft mc = Minecraft.getInstance();

    public static boolean isMoving() {
        return mc.player.zza != 0 || mc.player.xxa != 0;
    }

    public static double getDirection(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public static void fixMovement(MovementInputUpdateEvent event, float yaw) {
        Vec2 moveVector = event.getInput().getMoveVector();
        float forward = moveVector.y;
        float strafe = moveVector.x;

        double angle = Mth.wrapDegrees(Math.toDegrees(getDirection(mc.player.getYRot(), forward, strafe)));

        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = Mth.wrapDegrees(Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe)));
                final double difference = MathUtils.wrappedDifference(angle, predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.getInput().moveVector = new Vec2(closestStrafe, closestForward);
    }

}
