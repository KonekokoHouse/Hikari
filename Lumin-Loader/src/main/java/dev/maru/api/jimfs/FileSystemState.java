package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class FileSystemState
        implements Closeable {
    private final Set<Closeable> resources = Sets.newConcurrentHashSet();
    private final FileTimeSource fileTimeSource;
    private final Runnable onClose;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final AtomicInteger registering = new AtomicInteger();

    FileSystemState(FileTimeSource fileTimeSource, Runnable onClose) {
        this.fileTimeSource = Preconditions.checkNotNull(fileTimeSource);
        this.onClose = Preconditions.checkNotNull(onClose);
    }

    public boolean isOpen() {
        return this.open.get();
    }

    public void checkOpen() {
        if (!this.open.get()) {
            throw new ClosedFileSystemException();
        }
    }

    @CanIgnoreReturnValue
    public <C extends Closeable> C register(C resource) {
        this.checkOpen();
        this.registering.incrementAndGet();
        try {
            this.checkOpen();
            this.resources.add(resource);
            C c = resource;
            return c;
        } finally {
            this.registering.decrementAndGet();
        }
    }

    public void unregister(Closeable resource) {
        this.resources.remove(resource);
    }

    public FileTime now() {
        return this.fileTimeSource.now();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() throws IOException {
        if (this.open.compareAndSet(true, false)) {
            this.onClose.run();
            Throwable thrown = null;
            do {
                for (Closeable resource : this.resources) {
                    try {
                        resource.close();
                    } catch (Throwable e) {
                        if (thrown == null) {
                            thrown = e;
                            continue;
                        }
                        thrown.addSuppressed(e);
                    } finally {
                        this.resources.remove(resource);
                    }
                }
            } while (this.registering.get() > 0 || !this.resources.isEmpty());
            if (thrown != null) {
                Throwables.throwIfInstanceOf(thrown, IOException.class);
                Throwables.throwIfUnchecked(thrown);
            }
        }
    }
}
