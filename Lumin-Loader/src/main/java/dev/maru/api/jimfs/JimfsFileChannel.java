package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

final class JimfsFileChannel
        extends FileChannel {
    @GuardedBy(value = "blockingThreads")
    private final Set<Thread> blockingThreads = new HashSet<Thread>();
    private final RegularFile file;
    private final FileSystemState fileSystemState;
    private final boolean read;
    private final boolean write;
    private final boolean append;
    @GuardedBy(value = "this")
    private long position;

    public JimfsFileChannel(RegularFile file, Set<OpenOption> options, FileSystemState fileSystemState) {
        this.file = file;
        this.fileSystemState = fileSystemState;
        this.read = options.contains(StandardOpenOption.READ);
        this.write = options.contains(StandardOpenOption.WRITE);
        this.append = options.contains(StandardOpenOption.APPEND);
        fileSystemState.register(this);
    }

    public AsynchronousFileChannel asAsynchronousFileChannel(ExecutorService executor) {
        return new JimfsAsynchronousFileChannel(this, executor);
    }

    void checkReadable() {
        if (!this.read) {
            throw new NonReadableChannelException();
        }
    }

    void checkWritable() {
        if (!this.write) {
            throw new NonWritableChannelException();
        }
    }

    void checkOpen() throws ClosedChannelException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean beginBlocking() {
        this.begin();
        Set<Thread> set = this.blockingThreads;
        synchronized (set) {
            if (this.isOpen()) {
                this.blockingThreads.add(Thread.currentThread());
                return true;
            }
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void endBlocking(boolean completed) throws AsynchronousCloseException {
        Set<Thread> set = this.blockingThreads;
        synchronized (set) {
            this.blockingThreads.remove(Thread.currentThread());
        }
        this.end(completed);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int read(ByteBuffer dst) throws IOException {
        Preconditions.checkNotNull(dst);
        this.checkOpen();
        this.checkReadable();
        int read = 0;
        JimfsFileChannel jimfsFileChannel = this;
        synchronized (jimfsFileChannel) {
            boolean completed = false;
            try {
                if (!this.beginBlocking()) {
                    int n = 0;
                    return n;
                }
                this.file.readLock().lockInterruptibly();
                try {
                    read = this.file.read(this.position, dst);
                    if (read != -1) {
                        this.position += (long) read;
                    }
                    this.file.setLastAccessTime(this.fileSystemState.now());
                    completed = true;
                } finally {
                    this.file.readLock().unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.endBlocking(completed);
            }
            return read;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        Preconditions.checkPositionIndexes(offset, offset + length, dsts.length);
        List<ByteBuffer> buffers = Arrays.asList(dsts).subList(offset, offset + length);
        Util.checkNoneNull(buffers);
        this.checkOpen();
        this.checkReadable();
        long read = 0L;
        JimfsFileChannel jimfsFileChannel = this;
        synchronized (jimfsFileChannel) {
            boolean completed = false;
            try {
                if (!this.beginBlocking()) {
                    long l = 0L;
                    return l;
                }
                this.file.readLock().lockInterruptibly();
                try {
                    read = this.file.read(this.position, buffers);
                    if (read != -1L) {
                        this.position += read;
                    }
                    this.file.setLastAccessTime(this.fileSystemState.now());
                    completed = true;
                } finally {
                    this.file.readLock().unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.endBlocking(completed);
            }
            return read;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        Preconditions.checkNotNull(dst);
        Util.checkNotNegative(position, "position");
        this.checkOpen();
        this.checkReadable();
        int read = 0;
        boolean completed = false;
        try {
            if (!this.beginBlocking()) {
                int n = 0;
                return n;
            }
            this.file.readLock().lockInterruptibly();
            try {
                read = this.file.read(position, dst);
                this.file.setLastAccessTime(this.fileSystemState.now());
                completed = true;
            } finally {
                this.file.readLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.endBlocking(completed);
        }
        return read;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        Preconditions.checkNotNull(src);
        this.checkOpen();
        this.checkWritable();
        int written = 0;
        JimfsFileChannel jimfsFileChannel = this;
        synchronized (jimfsFileChannel) {
            boolean completed = false;
            try {
                if (!this.beginBlocking()) {
                    int n = 0;
                    return n;
                }
                this.file.writeLock().lockInterruptibly();
                try {
                    if (this.append) {
                        this.position = this.file.size();
                    }
                    written = this.file.write(this.position, src);
                    this.position += (long) written;
                    this.file.setLastModifiedTime(this.fileSystemState.now());
                    completed = true;
                } finally {
                    this.file.writeLock().unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.endBlocking(completed);
            }
            return written;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        Preconditions.checkPositionIndexes(offset, offset + length, srcs.length);
        List<ByteBuffer> buffers = Arrays.asList(srcs).subList(offset, offset + length);
        Util.checkNoneNull(buffers);
        this.checkOpen();
        this.checkWritable();
        long written = 0L;
        JimfsFileChannel jimfsFileChannel = this;
        synchronized (jimfsFileChannel) {
            boolean completed = false;
            try {
                if (!this.beginBlocking()) {
                    long l = 0L;
                    return l;
                }
                this.file.writeLock().lockInterruptibly();
                try {
                    if (this.append) {
                        this.position = this.file.size();
                    }
                    written = this.file.write(this.position, buffers);
                    this.position += written;
                    this.file.setLastModifiedTime(this.fileSystemState.now());
                    completed = true;
                } finally {
                    this.file.writeLock().unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.endBlocking(completed);
            }
            return written;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        Preconditions.checkNotNull(src);
        Util.checkNotNegative(position, "position");
        this.checkOpen();
        this.checkWritable();
        int written = 0;
        if (this.append) {
            JimfsFileChannel jimfsFileChannel = this;
            synchronized (jimfsFileChannel) {
                boolean completed2 = false;
                try {
                    if (!this.beginBlocking()) {
                        int n = 0;
                        return n;
                    }
                    this.file.writeLock().lockInterruptibly();
                    try {
                        position = this.file.sizeWithoutLocking();
                        written = this.file.write(position, src);
                        this.position = position + (long) written;
                        this.file.setLastModifiedTime(this.fileSystemState.now());
                        completed2 = true;
                    } finally {
                        this.file.writeLock().unlock();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    this.endBlocking(completed2);
                }
            }
        } else {
            boolean completed = false;
            try {
                if (!this.beginBlocking()) {
                    int completed2 = 0;
                    return completed2;
                }
                this.file.writeLock().lockInterruptibly();
                try {
                    written = this.file.write(position, src);
                    this.file.setLastModifiedTime(this.fileSystemState.now());
                    completed = true;
                } finally {
                    this.file.writeLock().unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.endBlocking(completed);
            }
        }
        {
            return written;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public long position() throws IOException {
        this.checkOpen();
        JimfsFileChannel jimfsFileChannel = this;
        synchronized (jimfsFileChannel) {
            long pos;
            boolean completed = false;
            try {
                this.begin();
                if (!this.isOpen()) {
                    long l = 0L;
                    return l;
                }
                pos = this.position;
                completed = true;
            } finally {
                this.end(completed);
            }
            return pos;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    @CanIgnoreReturnValue
    public FileChannel position(long newPosition) throws IOException {
        Util.checkNotNegative(newPosition, "newPosition");
        this.checkOpen();
        JimfsFileChannel jimfsFileChannel = this;
        synchronized (jimfsFileChannel) {
            boolean completed = false;
            try {
                this.begin();
                if (!this.isOpen()) {
                    JimfsFileChannel jimfsFileChannel2 = this;
                    return jimfsFileChannel2;
                }
                this.position = newPosition;
                completed = true;
            } finally {
                this.end(completed);
            }
            return this;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public long size() throws IOException {
        this.checkOpen();
        long size = 0L;
        boolean completed = false;
        try {
            if (!this.beginBlocking()) {
                long l = 0L;
                return l;
            }
            this.file.readLock().lockInterruptibly();
            try {
                size = this.file.sizeWithoutLocking();
                completed = true;
            } finally {
                this.file.readLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.endBlocking(completed);
        }
        return size;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    @CanIgnoreReturnValue
    public FileChannel truncate(long size) throws IOException {
        Util.checkNotNegative(size, "size");
        this.checkOpen();
        this.checkWritable();
        JimfsFileChannel jimfsFileChannel = this;
        synchronized (jimfsFileChannel) {
            boolean completed = false;
            try {
                if (!this.beginBlocking()) {
                    JimfsFileChannel jimfsFileChannel2 = this;
                    return jimfsFileChannel2;
                }
                this.file.writeLock().lockInterruptibly();
                try {
                    this.file.truncate(size);
                    if (this.position > size) {
                        this.position = size;
                    }
                    this.file.setLastModifiedTime(this.fileSystemState.now());
                    completed = true;
                } finally {
                    this.file.writeLock().unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.endBlocking(completed);
            }
            return this;
        }
    }

    @Override
    public void force(boolean metaData) throws IOException {
        this.checkOpen();
        boolean completed = false;
        try {
            this.begin();
            completed = true;
        } finally {
            this.end(completed);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        Preconditions.checkNotNull(target);
        Util.checkNotNegative(position, "position");
        Util.checkNotNegative(count, "count");
        this.checkOpen();
        this.checkReadable();
        long transferred = 0L;
        boolean completed = false;
        try {
            if (!this.beginBlocking()) {
                long l = 0L;
                return l;
            }
            this.file.readLock().lockInterruptibly();
            try {
                transferred = this.file.transferTo(position, count, target);
                this.file.setLastAccessTime(this.fileSystemState.now());
                completed = true;
            } finally {
                this.file.readLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.endBlocking(completed);
        }
        return transferred;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        Preconditions.checkNotNull(src);
        Util.checkNotNegative(position, "position");
        Util.checkNotNegative(count, "count");
        this.checkOpen();
        this.checkWritable();
        long transferred = 0L;
        if (this.append) {
            JimfsFileChannel jimfsFileChannel = this;
            synchronized (jimfsFileChannel) {
                boolean completed2 = false;
                try {
                    if (!this.beginBlocking()) {
                        long l = 0L;
                        return l;
                    }
                    this.file.writeLock().lockInterruptibly();
                    try {
                        position = this.file.sizeWithoutLocking();
                        transferred = this.file.transferFrom(src, position, count);
                        this.position = position + transferred;
                        this.file.setLastModifiedTime(this.fileSystemState.now());
                        completed2 = true;
                    } finally {
                        this.file.writeLock().unlock();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    this.endBlocking(completed2);
                }
            }
        } else {
            boolean completed = false;
            try {
                if (!this.beginBlocking()) {
                    long completed2 = 0L;
                    return completed2;
                }
                this.file.writeLock().lockInterruptibly();
                try {
                    transferred = this.file.transferFrom(src, position, count);
                    this.file.setLastModifiedTime(this.fileSystemState.now());
                    completed = true;
                } finally {
                    this.file.writeLock().unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.endBlocking(completed);
            }
        }
        {
            return transferred;
        }
    }

    @Override
    public MappedByteBuffer map(FileChannel.MapMode mode, long position, long size) throws IOException {
        throw new UnsupportedOperationException();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        this.checkLockArguments(position, size, shared);
        boolean completed = false;
        try {
            this.begin();
            completed = true;
            FakeFileLock fakeFileLock = new FakeFileLock(this, position, size, shared);
            return fakeFileLock;
        } finally {
            try {
                this.end(completed);
            } catch (ClosedByInterruptException e) {
                throw new FileLockInterruptionException();
            }
        }
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        this.checkLockArguments(position, size, shared);
        return new FakeFileLock(this, position, size, shared);
    }

    private void checkLockArguments(long position, long size, boolean shared) throws IOException {
        Util.checkNotNegative(position, "position");
        Util.checkNotNegative(size, "size");
        this.checkOpen();
        if (shared) {
            this.checkReadable();
        } else {
            this.checkWritable();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void implCloseChannel() {
        try {
            Set<Thread> set = this.blockingThreads;
            synchronized (set) {
                for (Thread thread : this.blockingThreads) {
                    thread.interrupt();
                }
            }
        } finally {
            this.fileSystemState.unregister(this);
            this.file.closed();
        }
    }

    static final class FakeFileLock
            extends FileLock {
        private final AtomicBoolean valid = new AtomicBoolean(true);

        public FakeFileLock(FileChannel channel, long position, long size, boolean shared) {
            super(channel, position, size, shared);
        }

        public FakeFileLock(AsynchronousFileChannel channel, long position, long size, boolean shared) {
            super(channel, position, size, shared);
        }

        @Override
        public boolean isValid() {
            return this.valid.get();
        }

        @Override
        public void release() throws IOException {
            this.valid.set(false);
        }
    }
}
