package dev.maru.api.jimfs;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public final class Configuration {
    final PathType pathType;
    final ImmutableSet<PathNormalization> nameDisplayNormalization;
    final ImmutableSet<PathNormalization> nameCanonicalNormalization;
    final boolean pathEqualityUsesCanonicalForm;
    final int blockSize;
    final long maxSize;
    final long maxCacheSize;
    final ImmutableSet<String> attributeViews;
    final ImmutableSet<AttributeProvider> attributeProviders;
    final ImmutableMap<String, Object> defaultAttributeValues;
    final FileTimeSource fileTimeSource;
    final WatchServiceConfiguration watchServiceConfig;
    final ImmutableSet<String> roots;
    final String workingDirectory;
    final ImmutableSet<Feature> supportedFeatures;
    private final String displayName;

    public static Configuration unix() {
        return UnixHolder.UNIX;
    }

    public static Configuration osX() {
        return OsxHolder.OS_X;
    }

    public static Configuration windows() {
        return WindowsHolder.WINDOWS;
    }

    public static Configuration forCurrentPlatform() {
        String os = System.getProperty("os.name");
        if (os.contains("Windows")) {
            return Configuration.windows();
        }
        if (os.contains("OS X")) {
            return Configuration.osX();
        }
        return Configuration.unix();
    }

    public static Builder builder(PathType pathType) {
        return new Builder(pathType);
    }

    private Configuration(Builder builder) {
        this.pathType = builder.pathType;
        this.nameDisplayNormalization = builder.nameDisplayNormalization;
        this.nameCanonicalNormalization = builder.nameCanonicalNormalization;
        this.pathEqualityUsesCanonicalForm = builder.pathEqualityUsesCanonicalForm;
        this.blockSize = builder.blockSize;
        this.maxSize = builder.maxSize;
        this.maxCacheSize = builder.maxCacheSize;
        this.attributeViews = builder.attributeViews;
        this.attributeProviders = builder.attributeProviders == null ? ImmutableSet.of() : ImmutableSet.copyOf(builder.attributeProviders);
        this.defaultAttributeValues = builder.defaultAttributeValues == null ? ImmutableMap.of() : ImmutableMap.copyOf(builder.defaultAttributeValues);
        this.fileTimeSource = builder.fileTimeSource;
        this.watchServiceConfig = builder.watchServiceConfig;
        this.roots = builder.roots;
        this.workingDirectory = builder.workingDirectory;
        this.supportedFeatures = builder.supportedFeatures;
        this.displayName = builder.displayName;
    }

    public String toString() {
        if (this.displayName != null) {
            return MoreObjects.toStringHelper(this).addValue(this.displayName).toString();
        }
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this).add("pathType", this.pathType).add("roots", this.roots).add("supportedFeatures", this.supportedFeatures).add("workingDirectory", this.workingDirectory);
        if (!this.nameDisplayNormalization.isEmpty()) {
            helper.add("nameDisplayNormalization", this.nameDisplayNormalization);
        }
        if (!this.nameCanonicalNormalization.isEmpty()) {
            helper.add("nameCanonicalNormalization", this.nameCanonicalNormalization);
        }
        helper.add("pathEqualityUsesCanonicalForm", this.pathEqualityUsesCanonicalForm).add("blockSize", this.blockSize).add("maxSize", this.maxSize);
        if (this.maxCacheSize != -1L) {
            helper.add("maxCacheSize", this.maxCacheSize);
        }
        if (!this.attributeViews.isEmpty()) {
            helper.add("attributeViews", this.attributeViews);
        }
        if (!this.attributeProviders.isEmpty()) {
            helper.add("attributeProviders", this.attributeProviders);
        }
        if (!this.defaultAttributeValues.isEmpty()) {
            helper.add("defaultAttributeValues", this.defaultAttributeValues);
        }
        helper.add("fileTimeSource", this.fileTimeSource);
        if (this.watchServiceConfig != WatchServiceConfiguration.DEFAULT) {
            helper.add("watchServiceConfig", this.watchServiceConfig);
        }
        return helper.toString();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        public static final int DEFAULT_BLOCK_SIZE = 8192;
        public static final long DEFAULT_MAX_SIZE = 0x100000000L;
        public static final long DEFAULT_MAX_CACHE_SIZE = -1L;
        private final PathType pathType;
        private ImmutableSet<PathNormalization> nameDisplayNormalization = ImmutableSet.of();
        private ImmutableSet<PathNormalization> nameCanonicalNormalization = ImmutableSet.of();
        private boolean pathEqualityUsesCanonicalForm = false;
        private int blockSize = 8192;
        private long maxSize = 0x100000000L;
        private long maxCacheSize = -1L;
        private ImmutableSet<String> attributeViews = ImmutableSet.of();
        private Set<AttributeProvider> attributeProviders = null;
        private Map<String, Object> defaultAttributeValues;
        private FileTimeSource fileTimeSource = SystemFileTimeSource.INSTANCE;
        private WatchServiceConfiguration watchServiceConfig = WatchServiceConfiguration.DEFAULT;
        private ImmutableSet<String> roots = ImmutableSet.of();
        private String workingDirectory;
        private ImmutableSet<Feature> supportedFeatures = ImmutableSet.of();
        private String displayName;
        private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("[^:]+:[^:]+");

        private Builder(PathType pathType) {
            this.pathType = Preconditions.checkNotNull(pathType);
        }

        private Builder(Configuration configuration) {
            this.pathType = configuration.pathType;
            this.nameDisplayNormalization = configuration.nameDisplayNormalization;
            this.nameCanonicalNormalization = configuration.nameCanonicalNormalization;
            this.pathEqualityUsesCanonicalForm = configuration.pathEqualityUsesCanonicalForm;
            this.blockSize = configuration.blockSize;
            this.maxSize = configuration.maxSize;
            this.maxCacheSize = configuration.maxCacheSize;
            this.attributeViews = configuration.attributeViews;
            this.attributeProviders = configuration.attributeProviders.isEmpty() ? null : new HashSet<AttributeProvider>(configuration.attributeProviders);
            this.defaultAttributeValues = configuration.defaultAttributeValues.isEmpty() ? null : new HashMap<String, Object>(configuration.defaultAttributeValues);
            this.fileTimeSource = configuration.fileTimeSource;
            this.watchServiceConfig = configuration.watchServiceConfig;
            this.roots = configuration.roots;
            this.workingDirectory = configuration.workingDirectory;
            this.supportedFeatures = configuration.supportedFeatures;
        }

        @CanIgnoreReturnValue
        public Builder setNameDisplayNormalization(PathNormalization first, PathNormalization... more) {
            this.nameDisplayNormalization = this.checkNormalizations(Lists.asList(first, more));
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setNameCanonicalNormalization(PathNormalization first, PathNormalization... more) {
            this.nameCanonicalNormalization = this.checkNormalizations(Lists.asList(first, more));
            return this;
        }

        private ImmutableSet<PathNormalization> checkNormalizations(List<PathNormalization> normalizations) {
            PathNormalization none = null;
            PathNormalization normalization = null;
            PathNormalization caseFold = null;
            block5:
            for (PathNormalization n : normalizations) {
                Preconditions.checkNotNull(n);
                Builder.checkNormalizationNotSet(n, none);
                switch (n) {
                    case NONE: {
                        none = n;
                        continue block5;
                    }
                    case NFC:
                    case NFD: {
                        Builder.checkNormalizationNotSet(n, normalization);
                        normalization = n;
                        continue block5;
                    }
                    case CASE_FOLD_UNICODE:
                    case CASE_FOLD_ASCII: {
                        Builder.checkNormalizationNotSet(n, caseFold);
                        caseFold = n;
                        continue block5;
                    }
                }
                throw new AssertionError();
            }
            if (none != null) {
                return ImmutableSet.of();
            }
            return Sets.immutableEnumSet(normalizations);
        }

        private static void checkNormalizationNotSet(PathNormalization n, @Nullable PathNormalization set) {
            if (set != null) {
                throw new IllegalArgumentException("can't set normalization " + n + ": normalization " + set + " already set");
            }
        }

        @CanIgnoreReturnValue
        public Builder setPathEqualityUsesCanonicalForm(boolean useCanonicalForm) {
            this.pathEqualityUsesCanonicalForm = useCanonicalForm;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setBlockSize(int blockSize) {
            Preconditions.checkArgument(blockSize > 0, "blockSize (%s) must be positive", blockSize);
            this.blockSize = blockSize;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setMaxSize(long maxSize) {
            Preconditions.checkArgument(maxSize > 0L, "maxSize (%s) must be positive", maxSize);
            this.maxSize = maxSize;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setMaxCacheSize(long maxCacheSize) {
            Preconditions.checkArgument(maxCacheSize >= 0L, "maxCacheSize (%s) may not be negative", maxCacheSize);
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setAttributeViews(String first, String... more) {
            this.attributeViews = ImmutableSet.copyOf(Lists.asList(first, more));
            return this;
        }

        @CanIgnoreReturnValue
        public Builder addAttributeProvider(AttributeProvider provider) {
            Preconditions.checkNotNull(provider);
            if (this.attributeProviders == null) {
                this.attributeProviders = new HashSet<AttributeProvider>();
            }
            this.attributeProviders.add(provider);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setDefaultAttributeValue(String attribute, Object value) {
            Preconditions.checkArgument(ATTRIBUTE_PATTERN.matcher(attribute).matches(), "attribute (%s) must be of the form \"view:attribute\"", (Object) attribute);
            Preconditions.checkNotNull(value);
            if (this.defaultAttributeValues == null) {
                this.defaultAttributeValues = new HashMap<String, Object>();
            }
            this.defaultAttributeValues.put(attribute, value);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setFileTimeSource(FileTimeSource source) {
            this.fileTimeSource = Preconditions.checkNotNull(source);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setRoots(String first, String... more) {
            List<String> roots = Lists.asList(first, more);
            for (String root : roots) {
                PathType.ParseResult parseResult = this.pathType.parsePath(root);
                Preconditions.checkArgument(parseResult.isRoot(), "invalid root: %s", (Object) root);
            }
            this.roots = ImmutableSet.copyOf(roots);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setWorkingDirectory(String workingDirectory) {
            PathType.ParseResult parseResult = this.pathType.parsePath(workingDirectory);
            Preconditions.checkArgument(parseResult.isAbsolute(), "working directory must be an absolute path: %s", (Object) workingDirectory);
            this.workingDirectory = Preconditions.checkNotNull(workingDirectory);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setSupportedFeatures(Feature... features) {
            this.supportedFeatures = Sets.immutableEnumSet(Arrays.asList(features));
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setWatchServiceConfiguration(WatchServiceConfiguration config) {
            this.watchServiceConfig = Preconditions.checkNotNull(config);
            return this;
        }

        @CanIgnoreReturnValue
        private Builder setDisplayName(String displayName) {
            this.displayName = Preconditions.checkNotNull(displayName);
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }

        static /* synthetic */ Builder access$100(Builder x0, String x1) {
            return x0.setDisplayName(x1);
        }
    }

    private static final class UnixHolder {
        private static final Configuration UNIX = Builder.access$100(Configuration.builder(PathType.unix()), "Unix").setRoots("/", new String[0]).setWorkingDirectory("/work").setAttributeViews("basic", new String[0]).setSupportedFeatures(Feature.LINKS, Feature.SYMBOLIC_LINKS, Feature.SECURE_DIRECTORY_STREAM, Feature.FILE_CHANNEL).build();

        private UnixHolder() {
        }
    }

    private static final class OsxHolder {
        private static final Configuration OS_X = Builder.access$100(Configuration.unix().toBuilder(), "OSX").setNameDisplayNormalization(PathNormalization.NFC, new PathNormalization[0]).setNameCanonicalNormalization(PathNormalization.NFD, PathNormalization.CASE_FOLD_ASCII).setSupportedFeatures(Feature.LINKS, Feature.SYMBOLIC_LINKS, Feature.FILE_CHANNEL).build();

        private OsxHolder() {
        }
    }

    private static final class WindowsHolder {
        private static final Configuration WINDOWS = Builder.access$100(Configuration.builder(PathType.windows()), "Windows").setRoots("C:\\", new String[0]).setWorkingDirectory("C:\\work").setNameCanonicalNormalization(PathNormalization.CASE_FOLD_ASCII, new PathNormalization[0]).setPathEqualityUsesCanonicalForm(true).setAttributeViews("basic", new String[0]).setSupportedFeatures(Feature.LINKS, Feature.SYMBOLIC_LINKS, Feature.FILE_CHANNEL).build();

        private WindowsHolder() {
        }
    }
}
