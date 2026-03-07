package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Iterator;
import java.util.Set;

final class JimfsSecureDirectoryStream
        implements SecureDirectoryStream<Path> {
    private final FileSystemView view;
    private final DirectoryStream.Filter<? super Path> filter;
    private final FileSystemState fileSystemState;
    private boolean open = true;
    private Iterator<Path> iterator = new DirectoryIterator();
    public static final DirectoryStream.Filter<Object> ALWAYS_TRUE_FILTER = new DirectoryStream.Filter<Object>() {

        @Override
        public boolean accept(Object entry) throws IOException {
            return true;
        }
    };

    public JimfsSecureDirectoryStream(FileSystemView view, DirectoryStream.Filter<? super Path> filter, FileSystemState fileSystemState) {
        this.view = Preconditions.checkNotNull(view);
        this.filter = Preconditions.checkNotNull(filter);
        this.fileSystemState = fileSystemState;
        fileSystemState.register(this);
    }

    private JimfsPath path() {
        return this.view.getWorkingDirectoryPath();
    }

    @Override
    public synchronized Iterator<Path> iterator() {
        this.checkOpen();
        Iterator<Path> result = this.iterator;
        Preconditions.checkState(result != null, "iterator() has already been called once");
        this.iterator = null;
        return result;
    }

    @Override
    public synchronized void close() {
        this.open = false;
        this.fileSystemState.unregister(this);
    }

    protected synchronized void checkOpen() {
        if (!this.open) {
            throw new ClosedDirectoryStreamException();
        }
    }

    @Override
    public SecureDirectoryStream<Path> newDirectoryStream(Path path, LinkOption... options) throws IOException {
        this.checkOpen();
        JimfsPath checkedPath = JimfsSecureDirectoryStream.checkPath(path);
        return (SecureDirectoryStream) this.view.newDirectoryStream(checkedPath, ALWAYS_TRUE_FILTER, Options.getLinkOptions(options), this.path().resolve(checkedPath));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        this.checkOpen();
        JimfsPath checkedPath = JimfsSecureDirectoryStream.checkPath(path);
        ImmutableSet<OpenOption> opts = Options.getOptionsForChannel(options);
        return new JimfsFileChannel(this.view.getOrCreateRegularFile(checkedPath, opts, new FileAttribute[0]), opts, this.fileSystemState);
    }

    @Override
    public void deleteFile(Path path) throws IOException {
        this.checkOpen();
        JimfsPath checkedPath = JimfsSecureDirectoryStream.checkPath(path);
        this.view.deleteFile(checkedPath, FileSystemView.DeleteMode.NON_DIRECTORY_ONLY);
    }

    @Override
    public void deleteDirectory(Path path) throws IOException {
        this.checkOpen();
        JimfsPath checkedPath = JimfsSecureDirectoryStream.checkPath(path);
        this.view.deleteFile(checkedPath, FileSystemView.DeleteMode.DIRECTORY_ONLY);
    }

    @Override
    public void move(Path srcPath, SecureDirectoryStream<Path> targetDir, Path targetPath) throws IOException {
        this.checkOpen();
        JimfsPath checkedSrcPath = JimfsSecureDirectoryStream.checkPath(srcPath);
        JimfsPath checkedTargetPath = JimfsSecureDirectoryStream.checkPath(targetPath);
        if (!(targetDir instanceof JimfsSecureDirectoryStream)) {
            throw new ProviderMismatchException("targetDir isn't a secure directory stream associated with this file system");
        }
        JimfsSecureDirectoryStream checkedTargetDir = (JimfsSecureDirectoryStream) targetDir;
        this.view.copy(checkedSrcPath, checkedTargetDir.view, checkedTargetPath, ImmutableSet.of(), true);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
        return this.getFileAttributeView(this.path().getFileSystem().getPath(".", new String[0]), type, new LinkOption[0]);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        this.checkOpen();
        final JimfsPath checkedPath = JimfsSecureDirectoryStream.checkPath(path);
        final ImmutableSet<LinkOption> optionsSet = Options.getLinkOptions(options);
        return this.view.getFileAttributeView(new FileLookup() {

            @Override
            public File lookup() throws IOException {
                JimfsSecureDirectoryStream.this.checkOpen();
                return JimfsSecureDirectoryStream.this.view.lookUpWithLock(checkedPath, optionsSet).requireExists(checkedPath).file();
            }
        }, type);
    }

    private static JimfsPath checkPath(Path path) {
        if (path instanceof JimfsPath) {
            return (JimfsPath) path;
        }
        throw new ProviderMismatchException("path " + path + " is not associated with a Jimfs file system");
    }

    private final class DirectoryIterator
            extends AbstractIterator<Path> {
        private @Nullable Iterator<Name> fileNames;

        private DirectoryIterator() {
        }

        @Override
        protected synchronized Path computeNext() {
            JimfsSecureDirectoryStream.this.checkOpen();
            try {
                if (this.fileNames == null) {
                    this.fileNames = JimfsSecureDirectoryStream.this.view.snapshotWorkingDirectoryEntries().iterator();
                }
                while (this.fileNames.hasNext()) {
                    Name name = this.fileNames.next();
                    JimfsPath path = JimfsSecureDirectoryStream.this.view.getWorkingDirectoryPath().resolve(name);
                    if (!JimfsSecureDirectoryStream.this.filter.accept(path)) continue;
                    return path;
                }
                return (Path) this.endOfData();
            } catch (IOException e) {
                throw new DirectoryIteratorException(e);
            }
        }
    }
}
