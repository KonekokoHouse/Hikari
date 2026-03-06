package dev.sakura.server.impl.handler;

import dev.sakura.server.impl.handler.implemention.*;
import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.implemention.c2s.*;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;

public class HandlerManager {
    private final Map<Class<? extends IRCPacket>, PacketHandler<?>> classToHandlerMap = new HashMap<>();

    public HandlerManager() {
        classToHandlerMap.put(HandshakeC2S.class, new HandshakeHandler());
        classToHandlerMap.put(UpdateIgnC2S.class, new UpdateIGNHandler());
        classToHandlerMap.put(MessageC2S.class, new MessageHandler());
        classToHandlerMap.put(LoginC2S.class, new LoginHandler());
        classToHandlerMap.put(RegisterC2S.class, new RegisterHandler());
        classToHandlerMap.put(RechargeC2S.class, new RechargeHandler());
        classToHandlerMap.put(CloudConfigC2S.class, new CloudConfigHandler());
        classToHandlerMap.put(RequestModC2S.class, new ModRequestHandler());
    }

    public boolean allowNull(IRCPacket packet) {
        if (!classToHandlerMap.containsKey(packet.getClass())) {
            Logger.warn("No {} handler found.", packet.getClass().getSimpleName());
            return false;
        }
        return classToHandlerMap.get(packet.getClass()).allowNull();
    }

    @SuppressWarnings("unchecked")
    public void handlePacket(IRCPacket packet, Connection connection, UserManager userManager, User user) {
        PacketHandler<IRCPacket> handler = (PacketHandler<IRCPacket>) classToHandlerMap.get(packet.getClass());
        if (handler == null) {
            Logger.warn("No {} handler found.", packet.getClass().getSimpleName());
            return;
        }
        handler.handle(packet, connection, userManager, user);
    }
}

