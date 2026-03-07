package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.attribute.*;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class PosixAttributeProvider
        extends AttributeProvider {
    private static final ImmutableSet<String> ATTRIBUTES = ImmutableSet.of("group", "permissions");
    private static final ImmutableSet<String> INHERITED_VIEWS = ImmutableSet.of("basic", "owner");
    private static final GroupPrincipal DEFAULT_GROUP = UserLookupService.createGroupPrincipal("group");
    private static final ImmutableSet<PosixFilePermission> DEFAULT_PERMISSIONS = Sets.immutableEnumSet(PosixFilePermissions.fromString("rw-r--r--"));

    PosixAttributeProvider() {
    }

    @Override
    public String name() {
        return "posix";
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
        Object userProvidedGroup = userProvidedDefaults.get("posix:group");
        GroupPrincipal group = DEFAULT_GROUP;
        if (userProvidedGroup != null) {
            if (userProvidedGroup instanceof String) {
                group = UserLookupService.createGroupPrincipal((String) userProvidedGroup);
            } else {
                throw new IllegalArgumentException("invalid type " + userProvidedGroup.getClass().getName() + " for attribute 'posix:group': should be one of " + String.class + " or " + GroupPrincipal.class);
            }
        }
        Object userProvidedPermissions = userProvidedDefaults.get("posix:permissions");
        ImmutableSet<PosixFilePermission> permissions = DEFAULT_PERMISSIONS;
        if (userProvidedPermissions != null) {
            if (userProvidedPermissions instanceof String) {
                permissions = Sets.immutableEnumSet(PosixFilePermissions.fromString((String) userProvidedPermissions));
            } else if (userProvidedPermissions instanceof Set) {
                permissions = PosixAttributeProvider.toPermissions((Set) userProvidedPermissions);
            } else {
                throw new IllegalArgumentException("invalid type " + userProvidedPermissions.getClass().getName() + " for attribute 'posix:permissions': should be one of " + String.class + " or " + Set.class);
            }
        }
        return ImmutableMap.of("posix:group", group, "posix:permissions", permissions);
    }

    @Override
    public @Nullable Object get(File file, String attribute) {
        switch (attribute) {
            case "group": {
                return file.getAttribute("posix", "group");
            }
            case "permissions": {
                return file.getAttribute("posix", "permissions");
            }
        }
        return null;
    }

    @Override
    public void set(File file, String view, String attribute, Object value, boolean create) {
        switch (attribute) {
            case "group": {
                PosixAttributeProvider.checkNotCreate(view, attribute, create);
                GroupPrincipal group = PosixAttributeProvider.checkType(view, attribute, value, GroupPrincipal.class);
                if (!(group instanceof UserLookupService.JimfsGroupPrincipal)) {
                    group = UserLookupService.createGroupPrincipal(group.getName());
                }
                file.setAttribute("posix", "group", group);
                break;
            }
            case "permissions": {
                file.setAttribute("posix", "permissions", PosixAttributeProvider.toPermissions(PosixAttributeProvider.checkType(view, attribute, value, Set.class)));
                break;
            }
        }
    }

    private static ImmutableSet<PosixFilePermission> toPermissions(Set<?> set) {
        ImmutableSet copy = ImmutableSet.copyOf(set);
        for (Object obj : copy) {
            if (obj instanceof PosixFilePermission) continue;
            throw new IllegalArgumentException("invalid element for attribute 'posix:permissions': should be Set<PosixFilePermission>, found element of type " + obj.getClass());
        }
        return Sets.immutableEnumSet(copy);
    }

    public Class<PosixFileAttributeView> viewType() {
        return PosixFileAttributeView.class;
    }

    @Override
    public PosixFileAttributeView view(FileLookup lookup, ImmutableMap<String, FileAttributeView> inheritedViews) {
        return new View(lookup, (BasicFileAttributeView) inheritedViews.get("basic"), (FileOwnerAttributeView) inheritedViews.get("owner"));
    }

    public Class<PosixFileAttributes> attributesType() {
        return PosixFileAttributes.class;
    }

    @Override
    public PosixFileAttributes readAttributes(File file) {
        return new Attributes(file);
    }

    private static class View
            extends AbstractAttributeView
            implements PosixFileAttributeView {
        private final BasicFileAttributeView basicView;
        private final FileOwnerAttributeView ownerView;

        protected View(FileLookup lookup, BasicFileAttributeView basicView, FileOwnerAttributeView ownerView) {
            super(lookup);
            this.basicView = Preconditions.checkNotNull(basicView);
            this.ownerView = Preconditions.checkNotNull(ownerView);
        }

        @Override
        public String name() {
            return "posix";
        }

        @Override
        public PosixFileAttributes readAttributes() throws IOException {
            return new Attributes(this.lookupFile());
        }

        @Override
        public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
            this.basicView.setTimes(lastModifiedTime, lastAccessTime, createTime);
        }

        @Override
        public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
            this.lookupFile().setAttribute("posix", "permissions", ImmutableSet.copyOf(perms));
        }

        @Override
        public void setGroup(GroupPrincipal group) throws IOException {
            this.lookupFile().setAttribute("posix", "group", Preconditions.checkNotNull(group));
        }

        @Override
        public UserPrincipal getOwner() throws IOException {
            return this.ownerView.getOwner();
        }

        @Override
        public void setOwner(UserPrincipal owner) throws IOException {
            this.ownerView.setOwner(owner);
        }
    }

    static class Attributes
            extends BasicAttributeProvider.Attributes
            implements PosixFileAttributes {
        private final UserPrincipal owner;
        private final GroupPrincipal group;
        private final ImmutableSet<PosixFilePermission> permissions;

        protected Attributes(File file) {
            super(file);
            this.owner = (UserPrincipal) file.getAttribute("owner", "owner");
            this.group = (GroupPrincipal) file.getAttribute("posix", "group");
            this.permissions = (ImmutableSet) file.getAttribute("posix", "permissions");
        }

        @Override
        public UserPrincipal owner() {
            return this.owner;
        }

        @Override
        public GroupPrincipal group() {
            return this.group;
        }

        @Override
        public Set<PosixFilePermission> permissions() {
            return new LinkedHashSet<PosixFilePermission>(this.permissions);
        }
    }
}
