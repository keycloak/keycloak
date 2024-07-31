package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstanceContext<T, A extends Annotation> {

    private final Registry registry;
    private final Supplier<T, A> supplier;
    private final A annotation;
    private final Set<InstanceContext<T, A>> dependencies = new HashSet<>();
    private T value;
    private Class<? extends T> requestedValueType;
    private final Class<?> config;
    private LifeCycle lifeCycle;
    private final String ref;
    private final String realmRef;
    private final Map<String, Object> notes = new HashMap<>();

    public InstanceContext(Registry registry, Supplier<T, A> supplier, A annotation, Class<? extends T> requestedValueType) {
        this.registry = registry;
        this.supplier = supplier;
        this.annotation = annotation;
        this.requestedValueType = requestedValueType;
        this.config = (Class<?>) supplier.getAnnotationElementValue(annotation, SupplierHelpers.CONFIG);
        this.lifeCycle = (LifeCycle) supplier.getAnnotationElementValue(annotation, SupplierHelpers.LIFECYCLE);
        this.ref = (String) supplier.getAnnotationElementValue(annotation, SupplierHelpers.REF);
        this.realmRef = (String) supplier.getAnnotationElementValue(annotation, SupplierHelpers.REALM_REF);
    }

    public InstanceContext(Registry registry, Supplier<T, A> supplier, Class<? extends T> requestedValueType, String ref, Class<?> config) {
        this.registry = registry;
        this.supplier = supplier;
        this.annotation = null;
        this.requestedValueType = requestedValueType;
        this.config = config;
        this.lifeCycle = supplier.getDefaultLifecycle();
        this.ref = ref;
        this.realmRef = "";
    }

    public <D> D getDependency(Class<D> typeClazz) {
        return registry.getDependency(typeClazz, this);
    }

    public Registry getRegistry() {
        return registry;
    }

    void setValue(T value) {
        this.value = value;
    }

    public Supplier<T, A> getSupplier() {
        return supplier;
    }

    public T getValue() {
        return value;
    }

    public Class<? extends T> getRequestedValueType() {
        return requestedValueType;
    }

    public Class<?> getConfig() {
        return config;
    }

    public LifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public String getRef() {
        return ref;
    }

    public String getRealmRef() {
        return realmRef;
    }

    public A getAnnotation() {
        return annotation;
    }

    public Set<InstanceContext<T, A>> getDependencies() {
        return dependencies;
    }

    public void registerDependency(InstanceContext<T, A> instanceContext) {
        dependencies.add(instanceContext);
    }

    public void addNote(String key, Object value) {
        notes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <N> N getNote(String key, Class<N> type) {
        return (N) notes.get(key);
    }

}
