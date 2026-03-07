package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.Watchable;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.*;

final class PollingWatchService
        extends AbstractWatchService {
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setNameFormat("dev.maru.api.protocol.PollingWatchService-thread-%d").setDaemon(true).build();
    private final ScheduledExecutorService pollingService = Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
    private final ConcurrentMap<AbstractWatchService.Key, Snapshot> snapshots = new ConcurrentHashMap<AbstractWatchService.Key, Snapshot>();
    private final FileSystemView view;
    private final PathService pathService;
    private final FileSystemState fileSystemState;
    @VisibleForTesting
    final long interval;
    @VisibleForTesting
    final TimeUnit timeUnit;
    private ScheduledFuture<?> pollingFuture;
    private final Runnable pollingTask = new Runnable() {

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            PollingWatchService pollingWatchService = PollingWatchService.this;
            synchronized (pollingWatchService) {
                for (Map.Entry entry : PollingWatchService.this.snapshots.entrySet()) {
                    AbstractWatchService.Key key = (AbstractWatchService.Key) entry.getKey();
                    Snapshot previousSnapshot = (Snapshot) entry.getValue();
                    JimfsPath path = (JimfsPath) key.watchable();
                    try {
                        Snapshot newSnapshot = PollingWatchService.this.takeSnapshot(path);
                        boolean posted = previousSnapshot.postChanges(newSnapshot, key);
                        entry.setValue(newSnapshot);
                        if (!posted) continue;
                        key.signal();
                    } catch (IOException e) {
                        key.cancel();
                    }
                }
            }
        }
    };

    PollingWatchService(FileSystemView view, PathService pathService, FileSystemState fileSystemState, long interval, TimeUnit timeUnit) {
        this.view = Preconditions.checkNotNull(view);
        this.pathService = Preconditions.checkNotNull(pathService);
        this.fileSystemState = Preconditions.checkNotNull(fileSystemState);
        Preconditions.checkArgument(interval >= 0L, "interval (%s) may not be negative", interval);
        this.interval = interval;
        this.timeUnit = Preconditions.checkNotNull(timeUnit);
        fileSystemState.register(this);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    @CanIgnoreReturnValue
    public AbstractWatchService.Key register(Watchable watchable, Iterable<? extends WatchEvent.Kind<?>> eventTypes) throws IOException {
        JimfsPath path = this.checkWatchable(watchable);
        AbstractWatchService.Key key = super.register(path, eventTypes);
        Snapshot snapshot = this.takeSnapshot(path);
        PollingWatchService pollingWatchService = this;
        synchronized (pollingWatchService) {
            this.snapshots.put(key, snapshot);
            if (this.pollingFuture == null) {
                this.startPolling();
            }
        }
        return key;
    }

    private JimfsPath checkWatchable(Watchable watchable) {
        if (!(watchable instanceof JimfsPath) || !this.isSameFileSystem((Path) watchable)) {
            throw new IllegalArgumentException("watchable (" + watchable + ") must be a Path associated with the same file system as this watch service");
        }
        return (JimfsPath) watchable;
    }

    private boolean isSameFileSystem(Path path) {
        return ((JimfsFileSystem) path.getFileSystem()).getDefaultView() == this.view;
    }

    @VisibleForTesting
    synchronized boolean isPolling() {
        return this.pollingFuture != null;
    }

    @Override
    public synchronized void cancelled(AbstractWatchService.Key key) {
        this.snapshots.remove(key);
        if (this.snapshots.isEmpty()) {
            this.stopPolling();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() {
        super.close();
        PollingWatchService pollingWatchService = this;
        synchronized (pollingWatchService) {
            for (AbstractWatchService.Key key : this.snapshots.keySet()) {
                key.cancel();
            }
            this.pollingService.shutdown();
            this.fileSystemState.unregister(this);
        }
    }

    private void startPolling() {
        this.pollingFuture = this.pollingService.scheduleAtFixedRate(this.pollingTask, this.interval, this.interval, this.timeUnit);
    }

    private void stopPolling() {
        this.pollingFuture.cancel(false);
        this.pollingFuture = null;
    }

    private Snapshot takeSnapshot(JimfsPath path) throws IOException {
        return new Snapshot(this.view.snapshotModifiedTimes(path));
    }

    private final class Snapshot {
        private final ImmutableMap<Name, FileTime> modifiedTimes;

        Snapshot(Map<Name, FileTime> modifiedTimes) {
            this.modifiedTimes = ImmutableMap.copyOf(modifiedTimes);
        }

        boolean postChanges(Snapshot newState, AbstractWatchService.Key key) {
            boolean changesPosted = false;
            if (key.subscribesTo(StandardWatchEventKinds.ENTRY_CREATE)) {
                Sets.SetView<Name> created = Sets.difference(newState.modifiedTimes.keySet(), this.modifiedTimes.keySet());
                for (Name name : created) {
                    key.post(new AbstractWatchService.Event<Path>(StandardWatchEventKinds.ENTRY_CREATE, 1, PollingWatchService.this.pathService.createFileName(name)));
                    changesPosted = true;
                }
            }
            if (key.subscribesTo(StandardWatchEventKinds.ENTRY_DELETE)) {
                Sets.SetView<Name> deleted = Sets.difference(this.modifiedTimes.keySet(), newState.modifiedTimes.keySet());
                for (Name name : deleted) {
                    key.post(new AbstractWatchService.Event<Path>(StandardWatchEventKinds.ENTRY_DELETE, 1, PollingWatchService.this.pathService.createFileName(name)));
                    changesPosted = true;
                }
            }
            if (key.subscribesTo(StandardWatchEventKinds.ENTRY_MODIFY)) {
                for (Map.Entry entry : this.modifiedTimes.entrySet()) {
                    Name name;
                    name = (Name) entry.getKey();
                    FileTime modifiedTime = (FileTime) entry.getValue();
                    FileTime newModifiedTime = newState.modifiedTimes.get(name);
                    if (newModifiedTime == null || modifiedTime.equals(newModifiedTime)) continue;
                    key.post(new AbstractWatchService.Event<Path>(StandardWatchEventKinds.ENTRY_MODIFY, 1, PollingWatchService.this.pathService.createFileName(name)));
                    changesPosted = true;
                }
            }
            return changesPosted;
        }
    }
}
