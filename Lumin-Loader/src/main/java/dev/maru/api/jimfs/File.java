package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.locks.ReadWriteLock;

public abstract class File {
    private final int id;
    private int links;
    private FileTime creationTime;
    private FileTime lastAccessTime;
    private FileTime lastModifiedTime;
    private @Nullable Table<String, String, Object> attributes;

    File(int id, FileTime creationTime) {
        this.id = id;
        this.creationTime = creationTime;
        this.lastAccessTime = creationTime;
        this.lastModifiedTime = creationTime;
    }

    public int id() {
        return this.id;
    }

    public long size() {
        return 0L;
    }

    public final boolean isDirectory() {
        return this instanceof Directory;
    }

    public final boolean isRegularFile() {
        return this instanceof RegularFile;
    }

    public final boolean isSymbolicLink() {
        return this instanceof SymbolicLink;
    }

    abstract File copyWithoutContent(int var1, FileTime var2);

    void copyContentTo(File file) throws IOException {
    }

    @Nullable ReadWriteLock contentLock() {
        return null;
    }

    void opened() {
    }

    void closed() {
    }

    void deleted() {
    }

    final boolean isRootDirectory() {
        return this.isDirectory() && this.equals(((Directory) this).parent());
    }

    public final synchronized int links() {
        return this.links;
    }

    void linked(DirectoryEntry entry) {
        Preconditions.checkNotNull(entry);
    }

    void unlinked() {
    }

    final synchronized void incrementLinkCount() {
        ++this.links;
    }

    final synchronized void decrementLinkCount() {
        --this.links;
    }

    public final synchronized FileTime getCreationTime() {
        return this.creationTime;
    }

    public final synchronized FileTime getLastAccessTime() {
        return this.lastAccessTime;
    }

    public final synchronized FileTime getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    final synchronized void setCreationTime(FileTime creationTime) {
        this.creationTime = creationTime;
    }

    final synchronized void setLastAccessTime(FileTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    final synchronized void setLastModifiedTime(FileTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public final synchronized ImmutableSet<String> getAttributeNames(String view) {
        if (this.attributes == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(this.attributes.row(view).keySet());
    }

    @VisibleForTesting
    final synchronized ImmutableSet<String> getAttributeKeys() {
        if (this.attributes == null) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder builder = ImmutableSet.builder();
        for (Table.Cell<String, String, Object> cell : this.attributes.cellSet()) {
            builder.add(cell.getRowKey() + ':' + cell.getColumnKey());
        }
        return builder.build();
    }

    public final synchronized @Nullable Object getAttribute(String view, String attribute) {
        if (this.attributes == null) {
            return null;
        }
        return this.attributes.get(view, attribute);
    }

    public final synchronized void setAttribute(String view, String attribute, Object value) {
        if (this.attributes == null) {
            this.attributes = HashBasedTable.create();
        }
        this.attributes.put(view, attribute, value);
    }

    public final synchronized void deleteAttribute(String view, String attribute) {
        if (this.attributes != null) {
            this.attributes.remove(view, attribute);
        }
    }

    final synchronized void copyBasicAttributes(File target) {
        target.setFileTimes(this.creationTime, this.lastModifiedTime, this.lastAccessTime);
    }

    private synchronized void setFileTimes(FileTime creationTime, FileTime lastModifiedTime, FileTime lastAccessTime) {
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
        this.lastAccessTime = lastAccessTime;
    }

    final synchronized void copyAttributes(File target) {
        this.copyBasicAttributes(target);
        target.putAll(this.attributes);
    }

    private synchronized void putAll(@Nullable Table<String, String, Object> attributes) {
        if (attributes != null && this.attributes != attributes) {
            if (this.attributes == null) {
                this.attributes = HashBasedTable.create();
            }
            this.attributes.putAll(attributes);
        }
    }

    public final String toString() {
        return MoreObjects.toStringHelper(this).add("id", this.id()).toString();
    }
}
