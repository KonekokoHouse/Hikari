package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class FileTree {
    private static final int MAX_SYMBOLIC_LINK_DEPTH = 40;
    private static final ImmutableList<Name> EMPTY_PATH_NAMES = ImmutableList.of(Name.SELF);
    private final ImmutableSortedMap<Name, Directory> roots;

    FileTree(Map<Name, Directory> roots) {
        this.roots = ImmutableSortedMap.copyOf(roots, Name.canonicalComparator());
    }

    public ImmutableSortedSet<Name> getRootDirectoryNames() {
        return this.roots.keySet();
    }

    public @Nullable DirectoryEntry getRoot(Name name) {
        Directory dir = this.roots.get(name);
        return dir == null ? null : dir.entryInParent();
    }

    public DirectoryEntry lookUp(File workingDirectory, JimfsPath path, Set<? super LinkOption> options) throws IOException {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(options);
        DirectoryEntry result = this.lookUp(workingDirectory, path, options, 0);
        if (result == null) {
            throw new NoSuchFileException(path.toString());
        }
        return result;
    }

    private @Nullable DirectoryEntry lookUp(File dir, JimfsPath path, Set<? super LinkOption> options, int linkDepth) throws IOException {
        ImmutableList<Name> names = path.names();
        if (path.isAbsolute()) {
            DirectoryEntry entry = this.getRoot(path.root());
            if (entry == null) {
                return null;
            }
            if (names.isEmpty()) {
                return entry;
            }
            dir = entry.file();
        } else if (FileTree.isEmpty(names)) {
            names = EMPTY_PATH_NAMES;
        }
        return this.lookUp(dir, names, options, linkDepth);
    }

    private @Nullable DirectoryEntry lookUp(File dir, Iterable<Name> names, Set<? super LinkOption> options, int linkDepth) throws IOException {
        Iterator<Name> nameIterator = names.iterator();
        Name name = nameIterator.next();
        while (nameIterator.hasNext()) {
            Directory directory = this.toDirectory(dir);
            if (directory == null) {
                return null;
            }
            DirectoryEntry entry = directory.get(name);
            if (entry == null) {
                return null;
            }
            File file = entry.file();
            if (file.isSymbolicLink()) {
                DirectoryEntry linkResult = this.followSymbolicLink(dir, (SymbolicLink) file, linkDepth);
                if (linkResult == null) {
                    return null;
                }
                dir = linkResult.fileOrNull();
            } else {
                dir = file;
            }
            name = nameIterator.next();
        }
        return this.lookUpLast(dir, name, options, linkDepth);
    }

    private @Nullable DirectoryEntry lookUpLast(@Nullable File dir, Name name, Set<? super LinkOption> options, int linkDepth) throws IOException {
        Directory directory = this.toDirectory(dir);
        if (directory == null) {
            return null;
        }
        DirectoryEntry entry = directory.get(name);
        if (entry == null) {
            return new DirectoryEntry(directory, name, null);
        }
        File file = entry.file();
        if (!options.contains(LinkOption.NOFOLLOW_LINKS) && file.isSymbolicLink()) {
            return this.followSymbolicLink(dir, (SymbolicLink) file, linkDepth);
        }
        return this.getRealEntry(entry);
    }

    private @Nullable DirectoryEntry followSymbolicLink(File dir, SymbolicLink link, int linkDepth) throws IOException {
        if (linkDepth >= 40) {
            throw new IOException("too many levels of symbolic links");
        }
        return this.lookUp(dir, link.target(), (Set<? super LinkOption>) Options.FOLLOW_LINKS, linkDepth + 1);
    }

    private @Nullable DirectoryEntry getRealEntry(DirectoryEntry entry) {
        Name name = entry.name();
        if (name.equals(Name.SELF) || name.equals(Name.PARENT)) {
            Directory dir = this.toDirectory(entry.file());
            assert (dir != null);
            return dir.entryInParent();
        }
        return entry;
    }

    private @Nullable Directory toDirectory(@Nullable File file) {
        return file == null || !file.isDirectory() ? null : (Directory) file;
    }

    private static boolean isEmpty(ImmutableList<Name> names) {
        return names.isEmpty() || names.size() == 1 && ((Name) names.get(0)).toString().isEmpty();
    }
}
