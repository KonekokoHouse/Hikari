package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

abstract class AbstractWatchService
        implements WatchService {
    private final BlockingQueue<WatchKey> queue = new LinkedBlockingQueue<WatchKey>();
    private final WatchKey poison = new Key(this, null, ImmutableSet.of());
    private final AtomicBoolean open = new AtomicBoolean(true);

    AbstractWatchService() {
    }

    public Key register(Watchable watchable, Iterable<? extends WatchEvent.Kind<?>> eventTypes) throws IOException {
        this.checkOpen();
        return new Key(this, watchable, eventTypes);
    }

    @VisibleForTesting
    public boolean isOpen() {
        return this.open.get();
    }

    final void enqueue(Key key) {
        if (this.isOpen()) {
            this.queue.add(key);
        }
    }

    public void cancelled(Key key) {
    }

    @VisibleForTesting
    ImmutableList<WatchKey> queuedKeys() {
        return ImmutableList.copyOf(this.queue);
    }

    @Override
    public @Nullable WatchKey poll() {
        this.checkOpen();
        return this.check((WatchKey) this.queue.poll());
    }

    @Override
    public @Nullable WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
        this.checkOpen();
        return this.check(this.queue.poll(timeout, unit));
    }

    @Override
    public WatchKey take() throws InterruptedException {
        this.checkOpen();
        return this.check(this.queue.take());
    }

    private @Nullable WatchKey check(@Nullable WatchKey key) {
        if (key == this.poison) {
            this.queue.offer(this.poison);
            throw new ClosedWatchServiceException();
        }
        return key;
    }

    protected final void checkOpen() {
        if (!this.open.get()) {
            throw new ClosedWatchServiceException();
        }
    }

    @Override
    public void close() {
        if (this.open.compareAndSet(true, false)) {
            this.queue.clear();
            this.queue.offer(this.poison);
        }
    }

    static final class Key
            implements WatchKey {
        @VisibleForTesting
        static final int MAX_QUEUE_SIZE = 256;
        private final AbstractWatchService watcher;
        private final Watchable watchable;
        private final ImmutableSet<WatchEvent.Kind<?>> subscribedTypes;
        private final AtomicReference<State> state = new AtomicReference<State>(State.READY);
        private final AtomicBoolean valid = new AtomicBoolean(true);
        private final AtomicInteger overflow = new AtomicInteger();
        private final BlockingQueue<WatchEvent<?>> events = new ArrayBlockingQueue(256);

        private static WatchEvent<Object> overflowEvent(int count) {
            return new Event<Object>(StandardWatchEventKinds.OVERFLOW, count, null);
        }

        public Key(AbstractWatchService watcher, @Nullable Watchable watchable, Iterable<? extends WatchEvent.Kind<?>> subscribedTypes) {
            this.watcher = Preconditions.checkNotNull(watcher);
            this.watchable = watchable;
            this.subscribedTypes = ImmutableSet.copyOf(subscribedTypes);
        }

        @VisibleForTesting
        State state() {
            return this.state.get();
        }

        public boolean subscribesTo(WatchEvent.Kind<?> eventType) {
            return this.subscribedTypes.contains(eventType);
        }

        public void post(WatchEvent<?> event) {
            if (!this.events.offer(event)) {
                this.overflow.incrementAndGet();
            }
        }

        public void signal() {
            if (this.state.getAndSet(State.SIGNALLED) == State.READY) {
                this.watcher.enqueue(this);
            }
        }

        @Override
        public boolean isValid() {
            return this.watcher.isOpen() && this.valid.get();
        }

        @Override
        public List<WatchEvent<?>> pollEvents() {
            List<WatchEvent<?>> result = new ArrayList<>(this.events.size());
            this.events.drainTo(result);
            int overflowCount = this.overflow.getAndSet(0);
            if (overflowCount != 0) {
                result.add(Key.overflowEvent(overflowCount));
            }
            return Collections.unmodifiableList(result);
        }

        @Override
        @CanIgnoreReturnValue
        public boolean reset() {
            if (this.isValid() && this.state.compareAndSet(State.SIGNALLED, State.READY) && !this.events.isEmpty()) {
                this.signal();
            }
            return this.isValid();
        }

        @Override
        public void cancel() {
            this.valid.set(false);
            this.watcher.cancelled(this);
        }

        @Override
        public Watchable watchable() {
            return this.watchable;
        }

        @VisibleForTesting
        static enum State {
            READY,
            SIGNALLED;

        }
    }

    static final class Event<T>
            implements WatchEvent<T> {
        private final WatchEvent.Kind<T> kind;
        private final int count;
        private final @Nullable T context;

        public Event(WatchEvent.Kind<T> kind, int count, @Nullable T context) {
            this.kind = Preconditions.checkNotNull(kind);
            Preconditions.checkArgument(count >= 0, "count (%s) must be non-negative", count);
            this.count = count;
            this.context = context;
        }

        @Override
        public WatchEvent.Kind<T> kind() {
            return this.kind;
        }

        @Override
        public int count() {
            return this.count;
        }

        @Override
        public @Nullable T context() {
            return this.context;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Event) {
                Event other = (Event) obj;
                return this.kind().equals(other.kind()) && this.count() == other.count() && Objects.equals(this.context(), other.context());
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.kind(), this.count(), this.context());
        }

        public String toString() {
            return MoreObjects.toStringHelper(this).add("kind", this.kind()).add("count", this.count()).add("context", this.context()).toString();
        }
    }
}
