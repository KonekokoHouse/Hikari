package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;

final class DowngradedSeekableByteChannel
        implements SeekableByteChannel {
    private final FileChannel channel;

    DowngradedSeekableByteChannel(FileChannel channel) {
        this.channel = Preconditions.checkNotNull(channel);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return this.channel.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return this.channel.write(src);
    }

    @Override
    public long position() throws IOException {
        return this.channel.position();
    }

    @Override
    @CanIgnoreReturnValue
    public SeekableByteChannel position(long newPosition) throws IOException {
        this.channel.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return this.channel.size();
    }

    @Override
    @CanIgnoreReturnValue
    public SeekableByteChannel truncate(long size) throws IOException {
        this.channel.truncate(size);
        return this;
    }

    @Override
    public boolean isOpen() {
        return this.channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }
}
