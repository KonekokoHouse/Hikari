package dev.maru.api.jimfs;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.*;

final class AttributeService {
    private static final String ALL_ATTRIBUTES = "*";
    private final ImmutableMap<String, AttributeProvider> providersByName;
    private final ImmutableMap<Class<?>, AttributeProvider> providersByViewType;
    private final ImmutableMap<Class<?>, AttributeProvider> providersByAttributesType;
    private final ImmutableList<FileAttribute<?>> defaultValues;
    private static final Splitter ATTRIBUTE_SPLITTER = Splitter.on(',');

    public AttributeService(Configuration configuration) {
        this(AttributeService.getProviders(configuration), configuration.defaultAttributeValues);
    }

    public AttributeService(Iterable<? extends AttributeProvider> providers, Map<String, ?> userProvidedDefaults) {
        ImmutableMap.Builder<String, AttributeProvider> byViewNameBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Class<?>, AttributeProvider> byViewTypeBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Class<?>, AttributeProvider> byAttributesTypeBuilder = ImmutableMap.builder();
        ImmutableList.Builder defaultAttributesBuilder = ImmutableList.builder();
        for (AttributeProvider attributeProvider : providers) {
            byViewNameBuilder.put(attributeProvider.name(), attributeProvider);
            byViewTypeBuilder.put(attributeProvider.viewType(), attributeProvider);
            if (attributeProvider.attributesType() != null) {
                byAttributesTypeBuilder.put(attributeProvider.attributesType(), attributeProvider);
            }
            for (Map.Entry entry : attributeProvider.defaultValues(userProvidedDefaults).entrySet()) {
                defaultAttributesBuilder.add(new SimpleFileAttribute((String) entry.getKey(), entry.getValue()));
            }
        }
        this.providersByName = byViewNameBuilder.build();
        this.providersByViewType = byViewTypeBuilder.build();
        this.providersByAttributesType = byAttributesTypeBuilder.build();
        this.defaultValues = defaultAttributesBuilder.build();
    }

    private static Iterable<AttributeProvider> getProviders(Configuration configuration) {
        HashMap<String, AttributeProvider> result = new HashMap<String, AttributeProvider>();
        for (AttributeProvider provider : configuration.attributeProviders) {
            result.put(provider.name(), provider);
        }
        for (String view : configuration.attributeViews) {
            AttributeService.addStandardProvider(result, view);
        }
        AttributeService.addMissingProviders(result);
        return Collections.unmodifiableCollection(result.values());
    }

    private static void addMissingProviders(Map<String, AttributeProvider> providers) {
        HashSet<String> missingViews = new HashSet<String>();
        for (AttributeProvider provider : providers.values()) {
            for (String inheritedView : provider.inherits()) {
                if (providers.containsKey(inheritedView)) continue;
                missingViews.add(inheritedView);
            }
        }
        if (missingViews.isEmpty()) {
            return;
        }
        for (String view : missingViews) {
            AttributeService.addStandardProvider(providers, view);
        }
        AttributeService.addMissingProviders(providers);
    }

    private static void addStandardProvider(Map<String, AttributeProvider> result, String view) {
        AttributeProvider provider = StandardAttributeProviders.get(view);
        if (provider == null) {
            if (!result.containsKey(view)) {
                throw new IllegalStateException("no provider found for attribute view '" + view + "'");
            }
        } else {
            result.put(provider.name(), provider);
        }
    }

    public ImmutableSet<String> supportedFileAttributeViews() {
        return this.providersByName.keySet();
    }

    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return this.providersByViewType.containsKey(type);
    }

    public void setInitialAttributes(File file, FileAttribute<?>... attrs) {
        for (int i = 0; i < this.defaultValues.size(); ++i) {
            FileAttribute attribute = (FileAttribute) this.defaultValues.get(i);
            int separatorIndex = attribute.name().indexOf(58);
            String view = attribute.name().substring(0, separatorIndex);
            String attr = attribute.name().substring(separatorIndex + 1);
            file.setAttribute(view, attr, attribute.value());
        }
        for (FileAttribute<?> attr : attrs) {
            this.setAttribute(file, attr.name(), attr.value(), true);
        }
    }

    public void copyAttributes(File file, File copy, AttributeCopyOption copyOption) {
        switch (copyOption) {
            case ALL: {
                file.copyAttributes(copy);
                break;
            }
            case BASIC: {
                file.copyBasicAttributes(copy);
                break;
            }
        }
    }

    public Object getAttribute(File file, String attribute) {
        String view = AttributeService.getViewName(attribute);
        String attr = AttributeService.getSingleAttribute(attribute);
        return this.getAttribute(file, view, attr);
    }

    public Object getAttribute(File file, String view, String attribute) {
        Object value = this.getAttributeInternal(file, view, attribute);
        if (value == null) {
            throw new IllegalArgumentException("invalid attribute for view '" + view + "': " + attribute);
        }
        return value;
    }

    private @Nullable Object getAttributeInternal(File file, String view, String attribute) {
        Object value;
        block2:
        {
            String inheritedView;
            AttributeProvider provider = this.providersByName.get(view);
            if (provider == null) {
                return null;
            }
            value = provider.get(file, attribute);
            if (value != null) break block2;
            Iterator iterator = provider.inherits().iterator();
            while (iterator.hasNext() && (value = this.getAttributeInternal(file, inheritedView = (String) iterator.next(), attribute)) == null) {
            }
        }
        return value;
    }

    public void setAttribute(File file, String attribute, Object value, boolean create) {
        String view = AttributeService.getViewName(attribute);
        String attr = AttributeService.getSingleAttribute(attribute);
        this.setAttributeInternal(file, view, attr, value, create);
    }

    private void setAttributeInternal(File file, String view, String attribute, Object value, boolean create) {
        AttributeProvider provider = this.providersByName.get(view);
        if (provider != null) {
            if (provider.supports(attribute)) {
                provider.set(file, view, attribute, value, create);
                return;
            }
            for (String inheritedView : provider.inherits()) {
                AttributeProvider inheritedProvider = this.providersByName.get(inheritedView);
                if (!inheritedProvider.supports(attribute)) continue;
                inheritedProvider.set(file, view, attribute, value, create);
                return;
            }
        }
        throw new UnsupportedOperationException("cannot set attribute '" + view + ":" + attribute + "'");
    }

    public <V extends FileAttributeView> @Nullable V getFileAttributeView(FileLookup lookup, Class<V> type) {
        AttributeProvider provider = this.providersByViewType.get(type);
        if (provider != null) {
            return (V) provider.view(lookup, this.createInheritedViews(lookup, provider));
        }
        return null;
    }

    private FileAttributeView getFileAttributeView(FileLookup lookup, Class<? extends FileAttributeView> viewType, Map<String, FileAttributeView> inheritedViews) {
        AttributeProvider provider = this.providersByViewType.get(viewType);
        this.createInheritedViews(lookup, provider, inheritedViews);
        return provider.view(lookup, ImmutableMap.copyOf(inheritedViews));
    }

    private ImmutableMap<String, FileAttributeView> createInheritedViews(FileLookup lookup, AttributeProvider provider) {
        if (provider.inherits().isEmpty()) {
            return ImmutableMap.of();
        }
        HashMap<String, FileAttributeView> inheritedViews = new HashMap<String, FileAttributeView>();
        this.createInheritedViews(lookup, provider, inheritedViews);
        return ImmutableMap.copyOf(inheritedViews);
    }

    private void createInheritedViews(FileLookup lookup, AttributeProvider provider, Map<String, FileAttributeView> inheritedViews) {
        for (String inherited : provider.inherits()) {
            if (inheritedViews.containsKey(inherited)) continue;
            AttributeProvider inheritedProvider = this.providersByName.get(inherited);
            FileAttributeView inheritedView = this.getFileAttributeView(lookup, inheritedProvider.viewType(), inheritedViews);
            inheritedViews.put(inherited, inheritedView);
        }
    }

    public ImmutableMap<String, Object> readAttributes(File file, String attributes) {
        String view = AttributeService.getViewName(attributes);
        ImmutableList<String> attrs = AttributeService.getAttributeNames(attributes);
        if (attrs.size() > 1 && attrs.contains(ALL_ATTRIBUTES)) {
            throw new IllegalArgumentException("invalid attributes: " + attributes);
        }
        HashMap<String, Object> result = new HashMap<String, Object>();
        if (attrs.size() == 1 && attrs.contains(ALL_ATTRIBUTES)) {
            AttributeProvider provider = this.providersByName.get(view);
            AttributeService.readAll(file, provider, result);
            for (String inheritedView : provider.inherits()) {
                AttributeProvider inheritedProvider = this.providersByName.get(inheritedView);
                AttributeService.readAll(file, inheritedProvider, result);
            }
        } else {
            for (String attr : attrs) {
                result.put(attr, this.getAttribute(file, view, attr));
            }
        }
        return ImmutableMap.copyOf(result);
    }

    public <A extends BasicFileAttributes> A readAttributes(File file, Class<A> type) {
        AttributeProvider provider = this.providersByAttributesType.get(type);
        if (provider != null) {
            return (A) provider.readAttributes(file);
        }
        throw new UnsupportedOperationException("unsupported attributes type: " + type);
    }

    private static void readAll(File file, AttributeProvider provider, Map<String, Object> map) {
        for (String attribute : provider.attributes(file)) {
            Object value = provider.get(file, attribute);
            if (value == null) continue;
            map.put(attribute, value);
        }
    }

    private static String getViewName(String attribute) {
        int separatorIndex = attribute.indexOf(58);
        if (separatorIndex == -1) {
            return "basic";
        }
        if (separatorIndex == 0 || separatorIndex == attribute.length() - 1 || attribute.indexOf(58, separatorIndex + 1) != -1) {
            throw new IllegalArgumentException("illegal attribute format: " + attribute);
        }
        return attribute.substring(0, separatorIndex);
    }

    private static ImmutableList<String> getAttributeNames(String attributes) {
        int separatorIndex = attributes.indexOf(58);
        String attributesPart = attributes.substring(separatorIndex + 1);
        return ImmutableList.copyOf(ATTRIBUTE_SPLITTER.split(attributesPart));
    }

    private static String getSingleAttribute(String attribute) {
        ImmutableList<String> attributeNames = AttributeService.getAttributeNames(attribute);
        if (attributeNames.size() != 1 || ALL_ATTRIBUTES.equals(attributeNames.get(0))) {
            throw new IllegalArgumentException("must specify a single attribute: " + attribute);
        }
        return (String) attributeNames.get(0);
    }

    private static final class SimpleFileAttribute<T>
            implements FileAttribute<T> {
        private final String name;
        private final T value;

        SimpleFileAttribute(String name, T value) {
            this.name = Preconditions.checkNotNull(name);
            this.value = Preconditions.checkNotNull(value);
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public T value() {
            return this.value;
        }
    }
}
