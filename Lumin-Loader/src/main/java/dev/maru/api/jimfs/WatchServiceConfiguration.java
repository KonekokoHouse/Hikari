package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;

public abstract class WatchServiceConfiguration {
    static final WatchServiceConfiguration DEFAULT = WatchServiceConfiguration.polling(5L, TimeUnit.SECONDS);

    public static WatchServiceConfiguration polling(long interval, TimeUnit timeUnit) {
        return new PollingConfig(interval, timeUnit);
    }

    WatchServiceConfiguration() {
    }

    abstract AbstractWatchService newWatchService(FileSystemView var1, PathService var2);

    private static final class PollingConfig
            extends WatchServiceConfiguration {
        private final long interval;
        private final TimeUnit timeUnit;

        private PollingConfig(long interval, TimeUnit timeUnit) {
            Preconditions.checkArgument(interval > 0L, "interval (%s) must be positive", interval);
            this.interval = interval;
            this.timeUnit = Preconditions.checkNotNull(timeUnit);
        }

        @Override
        AbstractWatchService newWatchService(FileSystemView view, PathService pathService) {
            return new PollingWatchService(view, pathService, view.state(), this.interval, this.timeUnit);
        }

        public String toString() {
            return "WatchServiceConfiguration.polling(" + this.interval + ", " + (Object) ((Object) this.timeUnit) + ")";
        }
    }
}
