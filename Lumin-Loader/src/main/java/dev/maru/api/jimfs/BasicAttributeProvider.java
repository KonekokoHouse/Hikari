package dev.maru.api.jimfs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;

final class BasicAttributeProvider
        extends AttributeProvider {
    private static final ImmutableSet<String> ATTRIBUTES = ImmutableSet.of("size", "fileKey", "isDirectory", "isRegularFile", "isSymbolicLink", "isOther", new String[]{"creationTime", "lastAccessTime", "lastModifiedTime"});

    BasicAttributeProvider() {
    }

    @Override
    public String name() {
        return "basic";
    }

    @Override
    public ImmutableSet<String> fixedAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public @Nullable Object get(File file, String attribute) {
        switch (attribute) {
            case "size": {
                return file.size();
            }
            case "fileKey": {
                return file.id();
            }
            case "isDirectory": {
                return file.isDirectory();
            }
            case "isRegularFile": {
                return file.isRegularFile();
            }
            case "isSymbolicLink": {
                return file.isSymbolicLink();
            }
            case "isOther": {
                return !file.isDirectory() && !file.isRegularFile() && !file.isSymbolicLink();
            }
            case "creationTime": {
                return file.getCreationTime();
            }
            case "lastAccessTime": {
                return file.getLastAccessTime();
            }
            case "lastModifiedTime": {
                return file.getLastModifiedTime();
            }
        }
        return null;
    }

    @Override
    public void set(File file, String view, String attribute, Object value, boolean create) {
        switch (attribute) {
            case "creationTime": {
                BasicAttributeProvider.checkNotCreate(view, attribute, create);
                file.setCreationTime(BasicAttributeProvider.checkType(view, attribute, value, FileTime.class));
                break;
            }
            case "lastAccessTime": {
                BasicAttributeProvider.checkNotCreate(view, attribute, create);
                file.setLastAccessTime(BasicAttributeProvider.checkType(view, attribute, value, FileTime.class));
                break;
            }
            case "lastModifiedTime": {
                BasicAttributeProvider.checkNotCreate(view, attribute, create);
                file.setLastModifiedTime(BasicAttributeProvider.checkType(view, attribute, value, FileTime.class));
                break;
            }
            case "size":
            case "fileKey":
            case "isDirectory":
            case "isRegularFile":
            case "isSymbolicLink":
            case "isOther": {
                throw BasicAttributeProvider.unsettable(view, attribute, create);
            }
        }
    }

    public Class<BasicFileAttributeView> viewType() {
        return BasicFileAttributeView.class;
    }

    @Override
    public BasicFileAttributeView view(FileLookup lookup, ImmutableMap<String, FileAttributeView> inheritedViews) {
        return new View(lookup);
    }

    public Class<BasicFileAttributes> attributesType() {
        return BasicFileAttributes.class;
    }

    @Override
    public BasicFileAttributes readAttributes(File file) {
        return new Attributes(file);
    }

    private static final class View
            extends AbstractAttributeView
            implements BasicFileAttributeView {
        protected View(FileLookup lookup) {
            super(lookup);
        }

        @Override
        public String name() {
            return "basic";
        }

        @Override
        public BasicFileAttributes readAttributes() throws IOException {
            return new Attributes(this.lookupFile());
        }

        @Override
        public void setTimes(@Nullable FileTime lastModifiedTime, @Nullable FileTime lastAccessTime, @Nullable FileTime createTime) throws IOException {
            File file = this.lookupFile();
            if (lastModifiedTime != null) {
                file.setLastModifiedTime(lastModifiedTime);
            }
            if (lastAccessTime != null) {
                file.setLastAccessTime(lastAccessTime);
            }
            if (createTime != null) {
                file.setCreationTime(createTime);
            }
        }
    }

    static class Attributes
            implements BasicFileAttributes {
        private final FileTime lastModifiedTime;
        private final FileTime lastAccessTime;
        private final FileTime creationTime;
        private final boolean regularFile;
        private final boolean directory;
        private final boolean symbolicLink;
        private final long size;
        private final Object fileKey;

        protected Attributes(File file) {
            this.lastModifiedTime = file.getLastModifiedTime();
            this.lastAccessTime = file.getLastAccessTime();
            this.creationTime = file.getCreationTime();
            this.regularFile = file.isRegularFile();
            this.directory = file.isDirectory();
            this.symbolicLink = file.isSymbolicLink();
            this.size = file.size();
            this.fileKey = file.id();
        }

        @Override
        public FileTime lastModifiedTime() {
            return this.lastModifiedTime;
        }

        @Override
        public FileTime lastAccessTime() {
            return this.lastAccessTime;
        }

        @Override
        public FileTime creationTime() {
            return this.creationTime;
        }

        @Override
        public boolean isRegularFile() {
            return this.regularFile;
        }

        @Override
        public boolean isDirectory() {
            return this.directory;
        }

        @Override
        public boolean isSymbolicLink() {
            return this.symbolicLink;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public long size() {
            return this.size;
        }

        @Override
        public Object fileKey() {
            return this.fileKey;
        }
    }
}
