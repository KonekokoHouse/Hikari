package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Random;

public class Disabler extends Module {

    public static final Disabler INSTANCE = new Disabler();

    private Disabler() {
        super("Disabler", Category.PLAYER);
    }

    private final BoolSetting DuplicateRotPlace = boolSetting("DuplicateRotPlace", true);
    private final BoolSetting aim360 = boolSetting("AimModulo360", false);
    private final BoolSetting AimDuplicateLook = boolSetting("AimDuplicateLook", false);

    private float playerYaw, lastYaw, lastPitch = 0;

    @SubscribeEvent
    public void onPacket(PacketEvent.Send event) {
        if (aim360.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
                float yaw = packet.getYRot(0);
                if (yaw < 360.0f && yaw > -360.0f) {
                    packet.yRot = yaw + 720f;
                }
            }
        }

        if (AimDuplicateLook.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket c03) {
                if (c03.hasRotation()) {
                    if (lastYaw == c03.getYRot(0) && lastPitch == c03.getXRot(0)) {
                        c03.yRot = c03.getYRot(0) + 0.001f;
                    }
                    lastYaw = c03.getYRot(0);
                    lastPitch = c03.getXRot(0);
                }
            }
        }

        if (DuplicateRotPlace.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
                if (packet.hasRotation()) {
                    float originalYaw = packet.getYRot(0.0F);

                    if (originalYaw < 360.0F && originalYaw > -360.0F) {
                        packet.yRot = originalYaw + 720f;
                    }

                    float lastPlayerYaw = this.playerYaw;
                    this.playerYaw = packet.getYRot(0.0F);

                    float deltaYaw = Math.abs(this.playerYaw - lastPlayerYaw);
                    if (deltaYaw > 2.0F) {
                        Random random = new Random();
                        float perturbation = 0.005f + random.nextFloat() * 0.015f;
                        if (random.nextBoolean()) {
                            packet.yRot = packet.getYRot(0) + perturbation;
                        } else {
                            packet.yRot = packet.getYRot(0) - perturbation;
                        }
                    }
                }
            }
        }
    }

}
