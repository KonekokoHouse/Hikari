package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.Nullable;

import java.nio.file.attribute.FileTime;
import java.util.Iterator;

final class Directory
        extends File
        implements Iterable<DirectoryEntry> {
    private DirectoryEntry entryInParent;
    private static final int INITIAL_CAPACITY = 16;
    private static final int INITIAL_RESIZE_THRESHOLD = 12;
    private DirectoryEntry[] table = new DirectoryEntry[16];
    private int resizeThreshold = 12;
    private int entryCount;

    public static Directory create(int id, FileTime creationTime) {
        return new Directory(id, creationTime);
    }

    public static Directory createRoot(int id, FileTime creationTime, Name name) {
        return new Directory(id, creationTime, name);
    }

    private Directory(int id, FileTime creationTime) {
        super(id, creationTime);
        this.put(new DirectoryEntry(this, Name.SELF, this));
    }

    private Directory(int id, FileTime creationTime, Name rootName) {
        this(id, creationTime);
        this.linked(new DirectoryEntry(this, rootName, this));
    }

    @Override
    Directory copyWithoutContent(int id, FileTime creationTime) {
        return Directory.create(id, creationTime);
    }

    public DirectoryEntry entryInParent() {
        return this.entryInParent;
    }

    public Directory parent() {
        return this.entryInParent.directory();
    }

    @Override
    void linked(DirectoryEntry entry) {
        Directory parent = entry.directory();
        this.entryInParent = entry;
        this.forcePut(new DirectoryEntry(this, Name.PARENT, parent));
    }

    @Override
    void unlinked() {
        this.parent().decrementLinkCount();
    }

    @VisibleForTesting
    int entryCount() {
        return this.entryCount;
    }

    public boolean isEmpty() {
        return this.entryCount() == 2;
    }

    public @Nullable DirectoryEntry get(Name name) {
        int index = Directory.bucketIndex(name, this.table.length);
        DirectoryEntry entry = this.table[index];
        while (entry != null) {
            if (name.equals(entry.name())) {
                return entry;
            }
            entry = entry.next;
        }
        return null;
    }

    public void link(Name name, File file) {
        DirectoryEntry entry = new DirectoryEntry(this, Directory.checkNotReserved(name, "link"), file);
        this.put(entry);
        file.linked(entry);
    }

    public void unlink(Name name) {
        DirectoryEntry entry = this.remove(Directory.checkNotReserved(name, "unlink"));
        entry.file().unlinked();
    }

    public ImmutableSortedSet<Name> snapshot() {
        ImmutableSortedSet.Builder<Name> builder = new ImmutableSortedSet.Builder<Name>(Name.displayComparator());
        for (DirectoryEntry entry : this) {
            if (Directory.isReserved(entry.name())) continue;
            builder.add(entry.name());
        }
        return builder.build();
    }

    private static Name checkNotReserved(Name name, String action) {
        if (Directory.isReserved(name)) {
            throw new IllegalArgumentException("cannot " + action + ": " + name);
        }
        return name;
    }

    private static boolean isReserved(Name name) {
        return name == Name.SELF || name == Name.PARENT;
    }

    private static int bucketIndex(Name name, int tableLength) {
        return name.hashCode() & tableLength - 1;
    }

    @VisibleForTesting
    void put(DirectoryEntry entry) {
        this.put(entry, false);
    }

    private void put(DirectoryEntry entry, boolean overwriteExisting) {
        int index = Directory.bucketIndex(entry.name(), this.table.length);
        DirectoryEntry prev = null;
        DirectoryEntry curr = this.table[index];
        while (curr != null) {
            if (curr.name().equals(entry.name())) {
                if (overwriteExisting) {
                    if (prev != null) {
                        prev.next = entry;
                    } else {
                        this.table[index] = entry;
                    }
                    entry.next = curr.next;
                    curr.next = null;
                    entry.file().incrementLinkCount();
                    return;
                }
                throw new IllegalArgumentException("entry '" + entry.name() + "' already exists");
            }
            prev = curr;
            curr = curr.next;
        }
        ++this.entryCount;
        if (this.expandIfNeeded()) {
            index = Directory.bucketIndex(entry.name(), this.table.length);
            Directory.addToBucket(index, this.table, entry);
        } else if (prev != null) {
            prev.next = entry;
        } else {
            this.table[index] = entry;
        }
        entry.file().incrementLinkCount();
    }

    private void forcePut(DirectoryEntry entry) {
        this.put(entry, true);
    }

    private boolean expandIfNeeded() {
        if (this.entryCount <= this.resizeThreshold) {
            return false;
        }
        DirectoryEntry[] newTable = new DirectoryEntry[this.table.length << 1];
        for (DirectoryEntry entry : this.table) {
            while (entry != null) {
                int index = Directory.bucketIndex(entry.name(), newTable.length);
                Directory.addToBucket(index, newTable, entry);
                DirectoryEntry next = entry.next;
                entry.next = null;
                entry = next;
            }
        }
        this.table = newTable;
        this.resizeThreshold <<= 1;
        return true;
    }

    private static void addToBucket(int bucketIndex, DirectoryEntry[] table, DirectoryEntry entryToAdd) {
        DirectoryEntry prev = null;
        DirectoryEntry existing = table[bucketIndex];
        while (existing != null) {
            prev = existing;
            existing = existing.next;
        }
        if (prev != null) {
            prev.next = entryToAdd;
        } else {
            table[bucketIndex] = entryToAdd;
        }
    }

    @CanIgnoreReturnValue
    @VisibleForTesting
    DirectoryEntry remove(Name name) {
        int index = Directory.bucketIndex(name, this.table.length);
        DirectoryEntry prev = null;
        DirectoryEntry entry = this.table[index];
        while (entry != null) {
            if (name.equals(entry.name())) {
                if (prev != null) {
                    prev.next = entry.next;
                } else {
                    this.table[index] = entry.next;
                }
                entry.next = null;
                --this.entryCount;
                entry.file().decrementLinkCount();
                return entry;
            }
            prev = entry;
            entry = entry.next;
        }
        throw new IllegalArgumentException("no entry matching '" + name + "' in this directory");
    }

    @Override
    public Iterator<DirectoryEntry> iterator() {
        return new AbstractIterator<DirectoryEntry>() {
            int index;
            @Nullable DirectoryEntry entry;

            @Override
            protected DirectoryEntry computeNext() {
                if (this.entry != null) {
                    this.entry = this.entry.next;
                }
                while (this.entry == null && this.index < Directory.this.table.length) {
                    this.entry = Directory.this.table[this.index++];
                }
                return this.entry != null ? this.entry : (DirectoryEntry) this.endOfData();
            }
        };
    }
}
