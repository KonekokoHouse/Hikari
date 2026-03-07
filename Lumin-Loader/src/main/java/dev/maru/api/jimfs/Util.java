package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;

final class Util {
    private static final int C1 = -862048943;
    private static final int C2 = 461845907;
    private static final int ARRAY_LEN = 8192;
    private static final byte[] ZERO_ARRAY = new byte[8192];
    private static final byte[][] NULL_ARRAY = new byte[8192][];

    private Util() {
    }

    public static int nextPowerOf2(int n) {
        if (n == 0) {
            return 1;
        }
        int b = Integer.highestOneBit(n);
        return b == n ? n : b << 1;
    }

    static void checkNotNegative(long n, String description) {
        Preconditions.checkArgument(n >= 0L, "%s must not be negative: %s", (Object) description, n);
    }

    static void checkNoneNull(Iterable<?> objects) {
        if (!(objects instanceof ImmutableCollection)) {
            for (Object o : objects) {
                Preconditions.checkNotNull(o);
            }
        }
    }

    static int smearHash(int hashCode) {
        return 461845907 * Integer.rotateLeft(hashCode * -862048943, 15);
    }

    static void zero(byte[] bytes, int off, int len) {
        int remaining;
        for (remaining = len; remaining > 8192; remaining -= 8192) {
            System.arraycopy(ZERO_ARRAY, 0, bytes, off, 8192);
            off += 8192;
        }
        System.arraycopy(ZERO_ARRAY, 0, bytes, off, remaining);
    }

    static void clear(byte[][] blocks, int off, int len) {
        int remaining;
        for (remaining = len; remaining > 8192; remaining -= 8192) {
            System.arraycopy(NULL_ARRAY, 0, blocks, off, 8192);
            off += 8192;
        }
        System.arraycopy(NULL_ARRAY, 0, blocks, off, remaining);
    }
}
