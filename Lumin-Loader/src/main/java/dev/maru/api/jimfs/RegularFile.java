package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class RegularFile
        extends File {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final HeapDisk disk;
    private byte[][] blocks;
    private int blockCount;
    private long size;
    private int openCount = 0;
    private boolean deleted = false;

    public static RegularFile create(int id, FileTime creationTime, HeapDisk disk) {
        return new RegularFile(id, creationTime, disk, new byte[32][], 0, 0L);
    }

    RegularFile(int id, FileTime creationTime, HeapDisk disk, byte[][] blocks, int blockCount, long size) {
        super(id, creationTime);
        this.disk = Preconditions.checkNotNull(disk);
        this.blocks = Preconditions.checkNotNull(blocks);
        this.blockCount = blockCount;
        Preconditions.checkArgument(size >= 0L);
        this.size = size;
    }

    public Lock readLock() {
        return this.lock.readLock();
    }

    public Lock writeLock() {
        return this.lock.writeLock();
    }

    private void expandIfNecessary(int minBlockCount) {
        if (minBlockCount > this.blocks.length) {
            this.blocks = (byte[][]) Arrays.copyOf(this.blocks, Util.nextPowerOf2(minBlockCount));
        }
    }

    int blockCount() {
        return this.blockCount;
    }

    void copyBlocksTo(RegularFile target, int count) {
        int start = this.blockCount - count;
        int targetEnd = target.blockCount + count;
        target.expandIfNecessary(targetEnd);
        System.arraycopy(this.blocks, start, target.blocks, target.blockCount, count);
        target.blockCount = targetEnd;
    }

    void transferBlocksTo(RegularFile target, int count) {
        this.copyBlocksTo(target, count);
        this.truncateBlocks(this.blockCount - count);
    }

    void truncateBlocks(int count) {
        Util.clear(this.blocks, count, this.blockCount - count);
        this.blockCount = count;
    }

    void addBlock(byte[] block) {
        this.expandIfNecessary(this.blockCount + 1);
        this.blocks[this.blockCount++] = block;
    }

    @VisibleForTesting
    byte[] getBlock(int index) {
        return this.blocks[index];
    }

    public long sizeWithoutLocking() {
        return this.size;
    }

    @Override
    public long size() {
        this.readLock().lock();
        try {
            long l = this.size;
            return l;
        } finally {
            this.readLock().unlock();
        }
    }

    @Override
    RegularFile copyWithoutContent(int id, FileTime creationTime) {
        byte[][] copyBlocks = new byte[Math.max(this.blockCount * 2, 32)][];
        return new RegularFile(id, creationTime, this.disk, copyBlocks, 0, this.size);
    }

    @Override
    void copyContentTo(File file) throws IOException {
        RegularFile copy = (RegularFile) file;
        this.disk.allocate(copy, this.blockCount);
        for (int i = 0; i < this.blockCount; ++i) {
            byte[] block = this.blocks[i];
            byte[] copyBlock = copy.blocks[i];
            System.arraycopy(block, 0, copyBlock, 0, block.length);
        }
    }

    @Override
    ReadWriteLock contentLock() {
        return this.lock;
    }

    @Override
    public synchronized void opened() {
        ++this.openCount;
    }

    @Override
    public synchronized void closed() {
        if (--this.openCount == 0 && this.deleted) {
            this.deleteContents();
        }
    }

    @Override
    public synchronized void deleted() {
        if (this.links() == 0) {
            this.deleted = true;
            if (this.openCount == 0) {
                this.deleteContents();
            }
        }
    }

    private void deleteContents() {
        this.disk.free(this);
        this.size = 0L;
    }

    @CanIgnoreReturnValue
    public boolean truncate(long size) {
        if (size >= this.size) {
            return false;
        }
        long lastPosition = size - 1L;
        this.size = size;
        int newBlockCount = this.blockIndex(lastPosition) + 1;
        int blocksToRemove = this.blockCount - newBlockCount;
        if (blocksToRemove > 0) {
            this.disk.free(this, blocksToRemove);
        }
        return true;
    }

    private void prepareForWrite(long pos, long len) throws IOException {
        long end = pos + len;
        int lastBlockIndex = this.blockCount - 1;
        int endBlockIndex = this.blockIndex(end - 1L);
        if (endBlockIndex > lastBlockIndex) {
            int additionalBlocksNeeded = endBlockIndex - lastBlockIndex;
            this.disk.allocate(this, additionalBlocksNeeded);
        }
        if (pos > this.size) {
            long remaining = pos - this.size;
            int blockIndex = this.blockIndex(this.size);
            byte[] block = this.blocks[blockIndex];
            int off = this.offsetInBlock(this.size);
            remaining -= (long) RegularFile.zero(block, off, this.length(off, remaining));
            while (remaining > 0L) {
                block = this.blocks[++blockIndex];
                remaining -= (long) RegularFile.zero(block, 0, this.length(remaining));
            }
            this.size = pos;
        }
    }

    @CanIgnoreReturnValue
    public int write(long pos, byte b) throws IOException {
        this.prepareForWrite(pos, 1L);
        byte[] block = this.blocks[this.blockIndex(pos)];
        int off = this.offsetInBlock(pos);
        block[off] = b;
        if (pos >= this.size) {
            this.size = pos + 1L;
        }
        return 1;
    }

    @CanIgnoreReturnValue
    public int write(long pos, byte[] b, int off, int len) throws IOException {
        this.prepareForWrite(pos, len);
        if (len == 0) {
            return 0;
        }
        int remaining = len;
        int blockIndex = this.blockIndex(pos);
        byte[] block = this.blocks[blockIndex];
        int offInBlock = this.offsetInBlock(pos);
        int written = RegularFile.put(block, offInBlock, b, off, this.length(offInBlock, remaining));
        remaining -= written;
        off += written;
        while (remaining > 0) {
            block = this.blocks[++blockIndex];
            written = RegularFile.put(block, 0, b, off, this.length(remaining));
            remaining -= written;
            off += written;
        }
        long endPos = pos + (long) len;
        if (endPos > this.size) {
            this.size = endPos;
        }
        return len;
    }

    @CanIgnoreReturnValue
    public int write(long pos, ByteBuffer buf) throws IOException {
        int len = buf.remaining();
        this.prepareForWrite(pos, len);
        if (len == 0) {
            return 0;
        }
        int blockIndex = this.blockIndex(pos);
        byte[] block = this.blocks[blockIndex];
        int off = this.offsetInBlock(pos);
        RegularFile.put(block, off, buf);
        while (buf.hasRemaining()) {
            block = this.blocks[++blockIndex];
            RegularFile.put(block, 0, buf);
        }
        long endPos = pos + (long) len;
        if (endPos > this.size) {
            this.size = endPos;
        }
        return len;
    }

    @CanIgnoreReturnValue
    public long write(long pos, Iterable<ByteBuffer> bufs) throws IOException {
        long start = pos;
        for (ByteBuffer buf : bufs) {
            pos += (long) this.write(pos, buf);
        }
        return pos - start;
    }

    public long transferFrom(ReadableByteChannel src, long startPos, long count) throws IOException {
        if (count == 0L || startPos > this.size) {
            return 0L;
        }
        long remaining = count;
        long currentPos = startPos;
        int blockIndex = this.blockIndex(startPos);
        int off = this.offsetInBlock(startPos);
        block0:
        while (remaining > 0L) {
            byte[] block = this.blockForWrite(blockIndex);
            ByteBuffer buf = ByteBuffer.wrap(block, off, this.length(off, remaining));
            while (buf.hasRemaining()) {
                int read = src.read(buf);
                if (read < 1) {
                    if (currentPos < this.size || buf.position() != 0) break block0;
                    this.disk.free(this, 1);
                    break block0;
                }
                currentPos += (long) read;
                remaining -= (long) read;
            }
            ++blockIndex;
            off = 0;
        }
        if (currentPos > this.size) {
            this.size = currentPos;
        }
        return currentPos - startPos;
    }

    public int read(long pos) {
        if (pos >= this.size) {
            return -1;
        }
        byte[] block = this.blocks[this.blockIndex(pos)];
        int off = this.offsetInBlock(pos);
        return UnsignedBytes.toInt(block[off]);
    }

    public int read(long pos, byte[] b, int off, int len) {
        int bytesToRead = (int) this.bytesToRead(pos, len);
        if (bytesToRead > 0) {
            int remaining = bytesToRead;
            int blockIndex = this.blockIndex(pos);
            byte[] block = this.blocks[blockIndex];
            int offsetInBlock = this.offsetInBlock(pos);
            int read = RegularFile.get(block, offsetInBlock, b, off, this.length(offsetInBlock, remaining));
            remaining -= read;
            off += read;
            while (remaining > 0) {
                int index = ++blockIndex;
                block = this.blocks[index];
                read = RegularFile.get(block, 0, b, off, this.length(remaining));
                remaining -= read;
                off += read;
            }
        }
        return bytesToRead;
    }

    public int read(long pos, ByteBuffer buf) {
        int bytesToRead = (int) this.bytesToRead(pos, buf.remaining());
        if (bytesToRead > 0) {
            int remaining = bytesToRead;
            int blockIndex = this.blockIndex(pos);
            byte[] block = this.blocks[blockIndex];
            int off = this.offsetInBlock(pos);
            remaining -= RegularFile.get(block, off, buf, this.length(off, remaining));
            while (remaining > 0) {
                int index = ++blockIndex;
                block = this.blocks[index];
                remaining -= RegularFile.get(block, 0, buf, this.length(remaining));
            }
        }
        return bytesToRead;
    }

    public long read(long pos, Iterable<ByteBuffer> bufs) {
        ByteBuffer buf;
        int read;
        if (pos >= this.size()) {
            return -1L;
        }
        long start = pos;
        Iterator<ByteBuffer> iterator = bufs.iterator();
        while (iterator.hasNext() && (read = this.read(pos, buf = iterator.next())) != -1) {
            pos += (long) read;
        }
        return pos - start;
    }

    public long transferTo(long pos, long count, WritableByteChannel dest) throws IOException {
        long bytesToRead = this.bytesToRead(pos, count);
        if (bytesToRead > 0L) {
            long remaining = bytesToRead;
            int blockIndex = this.blockIndex(pos);
            byte[] block = this.blocks[blockIndex];
            int off = this.offsetInBlock(pos);
            ByteBuffer buf = ByteBuffer.wrap(block, off, this.length(off, remaining));
            while (buf.hasRemaining()) {
                remaining -= (long) dest.write(buf);
            }
            Java8Compatibility.clear(buf);
            while (remaining > 0L) {
                int index = ++blockIndex;
                block = this.blocks[index];
                buf = ByteBuffer.wrap(block, 0, this.length(remaining));
                while (buf.hasRemaining()) {
                    remaining -= (long) dest.write(buf);
                }
                Java8Compatibility.clear(buf);
            }
        }
        return Math.max(bytesToRead, 0L);
    }

    private byte[] blockForWrite(int index) throws IOException {
        if (index >= this.blockCount) {
            int additionalBlocksNeeded = index - this.blockCount + 1;
            this.disk.allocate(this, additionalBlocksNeeded);
        }
        return this.blocks[index];
    }

    private int blockIndex(long position) {
        return (int) (position / (long) this.disk.blockSize());
    }

    private int offsetInBlock(long position) {
        return (int) (position % (long) this.disk.blockSize());
    }

    private int length(long max) {
        return (int) Math.min((long) this.disk.blockSize(), max);
    }

    private int length(int off, long max) {
        return (int) Math.min((long) (this.disk.blockSize() - off), max);
    }

    private long bytesToRead(long pos, long max) {
        long available = this.size - pos;
        if (available <= 0L) {
            return -1L;
        }
        return Math.min(available, max);
    }

    private static int zero(byte[] block, int offset, int len) {
        Util.zero(block, offset, len);
        return len;
    }

    private static int put(byte[] block, int offset, byte[] b, int off, int len) {
        System.arraycopy(b, off, block, offset, len);
        return len;
    }

    private static void put(byte[] block, int offset, ByteBuffer buf) {
        int len = Math.min(block.length - offset, buf.remaining());
        buf.get(block, offset, len);
    }

    private static int get(byte[] block, int offset, byte[] b, int off, int len) {
        System.arraycopy(block, offset, b, off, len);
        return len;
    }

    private static int get(byte[] block, int offset, ByteBuffer buf, int len) {
        buf.put(block, offset, len);
        return len;
    }
}
