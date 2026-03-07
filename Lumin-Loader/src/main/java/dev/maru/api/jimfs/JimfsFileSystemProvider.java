package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

final class JimfsFileSystemProvider
        extends FileSystemProvider {
    private static final JimfsFileSystemProvider INSTANCE = new JimfsFileSystemProvider();
    private static final FileAttribute<?>[] NO_ATTRS;

    JimfsFileSystemProvider() {
    }

    static JimfsFileSystemProvider instance() {
        return INSTANCE;
    }

    @Override
    public String getScheme() {
        return "protocol";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException("This method should not be called directly;use an overload of Jimfs.newFileSystem() to create a FileSystem.");
    }

    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        Preconditions.checkNotNull(env);
        URI pathUri = checkedPath.toUri();
        URI jarUri = URI.create("jar:" + pathUri);
        try {
            return FileSystems.newFileSystem(jarUri, env);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        throw new UnsupportedOperationException("This method should not be called directly; use FileSystems.getFileSystem(URI) instead.");
    }

    private static JimfsFileSystem getFileSystem(Path path) {
        return (JimfsFileSystem) JimfsFileSystemProvider.checkPath(path).getFileSystem();
    }

    @Override
    public Path getPath(URI uri) {
        throw new UnsupportedOperationException("This method should not be called directly; use Paths.get(URI) instead.");
    }

    private static JimfsPath checkPath(Path path) {
        if (path instanceof JimfsPath) {
            return (JimfsPath) path;
        }
        throw new ProviderMismatchException("path " + path + " is not associated with a Jimfs file system");
    }

    private static FileSystemView getDefaultView(JimfsPath path) {
        return JimfsFileSystemProvider.getFileSystem(path).getDefaultView();
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        if (!checkedPath.getJimfsFileSystem().getFileStore().supportsFeature(Feature.FILE_CHANNEL)) {
            throw new UnsupportedOperationException();
        }
        return this.newJimfsFileChannel(checkedPath, options, attrs);
    }

    private JimfsFileChannel newJimfsFileChannel(JimfsPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        ImmutableSet<OpenOption> opts = Options.getOptionsForChannel(options);
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(path);
        RegularFile file = view.getOrCreateRegularFile(path, opts, attrs);
        return new JimfsFileChannel(file, opts, view.state());
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        JimfsFileChannel channel = this.newJimfsFileChannel(checkedPath, options, attrs);
        return checkedPath.getJimfsFileSystem().getFileStore().supportsFeature(Feature.FILE_CHANNEL) ? channel : new DowngradedSeekableByteChannel(channel);
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options, @Nullable ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
        JimfsFileChannel channel = (JimfsFileChannel) this.newFileChannel(path, options, attrs);
        if (executor == null) {
            JimfsFileSystem fileSystem = (JimfsFileSystem) path.getFileSystem();
            executor = fileSystem.getDefaultThreadPool();
        }
        return channel.asAsynchronousFileChannel(executor);
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        ImmutableSet<OpenOption> opts = Options.getOptionsForInputStream(options);
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(checkedPath);
        RegularFile file = view.getOrCreateRegularFile(checkedPath, opts, NO_ATTRS);
        return new JimfsInputStream(file, view.state());
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        ImmutableSet<OpenOption> opts = Options.getOptionsForOutputStream(options);
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(checkedPath);
        RegularFile file = view.getOrCreateRegularFile(checkedPath, opts, NO_ATTRS);
        return new JimfsOutputStream(file, opts.contains(StandardOpenOption.APPEND), view.state());
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(dir);
        return JimfsFileSystemProvider.getDefaultView(checkedPath).newDirectoryStream(checkedPath, filter, Options.FOLLOW_LINKS, checkedPath);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(dir);
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(checkedPath);
        view.createDirectory(checkedPath, attrs);
    }

    @Override
    public void createLink(Path link, Path existing) throws IOException {
        JimfsPath linkPath = JimfsFileSystemProvider.checkPath(link);
        JimfsPath existingPath = JimfsFileSystemProvider.checkPath(existing);
        Preconditions.checkArgument(linkPath.getFileSystem().equals(existingPath.getFileSystem()), "link and existing paths must belong to the same file system instance");
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(linkPath);
        view.link(linkPath, JimfsFileSystemProvider.getDefaultView(existingPath), existingPath);
    }

    @Override
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
        JimfsPath linkPath = JimfsFileSystemProvider.checkPath(link);
        JimfsPath targetPath = JimfsFileSystemProvider.checkPath(target);
        Preconditions.checkArgument(linkPath.getFileSystem().equals(targetPath.getFileSystem()), "link and target paths must belong to the same file system instance");
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(linkPath);
        view.createSymbolicLink(linkPath, targetPath, attrs);
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(link);
        return JimfsFileSystemProvider.getDefaultView(checkedPath).readSymbolicLink(checkedPath);
    }

    @Override
    public void delete(Path path) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(checkedPath);
        view.deleteFile(checkedPath, FileSystemView.DeleteMode.ANY);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        this.copy(source, target, Options.getCopyOptions(options), false);
    }

    private void copy(Path source, Path target, ImmutableSet<CopyOption> options, boolean move) throws IOException {
        JimfsPath sourcePath = JimfsFileSystemProvider.checkPath(source);
        JimfsPath targetPath = JimfsFileSystemProvider.checkPath(target);
        FileSystemView sourceView = JimfsFileSystemProvider.getDefaultView(sourcePath);
        FileSystemView targetView = JimfsFileSystemProvider.getDefaultView(targetPath);
        sourceView.copy(sourcePath, targetView, targetPath, options, move);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        this.copy(source, target, Options.getMoveOptions(options), true);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        if (path.equals(path2)) {
            return true;
        }
        if (!(path instanceof JimfsPath) || !(path2 instanceof JimfsPath)) {
            return false;
        }
        JimfsPath checkedPath = (JimfsPath) path;
        JimfsPath checkedPath2 = (JimfsPath) path2;
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(checkedPath);
        FileSystemView view2 = JimfsFileSystemProvider.getDefaultView(checkedPath2);
        return view.isSameFile(checkedPath, view2, checkedPath2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        FileSystemView view = JimfsFileSystemProvider.getDefaultView(checkedPath);
        if (this.getFileStore(path).supportsFileAttributeView("dos")) {
            return view.readAttributes(checkedPath, DosFileAttributes.class, Options.NOFOLLOW_LINKS).isHidden();
        }
        return path.getNameCount() > 0 && path.getFileName().toString().startsWith(".");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return JimfsFileSystemProvider.getFileSystem(path).getFileStore();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        JimfsFileSystemProvider.getDefaultView(checkedPath).checkAccess(checkedPath);
    }

    @Override
    public <V extends FileAttributeView> @Nullable V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        return JimfsFileSystemProvider.getDefaultView(checkedPath).getFileAttributeView(checkedPath, type, Options.getLinkOptions(options));
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        return JimfsFileSystemProvider.getDefaultView(checkedPath).readAttributes(checkedPath, type, Options.getLinkOptions(options));
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        return JimfsFileSystemProvider.getDefaultView(checkedPath).readAttributes(checkedPath, attributes, Options.getLinkOptions(options));
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        JimfsPath checkedPath = JimfsFileSystemProvider.checkPath(path);
        JimfsFileSystemProvider.getDefaultView(checkedPath).setAttribute(checkedPath, attribute, value, Options.getLinkOptions(options));
    }

    static {
        try {
            Handler.register();
        } catch (Throwable throwable) {
            // empty catch block
        }
        NO_ATTRS = new FileAttribute[0];
    }
}
