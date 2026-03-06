package dev.maru.verify.management;

import com.google.gson.JsonObject;
import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.implemention.c2s.*;
import dev.maru.verify.packet.implemention.s2c.*;
import niurendeobf.ZKMIndy;

import java.util.HashMap;
import java.util.Map;

@ZKMIndy
public class PacketManager {
    private final Map<Integer, Class<? extends IRCPacket>> idToPacketMap = new HashMap<>();
    private final Map<Class<? extends IRCPacket>, Integer> packetToIdMap = new HashMap<>();
    private int id;

    public PacketManager() {
        register(DisconnectS2C.class, ConnectedS2C.class, UpdateUserListS2C.class, MessageS2C.class);

        register(HandshakeC2S.class, UpdateIgnC2S.class, MessageC2S.class);

        register(LoginResultS2C.class, RegisterResultS2C.class, RechargeResultS2C.class);
        register(LoginC2S.class, RegisterC2S.class, RechargeC2S.class);

        register(CloudConfigS2C.class);
        register(CloudConfigC2S.class);

        register(RequestModC2S.class);
        register(DownloadModS2C.class);
    }

    @SafeVarargs
    private void register(Class<? extends IRCPacket>... classes) {
        for (Class<? extends IRCPacket> clazz : classes) {
            idToPacketMap.put(id, clazz);
            packetToIdMap.put(clazz, id);
            id++;
        }
    }

    public IRCPacket readPacket(JsonObject object) {
        if (object.has("id") && object.has("cxt")) {
            int id = object.get("id").getAsInt();
            IRCPacket packet = create(id);
            packet.readPacket(object.get("cxt").getAsJsonObject());
            return packet;
        }
        throw new RuntimeException("Unknown packet");
    }

    public JsonObject writePacket(IRCPacket packet) {
        JsonObject jsonObject = new JsonObject();
        JsonObject packetJson = packet.writePacket();
        jsonObject.addProperty("id", packetToIdMap.get(packet.getClass()));
        jsonObject.add("cxt", packetJson);
        return jsonObject;
    }

    public IRCPacket create(int id) {
        Class<? extends IRCPacket> clazz = idToPacketMap.get(id);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown packet: " + id);
        }
        return create(clazz);
    }

    public IRCPacket create(Class<? extends IRCPacket> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

