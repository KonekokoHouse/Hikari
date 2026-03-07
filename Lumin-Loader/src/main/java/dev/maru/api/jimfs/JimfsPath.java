package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

final class JimfsPath
        implements Path {
    private final @Nullable Name root;
    private final ImmutableList<Name> names;
    private final PathService pathService;

    public JimfsPath(PathService pathService, @Nullable Name root, Iterable<Name> names) {
        this.pathService = Preconditions.checkNotNull(pathService);
        this.root = root;
        this.names = ImmutableList.copyOf(names);
    }

    public @Nullable Name root() {
        return this.root;
    }

    public ImmutableList<Name> names() {
        return this.names;
    }

    public @Nullable Name name() {
        if (!this.names.isEmpty()) {
            return Iterables.getLast(this.names);
        }
        return this.root;
    }

    public boolean isEmptyPath() {
        return this.root == null && this.names.size() == 1 && ((Name) this.names.get(0)).toString().isEmpty();
    }

    @Override
    public FileSystem getFileSystem() {
        return this.pathService.getFileSystem();
    }

    public JimfsFileSystem getJimfsFileSystem() {
        return (JimfsFileSystem) this.pathService.getFileSystem();
    }

    @Override
    public boolean isAbsolute() {
        return this.root != null;
    }

    @Override
    public @Nullable JimfsPath getRoot() {
        if (this.root == null) {
            return null;
        }
        return this.pathService.createRoot(this.root);
    }

    @Override
    public @Nullable JimfsPath getFileName() {
        return this.names.isEmpty() ? null : this.getName(this.names.size() - 1);
    }

    @Override
    public @Nullable JimfsPath getParent() {
        if (this.names.isEmpty() || this.names.size() == 1 && this.root == null) {
            return null;
        }
        return this.pathService.createPath(this.root, this.names.subList(0, this.names.size() - 1));
    }

    @Override
    public int getNameCount() {
        return this.names.size();
    }

    @Override
    public JimfsPath getName(int index) {
        Preconditions.checkArgument(index >= 0 && index < this.names.size(), "index (%s) must be >= 0 and < name count (%s)", index, this.names.size());
        return this.pathService.createFileName((Name) this.names.get(index));
    }

    @Override
    public JimfsPath subpath(int beginIndex, int endIndex) {
        Preconditions.checkArgument(beginIndex >= 0 && endIndex <= this.names.size() && endIndex > beginIndex, "beginIndex (%s) must be >= 0; endIndex (%s) must be <= name count (%s) and > beginIndex", (Object) beginIndex, (Object) endIndex, (Object) this.names.size());
        return this.pathService.createRelativePath(this.names.subList(beginIndex, endIndex));
    }

    private static boolean startsWith(List<?> list, List<?> other) {
        return list.size() >= other.size() && list.subList(0, other.size()).equals(other);
    }

    @Override
    public boolean startsWith(Path other) {
        JimfsPath otherPath = this.checkPath(other);
        return otherPath != null && this.getFileSystem().equals(otherPath.getFileSystem()) && Objects.equals(this.root, otherPath.root) && JimfsPath.startsWith(this.names, otherPath.names);
    }

    @Override
    public boolean startsWith(String other) {
        return this.startsWith(this.pathService.parsePath(other, new String[0]));
    }

    @Override
    public boolean endsWith(Path other) {
        JimfsPath otherPath = this.checkPath(other);
        if (otherPath == null) {
            return false;
        }
        if (otherPath.isAbsolute()) {
            return this.compareTo(otherPath) == 0;
        }
        return JimfsPath.startsWith(this.names.reverse(), otherPath.names.reverse());
    }

    @Override
    public boolean endsWith(String other) {
        return this.endsWith(this.pathService.parsePath(other, new String[0]));
    }

    @Override
    public JimfsPath normalize() {
        if (this.isNormal()) {
            return this;
        }
        ArrayDeque<Name> newNames = new ArrayDeque<Name>();
        for (Name name : this.names) {
            if (name.equals(Name.PARENT)) {
                Name lastName = (Name) newNames.peekLast();
                if (lastName != null && !lastName.equals(Name.PARENT)) {
                    newNames.removeLast();
                    continue;
                }
                if (this.isAbsolute()) continue;
                newNames.add(name);
                continue;
            }
            if (name.equals(Name.SELF)) continue;
            newNames.add(name);
        }
        return Iterables.elementsEqual(newNames, this.names) ? this : this.pathService.createPath(this.root, newNames);
    }

    private boolean isNormal() {
        if (this.getNameCount() == 0 || this.getNameCount() == 1 && !this.isAbsolute()) {
            return true;
        }
        boolean foundNonParentName = this.isAbsolute();
        boolean normal = true;
        for (Name name : this.names) {
            if (name.equals(Name.PARENT)) {
                if (!foundNonParentName) continue;
                normal = false;
                break;
            }
            if (name.equals(Name.SELF)) {
                normal = false;
                break;
            }
            foundNonParentName = true;
        }
        return normal;
    }

    JimfsPath resolve(Name name) {
        return this.resolve(this.pathService.createFileName(name));
    }

    @Override
    public JimfsPath resolve(Path other) {
        JimfsPath otherPath = this.checkPath(other);
        if (otherPath == null) {
            throw new ProviderMismatchException(other.toString());
        }
        if (this.isEmptyPath() || otherPath.isAbsolute()) {
            return otherPath;
        }
        if (otherPath.isEmptyPath()) {
            return this;
        }
        return this.pathService.createPath(this.root, ((ImmutableList.Builder) ((ImmutableList.Builder) ImmutableList.builder().addAll(this.names)).addAll(otherPath.names)).build());
    }

    @Override
    public JimfsPath resolve(String other) {
        return this.resolve(this.pathService.parsePath(other, new String[0]));
    }

    @Override
    public JimfsPath resolveSibling(Path other) {
        JimfsPath otherPath = this.checkPath(other);
        if (otherPath == null) {
            throw new ProviderMismatchException(other.toString());
        }
        if (otherPath.isAbsolute()) {
            return otherPath;
        }
        JimfsPath parent = this.getParent();
        if (parent == null) {
            return otherPath;
        }
        return parent.resolve(other);
    }

    @Override
    public JimfsPath resolveSibling(String other) {
        return this.resolveSibling(this.pathService.parsePath(other, new String[0]));
    }

    @Override
    public JimfsPath relativize(Path other) {
        JimfsPath otherPath = this.checkPath(other);
        if (otherPath == null) {
            throw new ProviderMismatchException(other.toString());
        }
        Preconditions.checkArgument(Objects.equals(this.root, otherPath.root), "Paths have different roots: %s, %s", (Object) this, (Object) other);
        if (this.equals(other)) {
            return this.pathService.emptyPath();
        }
        if (this.isEmptyPath()) {
            return otherPath;
        }
        ImmutableList<Name> otherNames = otherPath.names;
        int sharedSubsequenceLength = 0;
        for (int i = 0; i < Math.min(this.getNameCount(), otherNames.size()) && ((Name) this.names.get(i)).equals(otherNames.get(i)); ++i) {
            ++sharedSubsequenceLength;
        }
        int extraNamesInThis = Math.max(0, this.getNameCount() - sharedSubsequenceLength);
        ImmutableList extraNamesInOther = otherNames.size() <= sharedSubsequenceLength ? ImmutableList.of() : otherNames.subList(sharedSubsequenceLength, otherNames.size());
        ArrayList<Name> parts = new ArrayList<Name>(extraNamesInThis + extraNamesInOther.size());
        parts.addAll(Collections.nCopies(extraNamesInThis, Name.PARENT));
        parts.addAll(extraNamesInOther);
        return this.pathService.createRelativePath(parts);
    }

    @Override
    public JimfsPath toAbsolutePath() {
        return this.isAbsolute() ? this : this.getJimfsFileSystem().getWorkingDirectory().resolve(this);
    }

    @Override
    public JimfsPath toRealPath(LinkOption... options) throws IOException {
        return this.getJimfsFileSystem().getDefaultView().toRealPath(this, this.pathService, Options.getLinkOptions(options));
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        Preconditions.checkNotNull(modifiers);
        return this.register(watcher, events);
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        Preconditions.checkNotNull(watcher);
        Preconditions.checkNotNull(events);
        if (!(watcher instanceof AbstractWatchService)) {
            throw new IllegalArgumentException("watcher (" + watcher + ") is not associated with this file system");
        }
        AbstractWatchService service = (AbstractWatchService) watcher;
        return service.register(this, Arrays.asList(events));
    }

    @Override
    public URI toUri() {
        return this.getJimfsFileSystem().toUri(this);
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Path> iterator() {
        return this.asList().iterator();
    }

    private List<Path> asList() {
        return new AbstractList<Path>() {

            @Override
            public Path get(int index) {
                return JimfsPath.this.getName(index);
            }

            @Override
            public int size() {
                return JimfsPath.this.getNameCount();
            }
        };
    }

    @Override
    public int compareTo(Path other) {
        JimfsPath otherPath = (JimfsPath) other;
        Comparator<JimfsPath> comparator = Comparator.comparing((JimfsPath p) -> p.getJimfsFileSystem().getUri()).thenComparing(this.pathService);
        return comparator.compare(this, otherPath);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof JimfsPath && this.compareTo((JimfsPath) obj) == 0;
    }

    @Override
    public int hashCode() {
        return this.pathService.hash(this);
    }

    @Override
    public String toString() {
        return this.pathService.toString(this);
    }

    private @Nullable JimfsPath checkPath(Path other) {
        if (Preconditions.checkNotNull(other) instanceof JimfsPath && other.getFileSystem().equals(this.getFileSystem())) {
            return (JimfsPath) other;
        }
        return null;
    }
}
