package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.attribute.*;
import java.util.Map;

final class DosAttributeProvider
        extends AttributeProvider {
    private static final ImmutableSet<String> ATTRIBUTES = ImmutableSet.of("readonly", "hidden", "archive", "system");
    private static final ImmutableSet<String> INHERITED_VIEWS = ImmutableSet.of("basic", "owner");

    DosAttributeProvider() {
    }

    @Override
    public String name() {
        return "dos";
    }

    @Override
    public ImmutableSet<String> inherits() {
        return INHERITED_VIEWS;
    }

    @Override
    public ImmutableSet<String> fixedAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public ImmutableMap<String, ?> defaultValues(Map<String, ?> userProvidedDefaults) {
        return ImmutableMap.of("dos:readonly", DosAttributeProvider.getDefaultValue("dos:readonly", userProvidedDefaults), "dos:hidden", DosAttributeProvider.getDefaultValue("dos:hidden", userProvidedDefaults), "dos:archive", DosAttributeProvider.getDefaultValue("dos:archive", userProvidedDefaults), "dos:system", DosAttributeProvider.getDefaultValue("dos:system", userProvidedDefaults));
    }

    private static Boolean getDefaultValue(String attribute, Map<String, ?> userProvidedDefaults) {
        Object userProvidedValue = userProvidedDefaults.get(attribute);
        if (userProvidedValue != null) {
            return DosAttributeProvider.checkType("dos", attribute, userProvidedValue, Boolean.class);
        }
        return false;
    }

    @Override
    public @Nullable Object get(File file, String attribute) {
        if (ATTRIBUTES.contains(attribute)) {
            return file.getAttribute("dos", attribute);
        }
        return null;
    }

    @Override
    public void set(File file, String view, String attribute, Object value, boolean create) {
        if (this.supports(attribute)) {
            DosAttributeProvider.checkNotCreate(view, attribute, create);
            file.setAttribute("dos", attribute, DosAttributeProvider.checkType(view, attribute, value, Boolean.class));
        }
    }

    public Class<DosFileAttributeView> viewType() {
        return DosFileAttributeView.class;
    }

    @Override
    public DosFileAttributeView view(FileLookup lookup, ImmutableMap<String, FileAttributeView> inheritedViews) {
        return new View(lookup, (BasicFileAttributeView) inheritedViews.get("basic"));
    }

    public Class<DosFileAttributes> attributesType() {
        return DosFileAttributes.class;
    }

    @Override
    public DosFileAttributes readAttributes(File file) {
        return new Attributes(file);
    }

    private static final class View
            extends AbstractAttributeView
            implements DosFileAttributeView {
        private final BasicFileAttributeView basicView;

        public View(FileLookup lookup, BasicFileAttributeView basicView) {
            super(lookup);
            this.basicView = Preconditions.checkNotNull(basicView);
        }

        @Override
        public String name() {
            return "dos";
        }

        @Override
        public DosFileAttributes readAttributes() throws IOException {
            return new Attributes(this.lookupFile());
        }

        @Override
        public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
            this.basicView.setTimes(lastModifiedTime, lastAccessTime, createTime);
        }

        @Override
        public void setReadOnly(boolean value) throws IOException {
            this.lookupFile().setAttribute("dos", "readonly", value);
        }

        @Override
        public void setHidden(boolean value) throws IOException {
            this.lookupFile().setAttribute("dos", "hidden", value);
        }

        @Override
        public void setSystem(boolean value) throws IOException {
            this.lookupFile().setAttribute("dos", "system", value);
        }

        @Override
        public void setArchive(boolean value) throws IOException {
            this.lookupFile().setAttribute("dos", "archive", value);
        }
    }

    static class Attributes
            extends BasicAttributeProvider.Attributes
            implements DosFileAttributes {
        private final boolean readOnly;
        private final boolean hidden;
        private final boolean archive;
        private final boolean system;

        protected Attributes(File file) {
            super(file);
            this.readOnly = (Boolean) file.getAttribute("dos", "readonly");
            this.hidden = (Boolean) file.getAttribute("dos", "hidden");
            this.archive = (Boolean) file.getAttribute("dos", "archive");
            this.system = (Boolean) file.getAttribute("dos", "system");
        }

        @Override
        public boolean isReadOnly() {
            return this.readOnly;
        }

        @Override
        public boolean isHidden() {
            return this.hidden;
        }

        @Override
        public boolean isArchive() {
            return this.archive;
        }

        @Override
        public boolean isSystem() {
            return this.system;
        }
    }
}
