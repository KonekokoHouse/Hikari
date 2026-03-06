package dev.maru.verify.client;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.implemention.c2s.*;
import dev.maru.verify.packet.implemention.s2c.*;
import dev.maru.verify.protocol.IRCProtocol;
import niurendeobf.ZKMIndy;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ZKMIndy
public class IRCTransport {
    private final IRCProtocol protocol = new IRCProtocol();
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Object writeLock = new Object();
    private final Map<String, String> userToIgnMap = new ConcurrentHashMap<>();
    private final Map<String, String> ignToUserMap = new ConcurrentHashMap<>();
    private final AtomicBoolean disconnectedNotified = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile IRCHandler handler;
    private volatile ScheduledExecutorService scheduler;

    public IRCTransport(String host, int port, IRCHandler handler) throws IOException {
        this.handler = handler;
        this.socket = new Socket(host, port);
        this.socket.setTcpNoDelay(true);
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 1024 * 64));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 1024 * 64));
        startReaderThread();
    }

    private void startReaderThread() {
        Thread t = new Thread(() -> {
            try {
                while (!closed.get()) {
                    int length;
                    try {
                        length = in.readInt();
                    } catch (EOFException eof) {
                        notifyDisconnected("连接已断开");
                        return;
                    }
                    if (length < 0 || length > protocol.getMaxFrameLength()) {
                        notifyDisconnected("协议错误");
                        return;
                    }
                    byte[] payload = new byte[length];
                    in.readFully(payload);
                    IRCPacket msg;
                    try {
                        msg = protocol.decode(payload);
                    } catch (Exception e) {
                        notifyDisconnected("协议解析失败");
                        return;
                    }
                    processPacket(msg);
                }
            } catch (IOException e) {
                notifyDisconnected("连接已断开");
            } finally {
                closeQuietly();
            }
        }, "Sakura-Verify-Reader");
        t.setDaemon(true);
        t.start();
    }

    private void processPacket(IRCPacket msg) {
        if (msg instanceof DisconnectS2C p) {
            notifyDisconnected(p.getReason());
            return;
        }
        if (msg instanceof ConnectedS2C) {
            disconnectedNotified.set(false);
            IRCHandler h = handler;
            if (h != null) {
                h.onConnected();
            }
            startScheduler();
            return;
        }
        if (msg instanceof UpdateUserListS2C p) {
            userToIgnMap.clear();
            Map<String, String> m = p.getUserMap();
            if (m != null) {
                userToIgnMap.putAll(m);
            }
            ignToUserMap.clear();
            userToIgnMap.forEach((user, ign) -> {
                if (ign != null) {
                    ignToUserMap.put(ign, user);
                }
            });
            return;
        }
        if (msg instanceof MessageS2C p) {
            IRCHandler h = handler;
            if (h != null) {
                h.onMessage(p.getSender(), p.getMessage());
            }
            return;
        }
        if (msg instanceof LoginResultS2C p) {
            IRCHandler h = handler;
            if (h != null) {
                h.onLoginResult(p.isSuccess(), p.getExpireAt(), p.getTimeWindow(), p.getMessage());
            }
            return;
        }
        if (msg instanceof RegisterResultS2C p) {
            IRCHandler h = handler;
            if (h != null) {
                h.onRegisterResult(p.isSuccess(), p.getExpireAt(), p.getTimeWindow(), p.getMessage());
            }
            return;
        }
        if (msg instanceof RechargeResultS2C p) {
            IRCHandler h = handler;
            if (h != null) {
                h.onRechargeResult(p.isSuccess(), p.getExpireAt(), p.getTimeWindow(), p.getMessage());
            }
            return;
        }
        if (msg instanceof CloudConfigS2C p) {
            IRCHandler h = handler;
            if (h == null) {
                return;
            }
            String action = p.getAction();
            if (action == null) {
                return;
            }
            if (action.equalsIgnoreCase("upload")) {
                h.onCloudConfigUploadResult(p.isSuccess(), p.getMessage(), p.getMax());
                return;
            }
            if (action.equalsIgnoreCase("get")) {
                h.onCloudConfigGetResult(p.isSuccess(), p.getOwner(), p.getName(), p.getContent(), p.getMessage());
                return;
            }
            if (action.equalsIgnoreCase("delete")) {
                h.onCloudConfigDeleteResult(p.isSuccess(), p.getOwner(), p.getName(), p.getMessage());
                return;
            }
            if (action.equalsIgnoreCase("list")) {
                h.onCloudConfigListResult(p.isSuccess(), p.getNames(), p.getMax(), p.getMessage());
            }
        }
        if (msg instanceof DownloadModS2C p) {
            IRCHandler h = handler;
            if (h != null) {
                h.onModDownload(p.getContent(), p.getHash());
            }
            return;
        }
    }

    private void notifyDisconnected(String message) {
        if (!disconnectedNotified.compareAndSet(false, true)) {
            return;
        }
        stopScheduler();
        IRCHandler h = handler;
        if (h != null) {
            h.onDisconnected(message);
        }
        close();
    }

    public void close() {
        closed.set(true);
        closeQuietly();
        stopScheduler();
    }

    public boolean isClosed() {
        return closed.get()
                || socket.isClosed()
                || socket.isInputShutdown()
                || socket.isOutputShutdown();
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }

    public void sendPacket(IRCPacket packet) {
        try {
            byte[] data = protocol.encode(packet);
            synchronized (writeLock) {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
            }
        } catch (Exception e) {
            notifyDisconnected("发送失败");
            close();
        }
    }

    public boolean isUser(String name) {
        return ignToUserMap.containsKey(name);
    }

    public String getName(String ign) {
        return ignToUserMap.get(ign);
    }

    public String getIgn(String name) {
        return userToIgnMap.get(name);
    }

    public void sendChat(String message) {
        sendPacket(new MessageC2S(message));
    }

    public void sendInGameUsername(String username) {
        sendPacket(new UpdateIgnC2S(username));
    }

    public void sendInGameUsername() {
        IRCHandler h = handler;
        if (h != null) {
            sendInGameUsername(h.getInGameUsername());
        }
    }

    public void connect(String username, String token) {
        sendPacket(new HandshakeC2S(username, token));
    }

    public void login(String username, String password, String hwid, Set<String> qqSet, String phone) {
        sendPacket(new LoginC2S(username, password, hwid, qqSet, phone));
    }

    public void register(String username, String password, String hwid, Set<String> qqSet, String phone, String cardKey) {
        sendPacket(new RegisterC2S(username, password, hwid, qqSet, phone, cardKey));
    }

    public void recharge(String username, String cardKey) {
        sendPacket(new RechargeC2S(username, cardKey));
    }

    public void uploadCloudConfig(String name, String content) {
        sendPacket(new CloudConfigC2S("upload", "", name, content));
    }

    public void getCloudConfig(String name) {
        sendPacket(new CloudConfigC2S("get", "", name, ""));
    }

    public void getCloudConfig(String owner, String name) {
        sendPacket(new CloudConfigC2S("get", owner, name, ""));
    }

    public void listCloudConfigs() {
        sendPacket(new CloudConfigC2S("list", "", "", ""));
    }

    public void deleteCloudConfig(String name) {
        deleteCloudConfig("", name);
    }

    public void deleteCloudConfig(String owner, String name) {
        sendPacket(new CloudConfigC2S("delete", owner, name, ""));
    }

    public void setHandler(IRCHandler handler) {
        this.handler = handler;
    }

    private void startScheduler() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = this::sendInGameUsername;
        scheduler.scheduleAtFixedRate(task, 5, 5, TimeUnit.SECONDS);
    }

    private void stopScheduler() {
        ScheduledExecutorService s = scheduler;
        scheduler = null;
        if (s != null) {
            s.shutdownNow();
        }
    }
}
