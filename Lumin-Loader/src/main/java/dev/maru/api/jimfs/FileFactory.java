package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

final class FileFactory {
    private final AtomicInteger idGenerator = new AtomicInteger();
    private final HeapDisk disk;
    private final FileTimeSource fileTimeSource;
    private final Supplier<Directory> directorySupplier = new DirectorySupplier();
    private final Supplier<RegularFile> regularFileSupplier = new RegularFileSupplier();

    public FileFactory(HeapDisk disk, FileTimeSource fileTimeSource) {
        this.disk = Preconditions.checkNotNull(disk);
        this.fileTimeSource = Preconditions.checkNotNull(fileTimeSource);
    }

    private int nextFileId() {
        return this.idGenerator.getAndIncrement();
    }

    public Directory createDirectory() {
        return Directory.create(this.nextFileId(), this.fileTimeSource.now());
    }

    public Directory createRootDirectory(Name name) {
        return Directory.createRoot(this.nextFileId(), this.fileTimeSource.now(), name);
    }

    @VisibleForTesting
    RegularFile createRegularFile() {
        return RegularFile.create(this.nextFileId(), this.fileTimeSource.now(), this.disk);
    }

    @VisibleForTesting
    SymbolicLink createSymbolicLink(JimfsPath target) {
        return SymbolicLink.create(this.nextFileId(), this.fileTimeSource.now(), target);
    }

    public File copyWithoutContent(File file) throws IOException {
        return file.copyWithoutContent(this.nextFileId(), this.fileTimeSource.now());
    }

    public Supplier<Directory> directoryCreator() {
        return this.directorySupplier;
    }

    public Supplier<RegularFile> regularFileCreator() {
        return this.regularFileSupplier;
    }

    public Supplier<SymbolicLink> symbolicLinkCreator(JimfsPath target) {
        return new SymbolicLinkSupplier(target);
    }

    private final class DirectorySupplier
            implements Supplier<Directory> {
        private DirectorySupplier() {
        }

        @Override
        public Directory get() {
            return FileFactory.this.createDirectory();
        }
    }

    private final class RegularFileSupplier
            implements Supplier<RegularFile> {
        private RegularFileSupplier() {
        }

        @Override
        public RegularFile get() {
            return FileFactory.this.createRegularFile();
        }
    }

    private final class SymbolicLinkSupplier
            implements Supplier<SymbolicLink> {
        private final JimfsPath target;

        protected SymbolicLinkSupplier(JimfsPath target) {
            this.target = Preconditions.checkNotNull(target);
        }

        @Override
        public SymbolicLink get() {
            return FileFactory.this.createSymbolicLink(this.target);
        }
    }
}
