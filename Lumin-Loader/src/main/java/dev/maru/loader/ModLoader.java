package dev.maru.loader;

import com.mojang.logging.LogUtils;
import dev.maru.verify.LoaderWindow;
import dev.maru.verify.VerificationClient;
import dev.maru.verify.client.IRCHandler;
import dev.maru.verify.client.IRCTransport;
import dev.maru.verify.packet.implemention.c2s.RequestModC2S;
import dev.maru.verify.util.HwidUtil;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import niurendeobf.ZKMIndy;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ZKMIndy
public class ModLoader implements IModFileCandidateLocator {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static byte[] modData = null;
    private static final CountDownLatch downloadLatch = new CountDownLatch(1);

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        LOGGER.info("Lumin: ModLoader started. Verifying user...");
        // 1. Verify user first
        LoaderWindow.verifyOrExitBlocking();

        try {
            LOGGER.info("Lumin: User verified. Starting mod download...");
            
            // Connect to verification server
            VerificationClient.connect(new IRCHandler() {
                @Override
                public void onConnected() {
                    // This might not be called if connection is already established
                    // So we also check in main thread or rely on getTransport
                }

                @Override
                public void onDisconnected(String message) {
                    LOGGER.error("Lumin: Disconnected from server: {}", message);
                    downloadLatch.countDown(); // Unblock if disconnected
                }

                @Override
                public void onMessage(String sender, String message) {
                    // Ignore chat
                }

                @Override
                public String getInGameUsername() {
                    return "LuminLoader";
                }

                @Override
                public void onModDownload(String content, String hash) {
                    LOGGER.info("Lumin: Received mod data. Size: {}", content.length());
                    try {
                        modData = Base64.getDecoder().decode(content);
                    } catch (Exception e) {
                        LOGGER.error("Lumin: Failed to decode mod data", e);
                    } finally {
                        downloadLatch.countDown();
                    }
                }
            });

            // 2. Send request manually if connected (which should be true after verifyOrExitBlocking)
            IRCTransport transport = VerificationClient.getTransport();
            if (transport != null && !transport.isClosed()) {
                LOGGER.info("Lumin: Connection active. Sending request...");
                transport.sendPacket(new RequestModC2S(HwidUtil.getHWID()));
            } else {
                LOGGER.error("Lumin: Connection lost after verification!");
                // Fallback: try to reconnect? Or just fail.
                // VerificationClient.connect(...) above might trigger onConnected, but let's be safe
                // Wait a bit to see if onConnected fires from the connect() call above?
                // But connect() reuses existing transport if open.
            }

            // Wait for download
            LOGGER.info("Lumin: Waiting for mod download...");
            if (!downloadLatch.await(60, TimeUnit.SECONDS)) {
                LOGGER.error("Lumin: Mod download timed out.");
            }

            // Load into memory
            if (modData != null) {
                LOGGER.info("Lumin: Loading mod into memory...");
                Path memoryRoot = MemoryJarUtil.loadJarToMemoryFileSystem(modData);
                var result = pipeline.addPath(memoryRoot, ModFileDiscoveryAttributes.DEFAULT.withLocator(this), IncompatibleFileReporting.WARN_ALWAYS);
                if (result.isPresent()) {
                    LOGGER.info("Lumin: Mod loaded successfully: {}", result.get().getFileName());
                } else {
                    LOGGER.error("Lumin: Failed to add mod to pipeline! (NeoForge rejected it)");
                }
            } else {
                LOGGER.error("Lumin: No mod data received.");
            }

        } catch (Exception e) {
            LOGGER.error("Lumin: Fatal error in ModLoader", e);
        }
    }

}
