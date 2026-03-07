package dev.maru.api.jimfs;

import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.Nullable;

final class StandardAttributeProviders {
    private static final ImmutableMap<String, AttributeProvider> PROVIDERS = new ImmutableMap.Builder<String, AttributeProvider>().put("basic", new BasicAttributeProvider()).put("owner", new OwnerAttributeProvider()).put("posix", new PosixAttributeProvider()).put("dos", new DosAttributeProvider()).put("acl", new AclAttributeProvider()).put("user", new UserDefinedAttributeProvider()).build();

    private StandardAttributeProviders() {
    }

    public static @Nullable AttributeProvider get(String view) {
        AttributeProvider provider = PROVIDERS.get(view);
        if (provider == null && view.equals("unix")) {
            return new UnixAttributeProvider();
        }
        return provider;
    }
}
