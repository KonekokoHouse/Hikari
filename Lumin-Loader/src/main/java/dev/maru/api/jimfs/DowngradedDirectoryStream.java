package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.util.Iterator;

final class DowngradedDirectoryStream
        implements DirectoryStream<Path> {
    private final SecureDirectoryStream<Path> secureDirectoryStream;

    DowngradedDirectoryStream(SecureDirectoryStream<Path> secureDirectoryStream) {
        this.secureDirectoryStream = Preconditions.checkNotNull(secureDirectoryStream);
    }

    @Override
    public Iterator<Path> iterator() {
        return this.secureDirectoryStream.iterator();
    }

    @Override
    public void close() throws IOException {
        this.secureDirectoryStream.close();
    }
}
