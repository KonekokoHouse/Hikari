package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.util.Arrays;
import java.util.Map;

public abstract class AttributeProvider {
    public abstract String name();

    public ImmutableSet<String> inherits() {
        return ImmutableSet.of();
    }

    public abstract Class<? extends FileAttributeView> viewType();

    public abstract FileAttributeView view(FileLookup var1, ImmutableMap<String, FileAttributeView> var2);

    public ImmutableMap<String, ?> defaultValues(Map<String, ?> userDefaults) {
        return ImmutableMap.of();
    }

    public abstract ImmutableSet<String> fixedAttributes();

    public boolean supports(String attribute) {
        return this.fixedAttributes().contains(attribute);
    }

    public ImmutableSet<String> attributes(File file) {
        return this.fixedAttributes();
    }

    public abstract @Nullable Object get(File var1, String var2);

    public abstract void set(File var1, String var2, String var3, Object var4, boolean var5);

    public @Nullable Class<? extends BasicFileAttributes> attributesType() {
        return null;
    }

    public BasicFileAttributes readAttributes(File file) {
        throw new UnsupportedOperationException();
    }

    protected static RuntimeException unsettable(String view, String attribute, boolean create) {
        AttributeProvider.checkNotCreate(view, attribute, create);
        throw new IllegalArgumentException("cannot set attribute '" + view + ":" + attribute + "'");
    }

    protected static void checkNotCreate(String view, String attribute, boolean create) {
        if (create) {
            throw new UnsupportedOperationException("cannot set attribute '" + view + ":" + attribute + "' during file creation");
        }
    }

    protected static <T> T checkType(String view, String attribute, Object value, Class<T> type) {
        Preconditions.checkNotNull(value);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw AttributeProvider.invalidType(view, attribute, value, type);
    }

    protected static IllegalArgumentException invalidType(String view, String attribute, Object value, Class<?>... expectedTypes) {
        Class<?> expected = expectedTypes.length == 1 ? expectedTypes[0] : ("one of " + Arrays.toString(expectedTypes)).getClass();
        throw new IllegalArgumentException("invalid type " + value.getClass() + " for attribute '" + view + ":" + attribute + "': expected " + expected);
    }
}
