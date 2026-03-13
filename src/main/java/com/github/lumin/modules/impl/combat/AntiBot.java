package com.github.lumin.modules.impl.combat;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiBot extends Module {

    public static final AntiBot INSTANCE = new AntiBot();

    private AntiBot() {
        super("AntiBot", Category.COMBAT);
    }

    private static final Map<UUID, Long> uuids = new ConcurrentHashMap<>();
    private static final Set<Integer> ids = new HashSet<>();


    public boolean isBot(Entity entity) {
        return (ids.contains(entity.getId()) || !mc.getConnection().getOnlinePlayerIds().contains(entity.getUUID())) && isEnabled();
    }

    @SubscribeEvent
    public void onRespawn(PlayerTickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.player.tickCount <= 1) {
            ids.clear();
            uuids.clear();
        }
    }

    @SubscribeEvent
    public void onMotion(ClientTickEvent.Pre event) {
        for (Map.Entry<UUID, Long> entry : uuids.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() > 500L) {
                uuids.remove(entry.getKey());
            }
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket packet) {
            if (packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                    if (entry.displayName() != null && entry.profile() != null && entry.displayName().getSiblings().isEmpty() && entry.gameMode() == GameType.SURVIVAL) {
                        UUID uuid = entry.profile().id();
                        uuids.put(uuid, System.currentTimeMillis());
                    }
                }
            }
        } else if (event.getPacket() instanceof ClientboundAddEntityPacket packet && packet.getType() == EntityType.PLAYER) {
            if (uuids.containsKey(packet.getUUID())) {
                uuids.remove(packet.getUUID());
                ids.add(packet.getId());
            }
        } else if (event.getPacket() instanceof ClientboundRemoveEntitiesPacket packet) {
            for (Integer entityId : packet.getEntityIds()) {
                ids.remove(entityId);
            }
        }
    }

}