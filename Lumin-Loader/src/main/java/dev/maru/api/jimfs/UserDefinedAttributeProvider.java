package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;

final class UserDefinedAttributeProvider
        extends AttributeProvider {
    UserDefinedAttributeProvider() {
    }

    @Override
    public String name() {
        return "user";
    }

    @Override
    public ImmutableSet<String> fixedAttributes() {
        return ImmutableSet.of();
    }

    @Override
    public boolean supports(String attribute) {
        return true;
    }

    @Override
    public ImmutableSet<String> attributes(File file) {
        return UserDefinedAttributeProvider.userDefinedAttributes(file);
    }

    private static ImmutableSet<String> userDefinedAttributes(File file) {
        ImmutableSet.Builder builder = ImmutableSet.builder();
        for (String attribute : file.getAttributeNames("user")) {
            builder.add(attribute);
        }
        return builder.build();
    }

    @Override
    public @Nullable Object get(File file, String attribute) {
        Object value = file.getAttribute("user", attribute);
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            return bytes.clone();
        }
        return null;
    }

    @Override
    public void set(File file, String view, String attribute, Object value, boolean create) {
        byte[] bytes;
        Preconditions.checkNotNull(value);
        UserDefinedAttributeProvider.checkNotCreate(view, attribute, create);
        if (value instanceof byte[]) {
            bytes = (byte[]) ((byte[]) value).clone();
        } else if (value instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) value;
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        } else {
            throw UserDefinedAttributeProvider.invalidType(view, attribute, value, byte[].class, ByteBuffer.class);
        }
        file.setAttribute("user", attribute, bytes);
    }

    public Class<UserDefinedFileAttributeView> viewType() {
        return UserDefinedFileAttributeView.class;
    }

    @Override
    public UserDefinedFileAttributeView view(FileLookup lookup, ImmutableMap<String, FileAttributeView> inheritedViews) {
        return new View(lookup);
    }

    private static class View
            extends AbstractAttributeView
            implements UserDefinedFileAttributeView {
        public View(FileLookup lookup) {
            super(lookup);
        }

        @Override
        public String name() {
            return "user";
        }

        @Override
        public List<String> list() throws IOException {
            return UserDefinedAttributeProvider.userDefinedAttributes(this.lookupFile()).asList();
        }

        private byte[] getStoredBytes(String name) throws IOException {
            byte[] bytes = (byte[]) this.lookupFile().getAttribute(this.name(), name);
            if (bytes == null) {
                throw new IllegalArgumentException("attribute '" + this.name() + ":" + name + "' is not set");
            }
            return bytes;
        }

        @Override
        public int size(String name) throws IOException {
            return this.getStoredBytes(name).length;
        }

        @Override
        public int read(String name, ByteBuffer dst) throws IOException {
            byte[] bytes = this.getStoredBytes(name);
            dst.put(bytes);
            return bytes.length;
        }

        @Override
        public int write(String name, ByteBuffer src) throws IOException {
            byte[] bytes = new byte[src.remaining()];
            src.get(bytes);
            this.lookupFile().setAttribute(this.name(), name, bytes);
            return bytes.length;
        }

        @Override
        public void delete(String name) throws IOException {
            this.lookupFile().deleteAttribute(this.name(), name);
        }
    }
}
