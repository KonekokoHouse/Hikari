package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class PathService
        implements Comparator<JimfsPath> {
    private static final Comparator<Name> DISPLAY_ROOT_COMPARATOR = Comparator.nullsLast(Name.displayComparator());
    private static final Comparator<Iterable<Name>> DISPLAY_NAMES_COMPARATOR = Comparators.lexicographical(Name.displayComparator());
    private static final Comparator<Name> CANONICAL_ROOT_COMPARATOR = Comparator.nullsLast(Name.canonicalComparator());
    private static final Comparator<Iterable<Name>> CANONICAL_NAMES_COMPARATOR = Comparators.lexicographical(Name.canonicalComparator());
    private final PathType type;
    private final ImmutableSet<PathNormalization> displayNormalizations;
    private final ImmutableSet<PathNormalization> canonicalNormalizations;
    private final boolean equalityUsesCanonicalForm;
    private final Comparator<Name> rootComparator;
    private final Comparator<Iterable<Name>> namesComparator;
    private volatile FileSystem fileSystem;
    private volatile JimfsPath emptyPath;
    private static final Predicate<Object> NOT_EMPTY = new Predicate<Object>() {

        @Override
        public boolean apply(Object input) {
            return !input.toString().isEmpty();
        }
    };

    PathService(Configuration config) {
        this(config.pathType, config.nameDisplayNormalization, config.nameCanonicalNormalization, config.pathEqualityUsesCanonicalForm);
    }

    PathService(PathType type, Iterable<PathNormalization> displayNormalizations, Iterable<PathNormalization> canonicalNormalizations, boolean equalityUsesCanonicalForm) {
        this.type = Preconditions.checkNotNull(type);
        this.displayNormalizations = ImmutableSet.copyOf(displayNormalizations);
        this.canonicalNormalizations = ImmutableSet.copyOf(canonicalNormalizations);
        this.equalityUsesCanonicalForm = equalityUsesCanonicalForm;
        this.rootComparator = equalityUsesCanonicalForm ? CANONICAL_ROOT_COMPARATOR : DISPLAY_ROOT_COMPARATOR;
        this.namesComparator = equalityUsesCanonicalForm ? CANONICAL_NAMES_COMPARATOR : DISPLAY_NAMES_COMPARATOR;
    }

    public void setFileSystem(FileSystem fileSystem) {
        Preconditions.checkState(this.fileSystem == null, "may not set fileSystem twice");
        this.fileSystem = Preconditions.checkNotNull(fileSystem);
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public String getSeparator() {
        return this.type.getSeparator();
    }

    public JimfsPath emptyPath() {
        JimfsPath result = this.emptyPath;
        if (result == null) {
            this.emptyPath = result = this.createPathInternal(null, ImmutableList.of(Name.EMPTY));
            return result;
        }
        return result;
    }

    public Name name(String name) {
        switch (name) {
            case "": {
                return Name.EMPTY;
            }
            case ".": {
                return Name.SELF;
            }
            case "..": {
                return Name.PARENT;
            }
        }
        String display = PathNormalization.normalize(name, this.displayNormalizations);
        String canonical = PathNormalization.normalize(name, this.canonicalNormalizations);
        return Name.create(display, canonical);
    }

    @VisibleForTesting
    List<Name> names(Iterable<String> names) {
        ArrayList<Name> result = new ArrayList<Name>();
        for (String name : names) {
            result.add(this.name(name));
        }
        return result;
    }

    public JimfsPath createRoot(Name root) {
        return this.createPath(Preconditions.checkNotNull(root), ImmutableList.of());
    }

    public JimfsPath createFileName(Name name) {
        return this.createPath(null, ImmutableList.of(name));
    }

    public JimfsPath createRelativePath(Iterable<Name> names) {
        return this.createPath(null, ImmutableList.copyOf(names));
    }

    public JimfsPath createPath(@Nullable Name root, Iterable<Name> names) {
        ImmutableList<Name> nameList = ImmutableList.copyOf(Iterables.filter(names, NOT_EMPTY));
        if (root == null && nameList.isEmpty()) {
            return this.emptyPath();
        }
        return this.createPathInternal(root, nameList);
    }

    protected final JimfsPath createPathInternal(@Nullable Name root, Iterable<Name> names) {
        return new JimfsPath(this, root, names);
    }

    public JimfsPath parsePath(String first, String... more) {
        String joined = this.type.joiner().join(Iterables.filter(Lists.asList(first, more), NOT_EMPTY));
        return this.toPath(this.type.parsePath(joined));
    }

    private JimfsPath toPath(PathType.ParseResult parsed) {
        Name root = parsed.root() == null ? null : this.name(parsed.root());
        List<Name> names = this.names(parsed.names());
        return this.createPath(root, names);
    }

    public String toString(JimfsPath path) {
        Name root = path.root();
        String rootString = root == null ? null : root.toString();
        Iterable<String> names = Iterables.transform(path.names(), Functions.toStringFunction());
        return this.type.toString(rootString, names);
    }

    public int hash(JimfsPath path) {
        int hash = 31;
        hash = 31 * hash + this.getFileSystem().hashCode();
        Name root = path.root();
        ImmutableList<Name> names = path.names();
        if (this.equalityUsesCanonicalForm) {
            hash = 31 * hash + (root == null ? 0 : root.hashCode());
            for (Name name : names) {
                hash = 31 * hash + name.hashCode();
            }
        } else {
            hash = 31 * hash + (root == null ? 0 : root.toString().hashCode());
            for (Name name : names) {
                hash = 31 * hash + name.toString().hashCode();
            }
        }
        return hash;
    }

    @Override
    public int compare(JimfsPath a, JimfsPath b) {
        Comparator<JimfsPath> comparator = Comparator.comparing(JimfsPath::root, this.rootComparator).thenComparing(JimfsPath::names, this.namesComparator);
        return comparator.compare(a, b);
    }

    public URI toUri(URI fileSystemUri, JimfsPath path) {
        Preconditions.checkArgument(path.isAbsolute(), "path (%s) must be absolute", (Object) path);
        String root = String.valueOf(path.root());
        Iterable<String> names = Iterables.transform(path.names(), Functions.toStringFunction());
        return this.type.toUri(fileSystemUri, root, names, Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS));
    }

    public JimfsPath fromUri(URI uri) {
        return this.toPath(this.type.fromUri(uri));
    }

    public PathMatcher createPathMatcher(String syntaxAndPattern) {
        return PathMatchers.getPathMatcher(syntaxAndPattern, this.type.getSeparator() + this.type.getOtherSeparators(), this.equalityUsesCanonicalForm ? this.canonicalNormalizations : this.displayNormalizations);
    }
}
