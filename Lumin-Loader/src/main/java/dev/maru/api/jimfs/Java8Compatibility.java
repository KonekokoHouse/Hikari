package dev.maru.api.jimfs;

import java.nio.Buffer;

final class Java8Compatibility {
    static void clear(Buffer b) {
        b.clear();
    }

    private Java8Compatibility() {
    }
}
