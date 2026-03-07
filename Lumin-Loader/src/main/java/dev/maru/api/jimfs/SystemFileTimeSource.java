package dev.maru.api.jimfs;

import java.nio.file.attribute.FileTime;
import java.time.Instant;

enum SystemFileTimeSource implements FileTimeSource {
    INSTANCE;


    @Override
    public FileTime now() {
        return FileTime.from(Instant.now());
    }

    public String toString() {
        return "SystemFileTimeSource";
    }
}
