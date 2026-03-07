package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.io.InputStream;

final class JimfsInputStream
        extends InputStream {
    @GuardedBy(value = "this")
    @VisibleForTesting
    RegularFile file;
    @GuardedBy(value = "this")
    private long pos;
    @GuardedBy(value = "this")
    private boolean finished;
    private final FileSystemState fileSystemState;

    public JimfsInputStream(RegularFile file, FileSystemState fileSystemState) {
        this.file = Preconditions.checkNotNull(file);
        this.fileSystemState = fileSystemState;
        fileSystemState.register(this);
    }

    @Override
    public synchronized int read() throws IOException {
        this.checkNotClosed();
        if (this.finished) {
            return -1;
        }
        this.file.readLock().lock();
        try {
            int b = this.file.read(this.pos++);
            if (b == -1) {
                this.finished = true;
            } else {
                this.file.setLastAccessTime(this.fileSystemState.now());
            }
            int n = b;
            return n;
        } finally {
            this.file.readLock().unlock();
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.readInternal(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Preconditions.checkPositionIndexes(off, off + len, b.length);
        return this.readInternal(b, off, len);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private synchronized int readInternal(byte[] b, int off, int len) throws IOException {
        this.checkNotClosed();
        if (this.finished) {
            return -1;
        }
        this.file.readLock().lock();
        try {
            int read = this.file.read(this.pos, b, off, len);
            if (read == -1) {
                this.finished = true;
            } else {
                this.pos += (long) read;
            }
            this.file.setLastAccessTime(this.fileSystemState.now());
            int n = read;
            return n;
        } finally {
            this.file.readLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public long skip(long n) throws IOException {
        if (n <= 0L) {
            return 0L;
        }
        JimfsInputStream jimfsInputStream = this;
        synchronized (jimfsInputStream) {
            this.checkNotClosed();
            if (this.finished) {
                return 0L;
            }
            int skip = (int) Math.min(Math.max(this.file.size() - this.pos, 0L), n);
            this.pos += (long) skip;
            return skip;
        }
    }

    @Override
    public synchronized int available() throws IOException {
        this.checkNotClosed();
        if (this.finished) {
            return 0;
        }
        long available = Math.max(this.file.size() - this.pos, 0L);
        return Ints.saturatedCast(available);
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
