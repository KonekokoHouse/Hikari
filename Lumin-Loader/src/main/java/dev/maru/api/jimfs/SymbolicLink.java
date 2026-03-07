package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;

import java.nio.file.attribute.FileTime;

final class SymbolicLink
        extends File {
    private final JimfsPath target;

    public static SymbolicLink create(int id, FileTime creationTime, JimfsPath target) {
        return new SymbolicLink(id, creationTime, target);
    }

    private SymbolicLink(int id, FileTime creationTime, JimfsPath target) {
        super(id, creationTime);
        this.target = Preconditions.checkNotNull(target);
    }

    JimfsPath target() {
        return this.target;
    }

    @Override
    File copyWithoutContent(int id, FileTime creationTime) {
        return SymbolicLink.create(id, creationTime, this.target);
    }
}
