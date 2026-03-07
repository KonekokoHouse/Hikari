package dev.maru.verify;

import by.radioegor146.nativeobfuscator.Native;
import dev.maru.verify.client.IRCHandler;
import dev.maru.verify.client.IRCTransport;
import dev.maru.verify.client.MultiplexIRCHandler;
import niurendeobf.ZKMIndy;

import java.io.IOException;

@Native
@ZKMIndy
public final class VerificationClient {
    private static volatile IRCTransport transport;
    private static final MultiplexIRCHandler multiplexHandler = new MultiplexIRCHandler();
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 57444;

    private VerificationClient() {
    }

    public static String getHost() {
        return HOST;
    }

    public static int getPort() {
        return PORT;
    }

    public static synchronized IRCTransport connect(IRCHandler handler) throws IOException {
        if (transport != null) {
            if (transport.isClosed()) {
                transport = null;
            } else {
                if (handler != null) {
                    multiplexHandler.add(handler);
                }
                transport.setHandler(multiplexHandler);
                return transport;
            }
        }
        if (handler != null) {
            multiplexHandler.add(handler);
        }
        transport = new IRCTransport(getHost(), getPort(), multiplexHandler);
        return transport;
    }

    public static synchronized IRCTransport getTransport() {
        if (transport != null && transport.isClosed()) {
            transport = null;
        }
        return transport;
    }

    public static synchronized void addHandler(IRCHandler handler) {
        if (handler == null) {
            return;
        }
        multiplexHandler.add(handler);
        if (transport != null) {
            transport.setHandler(multiplexHandler);
        }
    }

    public static synchronized void shutdown() {
        if (transport != null) {
            transport.close();
            transport = null;
        }
    }
}
