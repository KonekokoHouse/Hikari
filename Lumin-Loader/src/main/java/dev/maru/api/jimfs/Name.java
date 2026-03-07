package dev.maru.api.jimfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

final class Name {
    static final Name EMPTY = new Name("", "");
    public static final Name SELF = new Name(".", ".");
    public static final Name PARENT = new Name("..", "..");
    private final String display;
    private final String canonical;
    private static final Comparator<Name> DISPLAY_COMPARATOR = Comparator.comparing(n -> n.display);
    private static final Comparator<Name> CANONICAL_COMPARATOR = Comparator.comparing(n -> n.canonical);

    @VisibleForTesting
    static Name simple(String name) {
        switch (name) {
            case ".": {
                return SELF;
            }
            case "..": {
                return PARENT;
            }
        }
        return new Name(name, name);
    }

    public static Name create(String display, String canonical) {
        return new Name(display, canonical);
    }

    private Name(String display, String canonical) {
        this.display = Preconditions.checkNotNull(display);
        this.canonical = Preconditions.checkNotNull(canonical);
    }

    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Name) {
            Name other = (Name) obj;
            return this.canonical.equals(other.canonical);
        }
        return false;
    }

    public int hashCode() {
        return Util.smearHash(this.canonical.hashCode());
    }

    public String toString() {
        return this.display;
    }

    static Comparator<Name> displayComparator() {
        return DISPLAY_COMPARATOR;
    }

    static Comparator<Name> canonicalComparator() {
        return CANONICAL_COMPARATOR;
    }
}
