package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.MapMaker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public final class SystemJimfsFileSystemProvider
        extends FileSystemProvider {
    static final String FILE_SYSTEM_KEY = "fileSystem";
    private static final ConcurrentMap<URI, FileSystem> fileSystems = new MapMaker().weakValues().makeMap();

    @Deprecated
    public SystemJimfsFileSystemProvider() {
    }

    @Override
    public String getScheme() {
        return "protocol";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        Preconditions.checkArgument(uri.getScheme().equalsIgnoreCase("protocol"), "uri (%s) scheme must be '%s'", (Object) uri, (Object) "protocol");
        Preconditions.checkArgument(SystemJimfsFileSystemProvider.isValidFileSystemUri(uri), "uri (%s) may not have a path, query or fragment", (Object) uri);
        Preconditions.checkArgument(env.get(FILE_SYSTEM_KEY) instanceof FileSystem, "env map (%s) must contain key '%s' mapped to an instance of %s", env, (Object) FILE_SYSTEM_KEY, FileSystem.class);
        FileSystem fileSystem = (FileSystem) env.get(FILE_SYSTEM_KEY);
        if (fileSystems.putIfAbsent(uri, fileSystem) != null) {
            throw new FileSystemAlreadyExistsException(uri.toString());
        }
        return fileSystem;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        FileSystem fileSystem = (FileSystem) fileSystems.get(uri);
        if (fileSystem == null) {
            throw new FileSystemNotFoundException(uri.toString());
        }
        return fileSystem;
    }

    @Override
    public Path getPath(URI uri) {
        Preconditions.checkArgument("protocol".equalsIgnoreCase(uri.getScheme()), "uri scheme does not match this provider: %s", (Object) uri);
        String path = uri.getPath();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "uri must have a path: %s", (Object) uri);
        return SystemJimfsFileSystemProvider.toPath(this.getFileSystem(SystemJimfsFileSystemProvider.toFileSystemUri(uri)), uri);
    }

    private static boolean isValidFileSystemUri(URI uri) {
        return Strings.isNullOrEmpty(uri.getPath()) && Strings.isNullOrEmpty(uri.getQuery()) && Strings.isNullOrEmpty(uri.getFragment());
    }

    private static URI toFileSystemUri(URI uri) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            throw new AssertionError((Object) e);
        }
    }

    private static Path toPath(FileSystem fileSystem, URI uri) {
        try {
            Method toPath = fileSystem.getClass().getDeclaredMethod("toPath", URI.class);
            return (Path) toPath.invoke((Object) fileSystem, uri);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("invalid file system: " + fileSystem, e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        FileSystemProvider realProvider = path.getFileSystem().provider();
        return realProvider.newFileSystem(path, env);
    }

    public static Runnable removeFileSystemRunnable(final URI uri) {
        return new Runnable() {

            @Override
            public void run() {
                fileSystems.remove(uri);
            }
        };
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }
}
