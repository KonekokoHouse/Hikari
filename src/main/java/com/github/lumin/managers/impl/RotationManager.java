package com.github.lumin.managers.impl;

import com.github.lumin.events.JumpRotationEvent;
import com.github.lumin.events.MotionEvent;
import com.github.lumin.events.RayTraceEvent;
import com.github.lumin.events.StrafeEvent;
import com.github.lumin.utils.AuthUtils;
import com.github.lumin.utils.player.MoveUtils;
import com.github.lumin.utils.rotation.MovementFix;
import com.github.lumin.utils.rotation.Priority;
import com.github.lumin.utils.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2f;

import java.util.function.Function;

public class RotationManager {

    Minecraft mc = Minecraft.getInstance();

    private final Vector2f offset = new Vector2f(0, 0);
    public Vector2f rotations;
    public Vector2f lastRotations = new Vector2f(0, 0);
    public Vector2f targetRotations;
    public Vector2f animationRotation = null;
    public Vector2f lastAnimationRotation = null;

    private boolean active;
    private boolean smoothed;
    private double rotationSpeed;
    private MovementFix correctMovement;
    private Function<Vector2f, Boolean> raycast;
    private float randomAngle;

    private int priority;

    public RotationManager() {
        NeoForge.EVENT_BUS.register(this);
    }

    public void setRotations(final Vector2f rotations, final double rotationSpeed) {
        setRotations(rotations, rotationSpeed, MovementFix.OFF, null, Priority.Lowest);
    }

    public void setRotations(final Vector2f rotations, final double rotationSpeed, final MovementFix correctMovement) {
        setRotations(rotations, rotationSpeed, correctMovement, null, Priority.Lowest);
    }

    public void setRotations(final Vector2f rotations, final double rotationSpeed, final MovementFix correctMovement, Priority priority) {
        AuthUtils.checkConnection();
        setRotations(rotations, rotationSpeed, correctMovement, null, priority);
    }

    public void setRotations(final Vector2f rotations, final double rotationSpeed, final MovementFix correctMovement, final Function<Vector2f, Boolean> raycast, Priority priority) {
        if (rotations == null || Double.isNaN(rotations.x) || Double.isNaN(rotations.y) || Double.isInfinite(rotations.x) || Double.isInfinite(rotations.y)) {
            return;
        }

        if (active && priority.priority < this.priority) {
            return;
        }

        this.targetRotations = rotations;
        this.rotationSpeed = rotationSpeed * 18;
        this.correctMovement = correctMovement;
        this.raycast = raycast;
        this.priority = priority.priority;
        active = true;

        smooth();
    }

    private void smooth() {
        if (!smoothed) {
            float targetYaw = targetRotations.x;
            float targetPitch = targetRotations.y;

            if (raycast != null && (Math.abs(targetYaw - rotations.x) > 5 || Math.abs(targetPitch - rotations.y) > 5)) {
                final Vector2f trueTargetRotations = new Vector2f(targetRotations.x, targetRotations.y);

                double speed = (Math.random() * Math.random() * Math.random()) * 20;
                randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360)) * (mc.player.tickCount / 10 % 2 == 0 ? -1 : 1));

                if (Float.isNaN(randomAngle) || Float.isInfinite(randomAngle)) randomAngle = 0;

                offset.x = ((float) (offset.x + -Mth.sin((float) Math.toRadians(randomAngle)) * speed));
                offset.y = ((float) (offset.y + Mth.cos((float) Math.toRadians(randomAngle)) * speed));

                if (Float.isNaN(offset.x) || Float.isInfinite(offset.x)) offset.x = 0;
                if (Float.isNaN(offset.y) || Float.isInfinite(offset.y)) offset.y = 0;

                targetYaw += offset.x;
                targetPitch += offset.y;

                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    randomAngle = (float) Math.toDegrees(Math.atan2(trueTargetRotations.x - targetYaw, targetPitch - trueTargetRotations.y)) - 180;
                    if (Float.isNaN(randomAngle)) randomAngle = 0;

                    targetYaw -= offset.x;
                    targetPitch -= offset.y;

                    offset.x = ((float) (offset.x + -Mth.sin((float) Math.toRadians(randomAngle)) * speed));
                    offset.y = ((float) (offset.y + Mth.cos((float) Math.toRadians(randomAngle)) * speed));

                    if (Float.isNaN(offset.x) || Float.isInfinite(offset.x)) offset.x = 0;
                    if (Float.isNaN(offset.y) || Float.isInfinite(offset.y)) offset.y = 0;

                    targetYaw = targetYaw + offset.x;
                    targetPitch = targetPitch + offset.y;
                }

                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    offset.x = 0;
                    offset.y = 0;

                    targetYaw = (float) (targetRotations.x + Math.random() * 2);
                    targetPitch = (float) (targetRotations.y + Math.random() * 2);
                }
            }

            rotations = RotationUtils.smooth(new Vector2f(targetYaw, targetPitch), rotationSpeed + Math.random());

            if (Float.isNaN(rotations.x) || Float.isInfinite(rotations.x)) {
                rotations.x = mc.player.getYRot();
            }

            if (Float.isNaN(rotations.y) || Float.isInfinite(rotations.y)) {
                rotations.y = mc.player.getXRot();
            }
        }

        smoothed = true;
    }

    public boolean isSmoothed() {
        return smoothed;
    }

    public void setSmoothed(boolean smoothed) {
        this.smoothed = smoothed;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public float getYaw() {
        if (mc.player == null) {
            return 0.0f;
        } else if (active) {
            return rotations.x;
        } else {
            return mc.player.getYRot();
        }
    }

    public float getPitch() {
        if (mc.player == null) {
            return 0.0f;
        } else if (active) {
            return rotations.y;
        } else {
            return mc.player.getXRot();
        }
    }

    public Vector2f getRotation() {
        if (active) return rotations;
        else return new Vector2f(mc.player.getYRot(), mc.player.getXRot());
    }

    public float[] getRotation(Vec3 vec) {
        return getRotation(mc.player.getEyePosition(), vec);
    }

    public float[] getRotation(Vec3 eyesPos, Vec3 vec) {
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch)};
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private void onClientTick(ClientTickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        if (!active || rotations == null || lastRotations == null || targetRotations == null) {
            final var defaultRotation = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
            targetRotations = defaultRotation;
            lastRotations = defaultRotation;
            rotations = defaultRotation;
        }

        if (active) {
            smooth();
        }
    }

    @SubscribeEvent
    private void onMoveInput(MovementInputUpdateEvent event) {
        if (active && correctMovement == MovementFix.ON && rotations != null) {
            MoveUtils.fixMovement(event, rotations.x);
        }
    }

    @SubscribeEvent
    private void onRaytrace(RayTraceEvent event) {
        if (active && rotations != null) {
            event.setYaw(rotations.x);
            event.setPitch(rotations.y);
        }
    }

    @SubscribeEvent
    private void onStrafe(StrafeEvent event) {
        if (active && correctMovement == MovementFix.ON && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @SubscribeEvent
    private void onJump(JumpRotationEvent event) {
        if (active && correctMovement == MovementFix.ON && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @SubscribeEvent
    private void onMotion(MotionEvent event) {
        if (active && rotations != null) {
            float yaw = rotations.x;
            float pitch = rotations.y;

            if (Float.isNaN(yaw) || Float.isInfinite(yaw)) yaw = mc.player.getYRot();
            if (Float.isNaN(pitch) || Float.isInfinite(pitch)) pitch = mc.player.getXRot();
            pitch = Mth.clamp(pitch, -90.0f, 90.0f);

            event.setYaw(yaw);
            event.setPitch(pitch);

            if (Math.abs((rotations.x - mc.player.getYRot()) % 360) < 1 && Math.abs((rotations.y - mc.player.getXRot())) < 1) {
                active = false;
                priority = 0;
                correctDisabledRotations();
            }

            lastRotations = rotations;
        } else {
            lastRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        }

        lastAnimationRotation = animationRotation;
        Vector2f currentAnimationRotation;
        if (active && rotations != null) {
            currentAnimationRotation = rotations;
        } else {
            currentAnimationRotation = new Vector2f(event.getYaw(), event.getPitch());
        }

        if (lastAnimationRotation == null) {
            animationRotation = currentAnimationRotation;
        } else {
            float targetYaw = currentAnimationRotation.x;
            float targetPitch = currentAnimationRotation.y;
            float lastYaw = lastAnimationRotation.x;
            float lastPitch = lastAnimationRotation.y;
            float yawDiff = Mth.wrapDegrees(targetYaw - lastYaw);
            float pitchDiff = targetPitch - lastPitch;

            float smoothYaw = lastYaw + yawDiff * 0.5f;
            float smoothPitch = lastPitch + pitchDiff * 0.5f;
            animationRotation = new Vector2f(Mth.wrapDegrees(smoothYaw), Mth.clamp(smoothPitch, -90.0f, 90.0f));
        }

        targetRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        smoothed = false;
    }

    private void correctDisabledRotations() {
        if (lastRotations == null) return;
        final Vector2f rotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        final Vector2f fixedRotations = RotationUtils.resetRotation(RotationUtils.applySensitivityPatch(rotations, lastRotations));

        if (!Float.isNaN(fixedRotations.x) && !Float.isNaN(fixedRotations.y)) {
            mc.player.setYRot(fixedRotations.x);
            mc.player.setXRot(fixedRotations.y);
        }
    }

}
