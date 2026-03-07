package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.nio.file.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

final class Options {
    public static final ImmutableSet<LinkOption> NOFOLLOW_LINKS = ImmutableSet.of(LinkOption.NOFOLLOW_LINKS);
    public static final ImmutableSet<LinkOption> FOLLOW_LINKS = ImmutableSet.of();
    private static final ImmutableSet<OpenOption> DEFAULT_READ = ImmutableSet.of(StandardOpenOption.READ);
    private static final ImmutableSet<OpenOption> DEFAULT_READ_NOFOLLOW_LINKS = ImmutableSet.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    private static final ImmutableSet<OpenOption> DEFAULT_WRITE = ImmutableSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

    private Options() {
    }

    public static ImmutableSet<LinkOption> getLinkOptions(LinkOption... options) {
        return options.length == 0 ? FOLLOW_LINKS : NOFOLLOW_LINKS;
    }

    public static ImmutableSet<OpenOption> getOptionsForChannel(Set<? extends OpenOption> options) {
        boolean read;
        if (options.isEmpty()) {
            return DEFAULT_READ;
        }
        boolean append = options.contains(StandardOpenOption.APPEND);
        boolean write = append || options.contains(StandardOpenOption.WRITE);
        boolean bl = read = !write || options.contains(StandardOpenOption.READ);
        if (read) {
            if (append) {
                throw new UnsupportedOperationException("'READ' + 'APPEND' not allowed");
            }
            if (!write) {
                return options.contains(LinkOption.NOFOLLOW_LINKS) ? DEFAULT_READ_NOFOLLOW_LINKS : DEFAULT_READ;
            }
        }
        return Options.addWrite(options);
    }

    public static ImmutableSet<OpenOption> getOptionsForInputStream(OpenOption... options) {
        boolean nofollowLinks = false;
        for (OpenOption option : options) {
            if (Preconditions.checkNotNull(option) == StandardOpenOption.READ) continue;
            if (option == LinkOption.NOFOLLOW_LINKS) {
                nofollowLinks = true;
                continue;
            }
            throw new UnsupportedOperationException("'" + option + "' not allowed");
        }
        return nofollowLinks ? ImmutableSet.<OpenOption>of(LinkOption.NOFOLLOW_LINKS) : ImmutableSet.<OpenOption>of();
    }

    public static ImmutableSet<OpenOption> getOptionsForOutputStream(OpenOption... options) {
        if (options.length == 0) {
            return DEFAULT_WRITE;
        }
        ImmutableSet<OpenOption> result = Options.addWrite(Arrays.asList(options));
        if (result.contains(StandardOpenOption.READ)) {
            throw new UnsupportedOperationException("'READ' not allowed");
        }
        return result;
    }

    private static ImmutableSet<OpenOption> addWrite(Collection<? extends OpenOption> options) {
        return options.contains(StandardOpenOption.WRITE) ? ImmutableSet.copyOf(options) : ((ImmutableSet.Builder) ((ImmutableSet.Builder) ImmutableSet.builder().add(StandardOpenOption.WRITE)).addAll(options)).build();
    }

    public static ImmutableSet<CopyOption> getMoveOptions(CopyOption... options) {
        return ImmutableSet.copyOf(Lists.asList(LinkOption.NOFOLLOW_LINKS, options));
    }

    public static ImmutableSet<CopyOption> getCopyOptions(CopyOption... options) {
        ImmutableSet<CopyOption> result = ImmutableSet.copyOf(options);
        if (result.contains(StandardCopyOption.ATOMIC_MOVE)) {
            throw new UnsupportedOperationException("'ATOMIC_MOVE' not allowed");
        }
        return result;
    }
}
