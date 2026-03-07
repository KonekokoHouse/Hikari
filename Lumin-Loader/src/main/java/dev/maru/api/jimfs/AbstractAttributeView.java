package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.attribute.FileAttributeView;

abstract class AbstractAttributeView
        implements FileAttributeView {
    private final FileLookup lookup;

    protected AbstractAttributeView(FileLookup lookup) {
        this.lookup = Preconditions.checkNotNull(lookup);
    }

    protected final File lookupFile() throws IOException {
        return this.lookup.lookup();
    }
}
