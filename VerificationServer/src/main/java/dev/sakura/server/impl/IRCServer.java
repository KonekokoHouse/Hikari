package dev.sakura.server.impl;

import dev.sakura.server.impl.auth.AuthService;
import dev.sakura.server.impl.cli.CommandConsole;
import dev.sakura.server.impl.handler.HandlerManager;
import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.storage.CardRepository;
import dev.sakura.server.impl.storage.SqliteDatabase;
import dev.sakura.server.impl.storage.UserRepository;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.implemention.c2s.HandshakeC2S;
import dev.sakura.server.packet.implemention.c2s.LoginC2S;
import dev.sakura.server.packet.implemention.c2s.RegisterC2S;
import dev.sakura.server.packet.implemention.s2c.DisconnectS2C;
import dev.sakura.server.packet.implemention.s2c.MessageS2C;
import dev.sakura.server.processor.IRCProtocol;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;
import org.tinylog.Logger;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IRCServer {
    private static IRCServer instance;
    private static final int DEFAULT_PORT = 57444;

    private final int port;
    private final long startedAt;
    private final UserManager userManager;
    private final SqliteDatabase database;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final AuthService authService;
    private final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final Thread consoleThread;
    private final AioQuickServer aioServer;

    public static void main(String[] args) throws Exception {
        int requestedPort = parsePort(args);
        int port = resolvePort(requestedPort);
        IRCServer server = new IRCServer(port);
        SwingUtilities.invokeLater(() -> dev.sakura.server.impl.gui.AdminApp.start(server));
    }

    private static int parsePort(String[] args) {
        if (args == null || args.length == 0) {
            return DEFAULT_PORT;
        }

        Integer candidate = null;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a == null || a.isEmpty()) {
                continue;
            }

            if (a.startsWith("--port=")) {
                candidate = tryParsePort(a.substring("--port=".length()));
                break;
            }

            if (a.equals("--port") || a.equals("-p")) {
                if (i + 1 < args.length) {
                    candidate = tryParsePort(args[i + 1]);
                    break;
                }
            }

            candidate = tryParsePort(a);
            if (candidate != null) {
                break;
            }
        }

        if (candidate == null) {
            return DEFAULT_PORT;
        }

        if (candidate < 1 || candidate > 65535) {
            Logger.warn("Invalid port: {}. Using default {}.", candidate, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
        return candidate;
    }

    private static int resolvePort(int requestedPort) {
        if (requestedPort < 1 || requestedPort > 65535) {
            return DEFAULT_PORT;
        }
        try (ServerSocket ignored = new ServerSocket(requestedPort)) {
            return requestedPort;
        } catch (BindException e) {
            try (ServerSocket s = new ServerSocket(0)) {
                int picked = s.getLocalPort();
                Logger.warn("Port {} is in use, falling back to {}.", requestedPort, picked);
                return picked;
            } catch (IOException ignored) {
                Logger.warn("Port {} is in use, falling back to default {}.", requestedPort, DEFAULT_PORT);
                return DEFAULT_PORT;
            }
        } catch (IOException ignored) {
            return requestedPort;
        }
    }

    private static Integer tryParsePort(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public IRCServer(int port) throws Exception {
        instance = this;
        this.port = port;
        this.startedAt = System.currentTimeMillis();
        Logger.info("Starting impl...");

        database = new SqliteDatabase(Paths.get(System.getProperty("sakura.verify.db", "verify.sqlite")));
        userRepository = new UserRepository();
        cardRepository = new CardRepository();
        authService = new AuthService(database, userRepository, cardRepository);
        initStorage();

        userManager = new UserManager();
        HandlerManager handlerManager = new HandlerManager();
        IRCProtocol protocol = new IRCProtocol();

        MessageProcessor<IRCPacket> processor = new MessageProcessor<>() {
            @Override
            public void process(AioSession session, IRCPacket packet) {
                String sessionKey = String.valueOf(session.getSessionID());
                WriteBuffer outputStream = session.writeBuffer();
                Connection connection = connectionMap.computeIfAbsent(sessionKey, ignored -> new Connection() {
                    @Override
                    public void sendPacket(IRCPacket packet) {
                        try {
                            byte[] bytes = protocol.encode(packet);
                            outputStream.writeInt(bytes.length);
                            outputStream.write(bytes);
                        } catch (Exception e) {
                            Logger.error("Error while sending packet");
                            Logger.error(e);
                        }
                    }

                    @Override
                    public String getIPAddress() {
                        try {
                            return session.getRemoteAddress().getHostString();
                        } catch (IOException e) {
                            Logger.error("Error while reading address");
                            Logger.error(e);
                            return "";
                        }
                    }

                    @Override
                    public String getSessionId() {
                        return sessionKey;
                    }

                    @Override
                    public void disconnect(String message) {
                        sendPacket(new DisconnectS2C(message));
                        disconnect();
                    }

                    @Override
                    public void disconnect() {
                        session.close();
                    }
                });

                User user = userManager.getUser(sessionKey);
                if (user == null && !(packet instanceof LoginC2S) && !(packet instanceof RegisterC2S) && !(packet instanceof HandshakeC2S)) {
                    connection.disconnect("请先登录或注册");
                    return;
                }

                if (user != null || handlerManager.allowNull(packet)) {
                    handlerManager.handlePacket(packet, connection, userManager, user);
                }
            }

            @Override
            public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (stateMachineEnum == StateMachineEnum.SESSION_CLOSED) {
                    String sessionKey = String.valueOf(session.getSessionID());
                    connectionMap.remove(sessionKey);
                    User user = userManager.getUser(sessionKey);
                    if (user != null) {
                        userManager.removeUser(sessionKey);
                        authService.setUserOnline(user.getUsername(), false);
                        Logger.info("User {} disconnected.", user.getUsername());
                    }
                }
            }
        };

        aioServer = new AioQuickServer(port, protocol, processor);
        try {
            Method m = aioServer.getClass().getMethod("setReadBufferSize", int.class);
            m.invoke(aioServer, 8 * 1024 * 1024 + Integer.BYTES);
        } catch (Exception ignored) {
        }
        aioServer.setBannerEnabled(false);
        aioServer.start();

        consoleThread = new Thread(new CommandConsole(database, userRepository, cardRepository), "command-console");
        consoleThread.setDaemon(true);
        consoleThread.start();

        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = this::sendInGameUsername;
        scheduler.scheduleAtFixedRate(task, 5, 5, TimeUnit.SECONDS);

        Logger.info("Server started on port {}!", port);
    }

    private void initStorage() throws SQLException {
        database.initSchema();
        int reset = database.resetAllOnline();
        Logger.info("Reset {} users online status.", reset);

        try (java.sql.Connection connection = database.openConnection()) {
            int backfill = userRepository.backfillQqIndex(connection);
            Logger.info("Backfilled {} user_qq rows.", backfill);
        }
    }

    public void boardCastMessage(MessageS2C packet) {
        for (Connection value : connectionMap.values()) {
            value.sendPacket(packet);
        }
    }

    public void sendInGameUsername() {
        for (Connection value : connectionMap.values()) {
            userManager.syncUserList(value);
        }
    }

    public static IRCServer getInstance() {
        return instance;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public SqliteDatabase getDatabase() {
        return database;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public CardRepository getCardRepository() {
        return cardRepository;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public int getPort() {
        return port;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public int getConnectionCount() {
        return connectionMap.size();
    }

    public Map<String, Connection> getConnectionSnapshot() {
        return Map.copyOf(connectionMap);
    }

    public void shutdown() {
        try {
            scheduler.shutdownNow();
        } catch (Exception ignored) {
        }
        tryInvoke(aioServer, "shutdown");
        tryInvoke(aioServer, "stop");
        tryInvoke(aioServer, "close");
    }

    private static void tryInvoke(Object target, String methodName) {
        if (target == null) {
            return;
        }
        try {
            Method m = target.getClass().getMethod(methodName);
            m.invoke(target);
        } catch (Exception ignored) {
        }
    }

    public void disconnectUser(String username, String reason) {
        if (username == null || username.isEmpty()) {
            return;
        }

        for (Map.Entry<String, User> entry : userManager.sessionToUserMapEntrySet()) {
            User user = entry.getValue();
            if (user != null && username.equals(user.getUsername())) {
                String sessionId = entry.getKey();
                Connection connection = connectionMap.get(sessionId);
                if (connection != null) {
                    connection.disconnect(reason);
                }
                authService.setUserOnline(username, false);
            }
        }
    }
}
