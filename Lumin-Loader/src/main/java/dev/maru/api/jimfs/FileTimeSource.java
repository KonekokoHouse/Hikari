package dev.maru.api.jimfs;

import java.nio.file.attribute.FileTime;

public interface FileTimeSource {
    public FileTime now();
}
