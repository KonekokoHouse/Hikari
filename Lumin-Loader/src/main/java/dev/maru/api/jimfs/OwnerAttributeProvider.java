package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.Map;

final class OwnerAttributeProvider
        extends AttributeProvider {
    private static final ImmutableSet<String> ATTRIBUTES = ImmutableSet.of("owner");
    private static final UserPrincipal DEFAULT_OWNER = UserLookupService.createUserPrincipal("user");

    OwnerAttributeProvider() {
    }

    @Override
    public String name() {
        return "owner";
    }

    @Override
    public ImmutableSet<String> fixedAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public ImmutableMap<String, ?> defaultValues(Map<String, ?> userProvidedDefaults) {
        Object userProvidedOwner = userProvidedDefaults.get("owner:owner");
        UserPrincipal owner = DEFAULT_OWNER;
        if (userProvidedOwner != null) {
            if (userProvidedOwner instanceof String) {
                owner = UserLookupService.createUserPrincipal((String) userProvidedOwner);
            } else {
                throw OwnerAttributeProvider.invalidType("owner", "owner", userProvidedOwner, String.class, UserPrincipal.class);
            }
        }
        return ImmutableMap.of("owner:owner", owner);
    }

    @Override
    public @Nullable Object get(File file, String attribute) {
        if (attribute.equals("owner")) {
            return file.getAttribute("owner", "owner");
        }
        return null;
    }

    @Override
    public void set(File file, String view, String attribute, Object value, boolean create) {
        if (attribute.equals("owner")) {
            OwnerAttributeProvider.checkNotCreate(view, attribute, create);
            UserPrincipal user = OwnerAttributeProvider.checkType(view, attribute, value, UserPrincipal.class);
            if (!(user instanceof UserLookupService.JimfsUserPrincipal)) {
                user = UserLookupService.createUserPrincipal(user.getName());
            }
            file.setAttribute("owner", "owner", user);
        }
    }

    public Class<FileOwnerAttributeView> viewType() {
        return FileOwnerAttributeView.class;
    }

    @Override
    public FileOwnerAttributeView view(FileLookup lookup, ImmutableMap<String, FileAttributeView> inheritedViews) {
        return new View(lookup);
    }

    private static final class View
            extends AbstractAttributeView
            implements FileOwnerAttributeView {
        public View(FileLookup lookup) {
            super(lookup);
        }

        @Override
        public String name() {
            return "owner";
        }

        @Override
        public UserPrincipal getOwner() throws IOException {
            return (UserPrincipal) this.lookupFile().getAttribute("owner", "owner");
        }

        @Override
        public void setOwner(UserPrincipal owner) throws IOException {
            this.lookupFile().setAttribute("owner", "owner", Preconditions.checkNotNull(owner));
        }
    }
}
