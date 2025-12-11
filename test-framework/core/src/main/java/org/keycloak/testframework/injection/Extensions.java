package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.config.Config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class Extensions {

    private final RegistryLogger logger;
    private final ValueTypeAlias valueTypeAlias;
    private final List<Supplier<?, ?>> suppliers;
    private final List<Class<?>> alwaysEnabledValueTypes;

    private static Extensions INSTANCE;

    public static Extensions getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Extensions();
        }
        return INSTANCE;
    }

    public static void reset() {
        INSTANCE = null;
    }

    private Extensions() {
        List<TestFrameworkExtension> extensions = loadExtensions();
        valueTypeAlias = loadValueTypeAlias(extensions);
        Config.registerValueTypeAlias(valueTypeAlias);
        logger = new RegistryLogger(valueTypeAlias);
        suppliers = loadSuppliers(extensions);
        alwaysEnabledValueTypes = loadAlwaysEnabledValueTypes(extensions);
    }

    public ValueTypeAlias getValueTypeAlias() {
        return valueTypeAlias;
    }

    public List<Supplier<?, ?>> getSuppliers() {
        return suppliers;
    }

    public List<Class<?>> getAlwaysEnabledValueTypes() {
        return alwaysEnabledValueTypes;
    }

    @SuppressWarnings("unchecked")
    public <T> Supplier<T, ?> findSupplierByType(Class<T> typeClass) {
        return (Supplier<T, ?>) suppliers.stream().filter(s -> s.getValueType().equals(typeClass)).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T> Supplier<T, ?> findSupplierByAnnotation(Annotation annotation) {
        return (Supplier<T, ?>) suppliers.stream().filter(s -> s.getAnnotationClass().equals(annotation.annotationType())).findFirst().orElse(null);
    }

    private List<TestFrameworkExtension> loadExtensions() {
        List<TestFrameworkExtension> extensions = new LinkedList<>();
        ServiceLoader.load(TestFrameworkExtension.class).iterator().forEachRemaining(extensions::add);
        return extensions;
    }

    private ValueTypeAlias loadValueTypeAlias(List<TestFrameworkExtension> extensions) {
        ValueTypeAlias valueTypeAlias = new ValueTypeAlias();
        extensions.forEach(e -> valueTypeAlias.addAll(e.valueTypeAliases()));
        return valueTypeAlias;
    }

    private List<Supplier<?, ?>> loadSuppliers(List<TestFrameworkExtension> extensions) {
        List<Supplier<?, ?>> suppliers = new LinkedList<>();
        List<Supplier<?, ?>> skippedSuppliers = new LinkedList<>();
        Set<Class<?>> loadedValueTypes = new HashSet<>();

        for (TestFrameworkExtension extension : extensions) {
            for (var supplier : extension.suppliers()) {
                Class<?> valueType = supplier.getValueType();
                String requestedSupplier = Config.getSelectedSupplier(valueType);
                if (isSupplierIncluded(supplier) && (supplier.getAlias().equals(requestedSupplier) || (requestedSupplier == null && !loadedValueTypes.contains(valueType)))) {
                    configureSupplier(supplier);
                    suppliers.add(supplier);
                    loadedValueTypes.add(valueType);
                } else {
                    skippedSuppliers.add(supplier);
                }
            }
        }

        logger.logSuppliers(suppliers, skippedSuppliers);

        return suppliers;
    }

    private boolean isSupplierIncluded(Supplier<?, ?> supplier) {
        String includedSuppliers = Config.getIncludedSuppliers(supplier.getValueType());
        if (includedSuppliers != null) {
            if (Arrays.stream(includedSuppliers.split(",")).noneMatch(s -> s.equals(supplier.getAlias()))) {
                return false;
            }
        }

        String excludedSuppliers = Config.getExcludedSuppliers(supplier.getValueType());
        if (excludedSuppliers != null) {
            return Arrays.stream(excludedSuppliers.split(",")).noneMatch(s -> s.equals(supplier.getAlias()));
        }

        return true;
    }

    private List<Class<?>> loadAlwaysEnabledValueTypes(List<TestFrameworkExtension> extensions) {
        return extensions.stream().flatMap(s -> s.alwaysEnabledValueTypes().stream()).toList();
    }

    private void configureSupplier(Supplier<?, ?> supplier) {
        for (Field f : ReflectionUtils.listFields(supplier.getClass())) {
            ConfigProperty annotation = f.getAnnotation(ConfigProperty.class);
            if (annotation != null) {
                Object configValue = Config.getValueTypeConfig(supplier.getValueType(), annotation.name(), annotation.defaultValue(), f.getType());
                ReflectionUtils.setField(f, supplier, configValue);
            }
        }

    }

}
