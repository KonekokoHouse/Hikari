package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public final class Handler
        extends URLStreamHandler {
    private static final String JAVA_PROTOCOL_HANDLER_PACKAGES = "java.protocol.handler.pkgs";

    static void register() {
        Handler.register(Handler.class);
    }

    static void register(Class<? extends URLStreamHandler> handlerClass) {
        Preconditions.checkArgument("Handler".equals(handlerClass.getSimpleName()));
        String pkg = handlerClass.getPackage().getName();
        int lastDot = pkg.lastIndexOf(46);
        Preconditions.checkArgument(lastDot > 0, "package for Handler (%s) must have a parent package", (Object) pkg);
        String parentPackage = pkg.substring(0, lastDot);
        String packages = System.getProperty(JAVA_PROTOCOL_HANDLER_PACKAGES);
        packages = packages == null ? parentPackage : packages + "|" + parentPackage;
        System.setProperty(JAVA_PROTOCOL_HANDLER_PACKAGES, packages);
    }

    @Deprecated
    public Handler() {
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new PathURLConnection(url);
    }

    @Override
    protected @Nullable InetAddress getHostAddress(URL url) {
        return null;
    }
}
