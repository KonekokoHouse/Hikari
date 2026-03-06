package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.RequestModC2S;
import dev.sakura.server.packet.implemention.s2c.DownloadModS2C;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

public class ModRequestHandler implements PacketHandler<RequestModC2S> {

    private static String cachedModContent = null;
    private static String cachedModHash = null;
    private static long lastModified = 0;
    private static final String MOD_PATH = "mods/secure-mod.jar";

    @Override
    public void handle(RequestModC2S packet, Connection connection, UserManager userManager, User user) {
        // Here you can add verification logic using user or packet.getHwid()
        // For now, we allow anyone to download
        Logger.info("Received mod request from HWID: {}", packet.getHwid());

        try {
            ensureModCached();
            if (cachedModContent != null) {
                connection.sendPacket(new DownloadModS2C(cachedModContent, cachedModHash));
                Logger.info("Sent mod to HWID: {}", packet.getHwid());
            } else {
                Logger.error("Mod file not found or empty at: " + new File(MOD_PATH).getAbsolutePath());
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to process mod request");
        }
    }

    private synchronized void ensureModCached() throws IOException {
        File file = new File(MOD_PATH);
        if (!file.exists()) {
            return;
        }

        if (file.lastModified() > lastModified) {
            Logger.info("Reloading mod file from disk...");
            byte[] bytes = Files.readAllBytes(file.toPath());
            cachedModContent = Base64.getEncoder().encodeToString(bytes);
            cachedModHash = getMD5(bytes);
            lastModified = file.lastModified();
        }
    }

    private String getMD5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public boolean allowNull() {
        return true;
    }
}
