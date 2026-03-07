package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

final class JimfsAsynchronousFileChannel
        extends AsynchronousFileChannel {
    private final JimfsFileChannel channel;
    private final ListeningExecutorService executor;

    public JimfsAsynchronousFileChannel(JimfsFileChannel channel, ExecutorService executor) {
        this.channel = Preconditions.checkNotNull(channel);
        this.executor = MoreExecutors.listeningDecorator(executor);
    }

    @Override
    public long size() throws IOException {
        return this.channel.size();
    }

    private <R, A> void addCallback(ListenableFuture<R> future, CompletionHandler<R, ? super A> handler, @Nullable A attachment) {
        future.addListener(new CompletionHandlerCallback(future, handler, attachment), this.executor);
    }

    @Override
    @CanIgnoreReturnValue
    public AsynchronousFileChannel truncate(long size) throws IOException {
        this.channel.truncate(size);
        return this;
    }

    @Override
    public void force(boolean metaData) throws IOException {
        this.channel.force(metaData);
    }

    @Override
    public <A> void lock(long position, long size, boolean shared, @Nullable A attachment, CompletionHandler<FileLock, ? super A> handler) {
        Preconditions.checkNotNull(handler);
        this.addCallback((ListenableFuture) this.lock(position, size, shared), (CompletionHandler) handler, attachment);
    }

    public ListenableFuture<FileLock> lock(final long position, final long size, final boolean shared) {
        Util.checkNotNegative(position, "position");
        Util.checkNotNegative(size, "size");
        if (!this.isOpen()) {
            return JimfsAsynchronousFileChannel.closedChannelFuture();
        }
        if (shared) {
            this.channel.checkReadable();
        } else {
            this.channel.checkWritable();
        }
        return this.executor.submit(new Callable<FileLock>() {

            @Override
            public FileLock call() throws IOException {
                return JimfsAsynchronousFileChannel.this.tryLock(position, size, shared);
            }
        });
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        Util.checkNotNegative(position, "position");
        Util.checkNotNegative(size, "size");
        this.channel.checkOpen();
        if (shared) {
            this.channel.checkReadable();
        } else {
            this.channel.checkWritable();
        }
        return new JimfsFileChannel.FakeFileLock(this, position, size, shared);
    }

    @Override
    public <A> void read(ByteBuffer dst, long position, @Nullable A attachment, CompletionHandler<Integer, ? super A> handler) {
        this.addCallback((ListenableFuture) this.read(dst, position), (CompletionHandler) handler, attachment);
    }

    public ListenableFuture<Integer> read(final ByteBuffer dst, final long position) {
        Preconditions.checkArgument(!dst.isReadOnly(), "dst may not be read-only");
        Util.checkNotNegative(position, "position");
        if (!this.isOpen()) {
            return JimfsAsynchronousFileChannel.closedChannelFuture();
        }
        this.channel.checkReadable();
        return this.executor.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws IOException {
                return JimfsAsynchronousFileChannel.this.channel.read(dst, position);
            }
        });
    }

    @Override
    public <A> void write(ByteBuffer src, long position, @Nullable A attachment, CompletionHandler<Integer, ? super A> handler) {
        this.addCallback((ListenableFuture) this.write(src, position), (CompletionHandler) handler, attachment);
    }

    public ListenableFuture<Integer> write(final ByteBuffer src, final long position) {
        Util.checkNotNegative(position, "position");
        if (!this.isOpen()) {
            return JimfsAsynchronousFileChannel.closedChannelFuture();
        }
        this.channel.checkWritable();
        return this.executor.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws IOException {
                return JimfsAsynchronousFileChannel.this.channel.write(src, position);
            }
        });
    }

    @Override
    public boolean isOpen() {
        return this.channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }

    private static <V> ListenableFuture<V> closedChannelFuture() {
        SettableFuture future = SettableFuture.create();
        future.setException(new ClosedChannelException());
        return future;
    }

    private static final class CompletionHandlerCallback<R, A>
            implements Runnable {
        private final ListenableFuture<R> future;
        private final CompletionHandler<R, ? super A> completionHandler;
        private final @Nullable A attachment;

        private CompletionHandlerCallback(ListenableFuture<R> future, CompletionHandler<R, ? super A> completionHandler, @Nullable A attachment) {
            this.future = Preconditions.checkNotNull(future);
            this.completionHandler = Preconditions.checkNotNull(completionHandler);
            this.attachment = attachment;
        }

        @Override
        public void run() {
            R result;
            try {
                result = Futures.getDone(this.future);
            } catch (ExecutionException e) {
                this.onFailure(e.getCause());
                return;
            } catch (Error | RuntimeException e) {
                this.onFailure(e);
                return;
            }
            this.onSuccess(result);
        }

        private void onSuccess(R result) {
            this.completionHandler.completed(result, this.attachment);
        }

        private void onFailure(Throwable t) {
            this.completionHandler.failed(t, this.attachment);
        }
    }
}
