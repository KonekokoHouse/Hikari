package dev.maru.api.jimfs;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.Nullable;

import java.nio.file.*;
import java.util.Objects;

final class DirectoryEntry {
    private final Directory directory;
    private final Name name;
    private final @Nullable File file;
    @Nullable DirectoryEntry next;

    DirectoryEntry(Directory directory, Name name, @Nullable File file) {
        this.directory = Preconditions.checkNotNull(directory);
        this.name = Preconditions.checkNotNull(name);
        this.file = file;
    }

    public boolean exists() {
        return this.file != null;
    }

    @CanIgnoreReturnValue
    public DirectoryEntry requireExists(Path pathForException) throws NoSuchFileException {
        if (!this.exists()) {
            throw new NoSuchFileException(pathForException.toString());
        }
        return this;
    }

    @CanIgnoreReturnValue
    public DirectoryEntry requireDoesNotExist(Path pathForException) throws FileAlreadyExistsException {
        if (this.exists()) {
            throw new FileAlreadyExistsException(pathForException.toString());
        }
        return this;
    }

    @CanIgnoreReturnValue
    public DirectoryEntry requireDirectory(Path pathForException) throws NoSuchFileException, NotDirectoryException {
        this.requireExists(pathForException);
        if (!this.file().isDirectory()) {
            throw new NotDirectoryException(pathForException.toString());
        }
        return this;
    }

    @CanIgnoreReturnValue
    public DirectoryEntry requireSymbolicLink(Path pathForException) throws NoSuchFileException, NotLinkException {
        this.requireExists(pathForException);
        if (!this.file().isSymbolicLink()) {
            throw new NotLinkException(pathForException.toString());
        }
        return this;
    }

    public Directory directory() {
        return this.directory;
    }

    public Name name() {
        return this.name;
    }

    public File file() {
        Preconditions.checkState(this.exists());
        return this.file;
    }

    public @Nullable File fileOrNull() {
        return this.file;
    }

    public boolean equals(Object obj) {
        if (obj instanceof DirectoryEntry) {
            DirectoryEntry other = (DirectoryEntry) obj;
            return this.directory.equals(other.directory) && this.name.equals(other.name) && Objects.equals(this.file, other.file);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.directory, this.name, this.file);
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add("directory", this.directory).add("name", this.name).add("file", this.file).toString();
    }
}
