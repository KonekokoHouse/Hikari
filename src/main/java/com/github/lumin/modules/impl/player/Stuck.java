package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.utils.network.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class Stuck extends Module {

    public static final Stuck INSTANCE = new Stuck();

    private Stuck() {
        super("Stuck", Category.PLAYER);
    }

    private float lastYaw;
    private float lastPitch;

    @Override
    public void onDisable() {
        if (mc.player != null && !mc.player.onGround()) {
            PacketUtils.sendPacketNoEvent(new ServerboundMovePlayerPacket.PosRot(mc.player.getX() + 1337, mc.player.getY(), mc.player.getZ() + 1337, mc.player.getYRot() + 0.01f, mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));
        }
    }

    @SubscribeEvent
    public void onMovementInputUpdate(MovementInputUpdateEvent event) {
        event.getInput().moveVector = new Vec2(0, 0);
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Send e) {
        if (e.getPacket() instanceof ServerboundMovePlayerPacket || (e.getPacket() instanceof ClientboundSetEntityMotionPacket setEntityMotionPacket && setEntityMotionPacket.getId() == mc.player.getId())) {
            e.setCanceled(true);
        }
        if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
            toggle();
        }
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.RightClickItem event) {
        if (mc.player.getYRot() != lastYaw || mc.player.getXRot() != lastPitch) {
            PacketUtils.sendPacketNoEvent(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));
        }
        lastPitch = mc.player.getXRot();
        lastYaw = mc.player.getYRot();
    }

}
