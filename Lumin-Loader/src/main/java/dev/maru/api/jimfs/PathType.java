package dev.maru.api.jimfs;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public abstract class PathType {
    private final boolean allowsMultipleRoots;
    private final String separator;
    private final String otherSeparators;
    private final Joiner joiner;
    private final Splitter splitter;
    private static final char[] regexReservedChars = "^$.?+*\\[]{}()".toCharArray();

    public static PathType unix() {
        return UnixPathType.INSTANCE;
    }

    public static PathType windows() {
        return WindowsPathType.INSTANCE;
    }

    protected PathType(boolean allowsMultipleRoots, char separator, char... otherSeparators) {
        this.separator = String.valueOf(separator);
        this.allowsMultipleRoots = allowsMultipleRoots;
        this.otherSeparators = String.valueOf(otherSeparators);
        this.joiner = Joiner.on(separator);
        this.splitter = PathType.createSplitter(separator, otherSeparators);
    }

    private static boolean isRegexReserved(char c) {
        return Arrays.binarySearch(regexReservedChars, c) >= 0;
    }

    private static Splitter createSplitter(char separator, char... otherSeparators) {
        if (otherSeparators.length == 0) {
            return Splitter.on(separator).omitEmptyStrings();
        }
        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append("[");
        PathType.appendToRegex(separator, patternBuilder);
        for (char other : otherSeparators) {
            PathType.appendToRegex(other, patternBuilder);
        }
        patternBuilder.append("]");
        return Splitter.onPattern(patternBuilder.toString()).omitEmptyStrings();
    }

    private static void appendToRegex(char separator, StringBuilder patternBuilder) {
        if (PathType.isRegexReserved(separator)) {
            patternBuilder.append("\\");
        }
        patternBuilder.append(separator);
    }

    public final boolean allowsMultipleRoots() {
        return this.allowsMultipleRoots;
    }

    public final String getSeparator() {
        return this.separator;
    }

    public final String getOtherSeparators() {
        return this.otherSeparators;
    }

    public final Joiner joiner() {
        return this.joiner;
    }

    public final Splitter splitter() {
        return this.splitter;
    }

    protected final ParseResult emptyPath() {
        return new ParseResult(null, ImmutableList.of(""));
    }

    public abstract ParseResult parsePath(String var1);

    public String toString() {
        return this.getClass().getSimpleName();
    }

    public abstract String toString(@Nullable String var1, Iterable<String> var2);

    protected abstract String toUriPath(String var1, Iterable<String> var2, boolean var3);

    protected abstract ParseResult parseUriPath(String var1);

    public final URI toUri(URI fileSystemUri, String root, Iterable<String> names, boolean directory) {
        String path = this.toUriPath(root, names, directory);
        try {
            return new URI(fileSystemUri.getScheme(), fileSystemUri.getUserInfo(), fileSystemUri.getHost(), fileSystemUri.getPort(), path, null, null);
        } catch (URISyntaxException e) {
            throw new AssertionError((Object) e);
        }
    }

    public final ParseResult fromUri(URI uri) {
        return this.parseUriPath(uri.getPath());
    }

    static {
        Arrays.sort(regexReservedChars);
    }

    public static final class ParseResult {
        private final @Nullable String root;
        private final Iterable<String> names;

        public ParseResult(@Nullable String root, Iterable<String> names) {
            this.root = root;
            this.names = Preconditions.checkNotNull(names);
        }

        public boolean isAbsolute() {
            return this.root != null;
        }

        public boolean isRoot() {
            return this.root != null && Iterables.isEmpty(this.names);
        }

        public @Nullable String root() {
            return this.root;
        }

        public Iterable<String> names() {
            return this.names;
        }
    }
}
