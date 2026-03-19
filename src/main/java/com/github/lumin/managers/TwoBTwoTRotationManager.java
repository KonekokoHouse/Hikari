package com.github.lumin.managers;

import com.github.lumin.events.JumpEvent;
import com.github.lumin.events.MotionEvent;
import com.github.lumin.events.PacketEvent;
import com.github.lumin.events.RayTraceEvent;
import com.github.lumin.events.StrafeEvent;
import com.github.lumin.modules.impl.player.MoveFix;
import com.github.lumin.utils.player.MoveUtils;
import com.github.lumin.utils.rotation.MovementFix;
import com.github.lumin.utils.rotation.Priority;
import com.github.lumin.utils.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TwoBTwoTRotationManager {

    public static final TwoBTwoTRotationManager INSTANCE = new TwoBTwoTRotationManager();

    private final Minecraft mc = Minecraft.getInstance();
    private final List<RotationRequest> requests = new CopyOnWriteArrayList<>();

    private RotationRequest rotation;
    private boolean rotate;
    private int rotateTicks;
    private float serverYaw;
    private float serverPitch;
    private float lastServerYaw;
    private float lastServerPitch;

    private int preserveTicks;

    private TwoBTwoTRotationManager() {
        NeoForge.EVENT_BUS.register(this);
    }

    public void setRotations(Vector2f rotations, double rotationSpeed, MovementFix correctMovement, Priority priority) {
        if (mc.player == null || rotations == null) {
            return;
        }
        if (Float.isNaN(rotations.x) || Float.isNaN(rotations.y) || Float.isInfinite(rotations.x) || Float.isInfinite(rotations.y)) {
            return;
        }
        if (MoveFix.INSTANCE.mouseSensFix.getValue()) {
            double gcd = Math.pow(mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 1.2;
            rotations.x = (float) (rotations.x - (rotations.x - serverYaw) % gcd);
            rotations.y = (float) (rotations.y - (rotations.y - serverPitch) % gcd);
        }

        RotationRequest request = new RotationRequest(
                priority != null ? priority.priority : Priority.Lowest.priority,
                Mth.wrapDegrees(rotations.x),
                Mth.clamp(rotations.y, -90.0f, 90.0f),
                correctMovement,
                true
        );

        RotationRequest existing = requests.stream().filter(r -> r.priority == request.priority).findFirst().orElse(null);
        if (existing == null) {
            requests.add(request);
        } else {
            existing.yaw = request.yaw;
            existing.pitch = request.pitch;
            existing.movementFix = request.movementFix;
            existing.snap = request.snap;
        }
    }

    public boolean isActive() {
        return rotation != null;
    }

    public boolean isMovementFixEnabled() {
        return rotation != null && rotation.movementFix == MovementFix.ON;
    }

    public float getYaw() {
        return rotation != null ? rotation.yaw : (mc.player != null ? mc.player.getYRot() : 0.0f);
    }

    public float getPitch() {
        return rotation != null ? rotation.pitch : (mc.player != null ? mc.player.getXRot() : 0.0f);
    }

    public float getServerYaw() {
        return serverYaw;
    }

    public float getServerPitch() {
        return serverPitch;
    }

    @SubscribeEvent
    private void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
            lastServerYaw = serverYaw;
            lastServerPitch = serverPitch;
            serverYaw = packet.getYRot(0.0f);
            serverPitch = packet.getXRot(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private void onClientTick(ClientTickEvent.Pre event) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        RotationRequest request = getRotationRequest();
        if (request == null) {
            if (rotation != null) {
                rotateTicks++;
                if (rotateTicks > MoveFix.INSTANCE.preserveTicks.getValue()) {
                    rotation = null;
                    preserveTicks = 0;
                }
            }
            return;
        }
        rotation = request;
        rotate = true;
        rotateTicks = 0;
        preserveTicks = MoveFix.INSTANCE.preserveTicks.getValue();
    }

    @SubscribeEvent
    private void onMoveInput(MovementInputUpdateEvent event) {
        if (rotation != null && rotation.movementFix == MovementFix.ON) {
            MoveUtils.fixMovement(event, rotation.yaw);
        }
    }

    @SubscribeEvent
    private void onRayTrace(RayTraceEvent event) {
        if (rotation != null) {
            event.setYaw(rotation.yaw);
            event.setPitch(rotation.pitch);
        }
    }

    @SubscribeEvent
    private void onStrafe(StrafeEvent event) {
        if (rotation != null && rotation.movementFix == MovementFix.ON) {
            event.setYaw(rotation.yaw);
        }
    }

    @SubscribeEvent
    private void onJump(JumpEvent event) {
        if (rotation != null && rotation.movementFix == MovementFix.ON) {
            event.setYaw(rotation.yaw);
        }
    }

    @SubscribeEvent
    private void onMotion(MotionEvent event) {
        if (rotation == null) {
            return;
        }
        if (rotate) {
            removeRotation(rotation);
            event.setYaw(rotation.yaw);
            event.setPitch(rotation.pitch);
            rotate = false;
        }
        if (rotation.snap) {
            if (preserveTicks <= 0) {
                rotation = null;
            }
        }
    }

    private RotationRequest getRotationRequest() {
        RotationRequest best = null;
        for (RotationRequest request : requests) {
            if (best == null || request.priority > best.priority) {
                best = request;
            }
        }
        return best;
    }

    private void removeRotation(RotationRequest request) {
        requests.remove(request);
    }

    private static final class RotationRequest {
        private final int priority;
        private float yaw;
        private float pitch;
        private MovementFix movementFix;
        private boolean snap;

        private RotationRequest(int priority, float yaw, float pitch, MovementFix movementFix, boolean snap) {
            this.priority = priority;
            this.yaw = yaw;
            this.pitch = pitch;
            this.movementFix = movementFix;
            this.snap = snap;
        }
    }
}
