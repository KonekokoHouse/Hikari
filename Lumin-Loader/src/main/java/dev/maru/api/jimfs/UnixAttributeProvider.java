package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

final class UnixAttributeProvider
        extends AttributeProvider {
    private static final ImmutableSet<String> ATTRIBUTES = ImmutableSet.of("uid", "ino", "dev", "nlink", "rdev", "ctime", new String[]{"mode", "gid"});
    private static final ImmutableSet<String> INHERITED_VIEWS = ImmutableSet.of("basic", "owner", "posix");
    private final AtomicInteger uidGenerator = new AtomicInteger();
    private final ConcurrentMap<Object, Integer> idCache = new ConcurrentHashMap<Object, Integer>();

    UnixAttributeProvider() {
    }

    @Override
    public String name() {
        return "unix";
    }

    @Override
    public ImmutableSet<String> inherits() {
        return INHERITED_VIEWS;
    }

    @Override
    public ImmutableSet<String> fixedAttributes() {
        return ATTRIBUTES;
    }

    public Class<UnixFileAttributeView> viewType() {
        return UnixFileAttributeView.class;
    }

    @Override
    public UnixFileAttributeView view(FileLookup lookup, ImmutableMap<String, FileAttributeView> inheritedViews) {
        throw new UnsupportedOperationException();
    }

    private Integer getUniqueId(Object object) {
        Integer existing;
        Integer id = (Integer) this.idCache.get(object);
        if (id == null && (existing = this.idCache.putIfAbsent(object, id = Integer.valueOf(this.uidGenerator.incrementAndGet()))) != null) {
            return existing;
        }
        return id;
    }

    @Override
    public @Nullable Object get(File file, String attribute) {
        switch (attribute) {
            case "uid": {
                UserPrincipal user = (UserPrincipal) file.getAttribute("owner", "owner");
                return this.getUniqueId(user);
            }
            case "gid": {
                GroupPrincipal group = (GroupPrincipal) file.getAttribute("posix", "group");
                return this.getUniqueId(group);
            }
            case "mode": {
                Set permissions = (Set) file.getAttribute("posix", "permissions");
                return UnixAttributeProvider.toMode(permissions);
            }
            case "ctime": {
                return file.getCreationTime();
            }
            case "rdev": {
                return 0L;
            }
            case "dev": {
                return 1L;
            }
            case "ino": {
                return file.id();
            }
            case "nlink": {
                return file.links();
            }
        }
        return null;
    }

    @Override
    public void set(File file, String view, String attribute, Object value, boolean create) {
        throw UnixAttributeProvider.unsettable(view, attribute, create);
    }

    private static int toMode(Set<PosixFilePermission> permissions) {
        int result = 0;
        block11:
        for (PosixFilePermission permission : permissions) {
            Preconditions.checkNotNull(permission);
            switch (permission) {
                case OWNER_READ: {
                    result |= 0x100;
                    continue block11;
                }
                case OWNER_WRITE: {
                    result |= 0x80;
                    continue block11;
                }
                case OWNER_EXECUTE: {
                    result |= 0x40;
                    continue block11;
                }
                case GROUP_READ: {
                    result |= 0x20;
                    continue block11;
                }
                case GROUP_WRITE: {
                    result |= 0x10;
                    continue block11;
                }
                case GROUP_EXECUTE: {
                    result |= 8;
                    continue block11;
                }
                case OTHERS_READ: {
                    result |= 4;
                    continue block11;
                }
                case OTHERS_WRITE: {
                    result |= 2;
                    continue block11;
                }
                case OTHERS_EXECUTE: {
                    result |= 1;
                    continue block11;
                }
            }
            throw new AssertionError();
        }
        return result;
    }
}
