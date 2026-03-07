package dev.maru.api.jimfs;

import java.io.IOException;

public interface FileLookup {
    public File lookup() throws IOException;
}
