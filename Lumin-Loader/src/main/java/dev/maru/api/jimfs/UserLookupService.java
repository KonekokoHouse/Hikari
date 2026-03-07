package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;

final class UserLookupService
        extends UserPrincipalLookupService {
    private final boolean supportsGroups;

    public UserLookupService(boolean supportsGroups) {
        this.supportsGroups = supportsGroups;
    }

    @Override
    public UserPrincipal lookupPrincipalByName(String name) {
        return UserLookupService.createUserPrincipal(name);
    }

    @Override
    public GroupPrincipal lookupPrincipalByGroupName(String group) throws IOException {
        if (!this.supportsGroups) {
            throw new UserPrincipalNotFoundException(group);
        }
        return UserLookupService.createGroupPrincipal(group);
    }

    static UserPrincipal createUserPrincipal(String name) {
        return new JimfsUserPrincipal(name);
    }

    static GroupPrincipal createGroupPrincipal(String name) {
        return new JimfsGroupPrincipal(name);
    }

    static final class JimfsUserPrincipal
            extends NamedPrincipal {
        private JimfsUserPrincipal(String name) {
            super(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof JimfsUserPrincipal && this.getName().equals(((JimfsUserPrincipal) obj).getName());
        }
    }

    static final class JimfsGroupPrincipal
            extends NamedPrincipal
            implements GroupPrincipal {
        private JimfsGroupPrincipal(String name) {
            super(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof JimfsGroupPrincipal && ((JimfsGroupPrincipal) obj).name.equals(this.name);
        }
    }

    private static abstract class NamedPrincipal
            implements UserPrincipal {
        protected final String name;

        private NamedPrincipal(String name) {
            this.name = Preconditions.checkNotNull(name);
        }

        @Override
        public final String getName() {
            return this.name;
        }

        @Override
        public final int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public final String toString() {
            return this.name;
        }
    }
}
