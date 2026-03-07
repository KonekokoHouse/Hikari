package dev.maru.api.jimfs;

import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WindowsPathType
        extends PathType {
    static final WindowsPathType INSTANCE = new WindowsPathType();
    private static final Pattern WORKING_DIR_WITH_DRIVE = Pattern.compile("^[a-zA-Z]:([^\\\\].*)?$");
    private static final Pattern TRAILING_SPACES = Pattern.compile("[ ]+(\\\\|$)");
    private static final Pattern UNC_ROOT = Pattern.compile("^(\\\\\\\\)([^\\\\]+)?(\\\\[^\\\\]+)?");
    private static final Pattern DRIVE_LETTER_ROOT = Pattern.compile("^[a-zA-Z]:\\\\");

    private WindowsPathType() {
        super(true, '\\', '/');
    }

    @Override
    public PathType.ParseResult parsePath(String path) {
        int startIndex;
        String root;
        String original = path;
        if (WORKING_DIR_WITH_DRIVE.matcher(path = path.replace('/', '\\')).matches()) {
            throw new InvalidPathException(original, "Jimfs does not currently support the Windows syntax for a relative path on a specific drive (e.g. \"C:foo\\bar\")");
        }
        if (path.startsWith("\\\\")) {
            root = this.parseUncRoot(path, original);
        } else {
            if (path.startsWith("\\")) {
                throw new InvalidPathException(original, "Jimfs does not currently support the Windows syntax for an absolute path on the current drive (e.g. \"\\foo\\bar\")");
            }
            root = this.parseDriveRoot(path);
        }
        for (int i = startIndex = root == null || root.length() > 3 ? 0 : root.length(); i < path.length(); ++i) {
            char c = path.charAt(i);
            if (!WindowsPathType.isReserved(c)) continue;
            throw new InvalidPathException(original, "Illegal char <" + c + ">", i);
        }
        Matcher trailingSpaceMatcher = TRAILING_SPACES.matcher(path);
        if (trailingSpaceMatcher.find()) {
            throw new InvalidPathException(original, "Trailing char < >", trailingSpaceMatcher.start());
        }
        if (root != null) {
            path = path.substring(root.length());
            if (!root.endsWith("\\")) {
                root = root + "\\";
            }
        }
        return new PathType.ParseResult(root, this.splitter().split(path));
    }

    private String parseUncRoot(String path, String original) {
        Matcher uncMatcher = UNC_ROOT.matcher(path);
        if (uncMatcher.find()) {
            String host = uncMatcher.group(2);
            if (host == null) {
                throw new InvalidPathException(original, "UNC path is missing hostname");
            }
            String share = uncMatcher.group(3);
            if (share == null) {
                throw new InvalidPathException(original, "UNC path is missing sharename");
            }
            return path.substring(uncMatcher.start(), uncMatcher.end());
        }
        throw new InvalidPathException(original, "Invalid UNC path");
    }

    private @Nullable String parseDriveRoot(String path) {
        Matcher drivePathMatcher = DRIVE_LETTER_ROOT.matcher(path);
        if (drivePathMatcher.find()) {
            return path.substring(drivePathMatcher.start(), drivePathMatcher.end());
        }
        return null;
    }

    private static boolean isReserved(char c) {
        switch (c) {
            case '\"':
            case '*':
            case ':':
            case '<':
            case '>':
            case '?':
            case '|': {
                return true;
            }
        }
        return c <= '\u001f';
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
        root = root.startsWith("\\\\") ? root.replace('\\', '/') : "/" + root.replace('\\', '/');
        StringBuilder builder = new StringBuilder();
        builder.append(root);
        Iterator<String> iter = names.iterator();
        if (iter.hasNext()) {
            builder.append(iter.next());
            while (iter.hasNext()) {
                builder.append('/').append(iter.next());
            }
        }
        if (directory && builder.charAt(builder.length() - 1) != '/') {
            builder.append('/');
        }
        return builder.toString();
    }

    @Override
    public PathType.ParseResult parseUriPath(String uriPath) {
        if ((uriPath = uriPath.replace('/', '\\')).charAt(0) == '\\' && uriPath.charAt(1) != '\\') {
            uriPath = uriPath.substring(1);
        }
        return this.parsePath(uriPath);
    }
}
