package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.OutputStream;

final class JimfsOutputStream
        extends OutputStream {
    @GuardedBy(value = "this")
    @VisibleForTesting
    RegularFile file;
    @GuardedBy(value = "this")
    private long pos;
    private final boolean append;
    private final FileSystemState fileSystemState;

    JimfsOutputStream(RegularFile file, boolean append, FileSystemState fileSystemState) {
        this.file = Preconditions.checkNotNull(file);
        this.append = append;
        this.fileSystemState = fileSystemState;
        fileSystemState.register(this);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        this.checkNotClosed();
        this.file.writeLock().lock();
        try {
            if (this.append) {
                this.pos = this.file.sizeWithoutLocking();
            }
            this.file.write(this.pos++, (byte) b);
            this.file.setLastModifiedTime(this.fileSystemState.now());
        } finally {
            this.file.writeLock().unlock();
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.writeInternal(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Preconditions.checkPositionIndexes(off, off + len, b.length);
        this.writeInternal(b, off, len);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private synchronized void writeInternal(byte[] b, int off, int len) throws IOException {
        this.checkNotClosed();
        this.file.writeLock().lock();
        try {
            if (this.append) {
                this.pos = this.file.sizeWithoutLocking();
            }
            this.pos += (long) this.file.write(this.pos, b, off, len);
            this.file.setLastModifiedTime(this.fileSystemState.now());
        } finally {
            this.file.writeLock().unlock();
        }
    }

    @GuardedBy(value = "this")
    private void checkNotClosed() throws IOException {
        if (this.file == null) {
            throw new IOException("stream is closed");
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.isOpen()) {
            this.fileSystemState.unregister(this);
            this.file.closed();
            this.file = null;
        }
    }

    @GuardedBy(value = "this")
    private boolean isOpen() {
        return this.file != null;
    }
}
