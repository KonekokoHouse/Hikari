package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;

final class UnixPathType
        extends PathType {
    static final PathType INSTANCE = new UnixPathType();

    private UnixPathType() {
        super(false, '/', new char[0]);
    }

    @Override
    public PathType.ParseResult parsePath(String path) {
        if (path.isEmpty()) {
            return this.emptyPath();
        }
        UnixPathType.checkValid(path);
        String root = path.startsWith("/") ? "/" : null;
        return new PathType.ParseResult(root, this.splitter().split(path));
    }

    private static void checkValid(String path) {
        int nulIndex = path.indexOf(0);
        if (nulIndex != -1) {
            throw new InvalidPathException(path, "nul character not allowed", nulIndex);
        }
    }

    @Override
    public String toString(@Nullable String root, Iterable<String> names) {
        StringBuilder builder = new StringBuilder();
        if (root != null) {
            builder.append(root);
        }
        this.joiner().appendTo(builder, names);
        return builder.toString();
    }

    @Override
    public String toUriPath(String root, Iterable<String> names, boolean directory) {
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            builder.append('/').append(name);
        }
        if (directory || builder.length() == 0) {
            builder.append('/');
        }
        return builder.toString();
    }

    @Override
    public PathType.ParseResult parseUriPath(String uriPath) {
        Preconditions.checkArgument(uriPath.startsWith("/"), "uriPath (%s) must start with /", (Object) uriPath);
        return this.parsePath(uriPath);
    }
}
