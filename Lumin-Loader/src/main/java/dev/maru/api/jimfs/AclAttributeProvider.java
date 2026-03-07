package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.attribute.*;
import java.util.List;
import java.util.Map;

final class AclAttributeProvider
        extends AttributeProvider {
    private static final ImmutableSet<String> ATTRIBUTES = ImmutableSet.of("acl");
    private static final ImmutableSet<String> INHERITED_VIEWS = ImmutableSet.of("owner");
    private static final ImmutableList<AclEntry> DEFAULT_ACL = ImmutableList.of();

    AclAttributeProvider() {
    }

    @Override
    public String name() {
        return "acl";
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
        Object userProvidedAcl = userProvidedDefaults.get("acl:acl");
        ImmutableList<AclEntry> acl = DEFAULT_ACL;
        if (userProvidedAcl != null) {
            acl = AclAttributeProvider.toAcl(AclAttributeProvider.checkType("acl", "acl", userProvidedAcl, List.class));
        }
        return ImmutableMap.of("acl:acl", acl);
    }

    @Override
    public @Nullable Object get(File file, String attribute) {
        if (attribute.equals("acl")) {
            return file.getAttribute("acl", "acl");
        }
        return null;
    }

    @Override
    public void set(File file, String view, String attribute, Object value, boolean create) {
        if (attribute.equals("acl")) {
            AclAttributeProvider.checkNotCreate(view, attribute, create);
            file.setAttribute("acl", "acl", AclAttributeProvider.toAcl(AclAttributeProvider.checkType(view, attribute, value, List.class)));
        }
    }

    private static ImmutableList<AclEntry> toAcl(List<?> list) {
        ImmutableList.Builder<AclEntry> builder = ImmutableList.builder();
        for (Object e : list) {
            if (!(e instanceof AclEntry)) {
                throw new IllegalArgumentException("invalid element for attribute 'acl:acl': should be List<AclEntry>, found element of type " + e.getClass());
            }
            builder.add((AclEntry) e);
        }
        return builder.build();
    }

    public Class<AclFileAttributeView> viewType() {
        return AclFileAttributeView.class;
    }

    @Override
    public AclFileAttributeView view(FileLookup lookup, ImmutableMap<String, FileAttributeView> inheritedViews) {
        return new View(lookup, (FileOwnerAttributeView) inheritedViews.get("owner"));
    }

    private static final class View
            extends AbstractAttributeView
            implements AclFileAttributeView {
        private final FileOwnerAttributeView ownerView;

        public View(FileLookup lookup, FileOwnerAttributeView ownerView) {
            super(lookup);
            this.ownerView = Preconditions.checkNotNull(ownerView);
        }

        @Override
        public String name() {
            return "acl";
        }

        @Override
        public List<AclEntry> getAcl() throws IOException {
            return (List) this.lookupFile().getAttribute("acl", "acl");
        }

        @Override
        public void setAcl(List<AclEntry> acl) throws IOException {
            Preconditions.checkNotNull(acl);
            this.lookupFile().setAttribute("acl", "acl", ImmutableList.copyOf(acl));
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
}
