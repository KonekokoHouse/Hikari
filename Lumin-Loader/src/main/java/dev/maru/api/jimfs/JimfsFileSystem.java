package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class JimfsFileSystem
        extends FileSystem {
    private final JimfsFileSystemProvider provider;
    private final URI uri;
    private final JimfsFileStore fileStore;
    private final PathService pathService;
    private final UserPrincipalLookupService userLookupService = new UserLookupService(true);
    private final FileSystemView defaultView;
    private final WatchServiceConfiguration watchServiceConfig;
    private @Nullable ExecutorService defaultThreadPool;

    JimfsFileSystem(JimfsFileSystemProvider provider, URI uri, JimfsFileStore fileStore, PathService pathService, FileSystemView defaultView, WatchServiceConfiguration watchServiceConfig) {
        this.provider = Preconditions.checkNotNull(provider);
        this.uri = Preconditions.checkNotNull(uri);
        this.fileStore = Preconditions.checkNotNull(fileStore);
        this.pathService = Preconditions.checkNotNull(pathService);
        this.defaultView = Preconditions.checkNotNull(defaultView);
        this.watchServiceConfig = Preconditions.checkNotNull(watchServiceConfig);
    }

    @Override
    public JimfsFileSystemProvider provider() {
        return this.provider;
    }

    public URI getUri() {
        return this.uri;
    }

    public FileSystemView getDefaultView() {
        return this.defaultView;
    }

    @Override
    public String getSeparator() {
        return this.pathService.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        ImmutableSortedSet.Builder<JimfsPath> builder = ImmutableSortedSet.orderedBy(this.pathService);
        for (Name name : this.fileStore.getRootDirectoryNames()) {
            builder.add(this.pathService.createRoot(name));
        }
        return ImmutableList.copyOf(builder.build());
    }

    public JimfsPath getWorkingDirectory() {
        return this.defaultView.getWorkingDirectoryPath();
    }

    @VisibleForTesting
    PathService getPathService() {
        return this.pathService;
    }

    public JimfsFileStore getFileStore() {
        return this.fileStore;
    }

    public ImmutableSet<FileStore> getFileStores() {
        this.fileStore.state().checkOpen();
        return ImmutableSet.of(this.fileStore);
    }

    public ImmutableSet<String> supportedFileAttributeViews() {
        return this.fileStore.supportedFileAttributeViews();
    }

    @Override
    public JimfsPath getPath(String first, String... more) {
        this.fileStore.state().checkOpen();
        return this.pathService.parsePath(first, more);
    }

    public URI toUri(JimfsPath path) {
        this.fileStore.state().checkOpen();
        return this.pathService.toUri(this.uri, path.toAbsolutePath());
    }

    public JimfsPath toPath(URI uri) {
        this.fileStore.state().checkOpen();
        return this.pathService.fromUri(uri);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        this.fileStore.state().checkOpen();
        return this.pathService.createPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        this.fileStore.state().checkOpen();
        return this.userLookupService;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return this.watchServiceConfig.newWatchService(this.defaultView, this.pathService);
    }

    public synchronized ExecutorService getDefaultThreadPool() {
        if (this.defaultThreadPool == null) {
            this.defaultThreadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("JimfsFileSystem-" + this.uri.getHost() + "-defaultThreadPool-%s").build());
            this.fileStore.state().register(new Closeable() {

                @Override
                public void close() {
                    JimfsFileSystem.this.defaultThreadPool.shutdown();
                }
            });
        }
        return this.defaultThreadPool;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return this.fileStore.state().isOpen();
    }

    @Override
    public void close() throws IOException {
        this.fileStore.state().close();
    }
}
