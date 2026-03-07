package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.spi.FileSystemProvider;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Jimfs {
    public static final String URI_SCHEME = "jimfs";
    private static final Logger LOGGER = Logger.getLogger(Jimfs.class.getName());
    static final @Nullable FileSystemProvider systemProvider = Jimfs.getSystemJimfsProvider();

    private Jimfs() {
    }

    public static FileSystem newFileSystem() {
        return Jimfs.newFileSystem(Jimfs.newRandomFileSystemName());
    }

    public static FileSystem newFileSystem(String name) {
        return Jimfs.newFileSystem(name, Configuration.forCurrentPlatform());
    }

    public static FileSystem newFileSystem(Configuration configuration) {
        return Jimfs.newFileSystem(Jimfs.newRandomFileSystemName(), configuration);
    }

    public static FileSystem newFileSystem(String name, Configuration configuration) {
        try {
            URI uri = new URI(URI_SCHEME, name, null, null);
            return Jimfs.newFileSystem(uri, configuration);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @VisibleForTesting
    static FileSystem newFileSystem(URI uri, Configuration config) {
        Preconditions.checkArgument(URI_SCHEME.equals(uri.getScheme()), "uri (%s) must have scheme %s", (Object) uri, (Object) URI_SCHEME);
        try {
            JimfsFileSystem fileSystem = JimfsFileSystems.newFileSystem(JimfsFileSystemProvider.instance(), uri, config);
            try {
                ImmutableMap<String, JimfsFileSystem> env = ImmutableMap.of("fileSystem", fileSystem);
                FileSystems.newFileSystem(uri, env, SystemJimfsFileSystemProvider.class.getClassLoader());
            } catch (ProviderNotFoundException | ServiceConfigurationError throwable) {
                // empty catch block
            }
            return fileSystem;
        } catch (IOException e) {
            throw new AssertionError((Object) e);
        }
    }

    private static @Nullable FileSystemProvider getSystemJimfsProvider() {
        try {
            for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
                if (!provider.getScheme().equals(URI_SCHEME)) continue;
                return provider;
            }
            ServiceLoader<FileSystemProvider> loader = ServiceLoader.load(FileSystemProvider.class, SystemJimfsFileSystemProvider.class.getClassLoader());
            for (FileSystemProvider provider : loader) {
                if (!provider.getScheme().equals(URI_SCHEME)) continue;
                return provider;
            }
        } catch (ProviderNotFoundException | ServiceConfigurationError e) {
            LOGGER.log(Level.INFO, "An exception occurred when attempting to find the system-loaded FileSystemProvider for Jimfs. This likely means that your environment does not support loading services via ServiceLoader or is not configured correctly. This does not prevent using Jimfs, but it will mean that methods that look up via URI such as Paths.get(URI) cannot work.", e);
        }
        return null;
    }

    private static String newRandomFileSystemName() {
        return UUID.randomUUID().toString();
    }
}
