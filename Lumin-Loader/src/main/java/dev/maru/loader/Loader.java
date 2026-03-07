package dev.maru.loader;

import by.radioegor146.nativeobfuscator.Native;
import dev.maru.verify.AuthState;
import dev.maru.verify.VerificationClient;
import dev.maru.verify.client.IRCHandler;
import dev.maru.verify.client.IRCTransport;
import dev.maru.verify.packet.implemention.c2s.RequestModC2S;
import dev.maru.verify.util.AuthUtil;
import dev.maru.verify.util.ExitUtil;
import dev.maru.verify.util.HwidUtil;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import niurendeobf.ZKMIndy;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ZKMIndy
@Native
public class Loader {

    private static byte[] modData = null;
    private static final CountDownLatch downloadLatch = new CountDownLatch(1);

    static {
        String pkgs = System.getProperty("java.protocol.handler.pkgs", "");
        if (!pkgs.contains("dev.maru.loader.protocols")) {
            if (!pkgs.isEmpty()) {
                pkgs += "|";
            }
            pkgs += "dev.maru.loader.protocols";
            System.setProperty("java.protocol.handler.pkgs", pkgs);
        }
    }

    public static void load(IDiscoveryPipeline pipeline, ModLoader modLoader) {
        LoaderWindow.verifyOrExitBlocking();

        ExitUtil.ensureVerifiedOrExit();

        if (!AuthState.isAuthed() || AuthState.getExpireAt() <= System.currentTimeMillis()) {
            AuthState.clear();
            ExitUtil.exit0();
            return;
        }

        try {
            VerificationClient.connect(new IRCHandler() {
                @Override
                public void onMessage(String sender, String message) {

                }

                @Override
                public void onDisconnected(String message) {
                    downloadLatch.countDown();
                }

                @Override
                public void onConnected() {

                }

                @Override
                public String getInGameUsername() {
                    return "";
                }

                @Override
                public void onModDownload(String content, String hash) {
                    try {
                        modData = Base64.getDecoder().decode(content);
                    } catch (Exception ignored) {
                    } finally {
                        downloadLatch.countDown();
                    }
                }
            });

            IRCTransport transport = VerificationClient.getTransport();
            if (transport != null && !transport.isClosed() && AuthUtil.authed.get().length() == 32) {
                String name = LoaderWindow.ModSelection.name;
                String version = LoaderWindow.ModSelection.version;
                transport.sendPacket(new RequestModC2S(HwidUtil.getHWID(), name, version));
            } else if (transport == null || AuthUtil.authed.get().length() != 32) {
                try {
                    Class<?> System = Loader.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                    Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                    exit.invoke(null, 0);
                } catch (Exception ignored) {
                }
            }

            downloadLatch.await(60, TimeUnit.SECONDS);

            if (modData != null) {
                try {
                    Path memoryRoot = MemoryJarUtil.loadJarToMemoryFileSystem(modData);

                    String name = LoaderWindow.ModSelection.name;
                    String version = LoaderWindow.ModSelection.version;
                    if (name != null && version != null) {
                        String filename = (name + "_" + version + ".zip").replaceAll("[\\\\/:*?\"<>|]", "");
                        File assetFile = new File(System.getProperty("user.home"), "Lumin/assets/" + filename);
                        if (assetFile.exists()) {
                            MemoryJarUtil.mergeZip(memoryRoot, assetFile);
                        }
                    }

                    pipeline.addPath(memoryRoot, ModFileDiscoveryAttributes.DEFAULT.withLocator(modLoader), IncompatibleFileReporting.WARN_ALWAYS);
                } finally {
                    Arrays.fill(modData, (byte) 0);
                    modData = null;
                }
            }
        } catch (Exception ignored) {
        }
    }

}
