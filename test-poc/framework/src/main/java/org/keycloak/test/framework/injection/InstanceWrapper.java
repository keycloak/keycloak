package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstanceWrapper<T, A extends Annotation> {

    private final Supplier<T, A> supplier;
    private final A annotation;
    private final Set<InstanceWrapper<T, A>> dependencies = new HashSet<>();
    private final Field field;
    private T value;
    private final Map<String, Object> notes = new HashMap<>();

    public InstanceWrapper(Supplier<T, A> supplier, A annotation) {
        this(supplier, annotation, null, null);
    }

    public InstanceWrapper(Supplier<T, A> supplier, A annotation, T value) {
        this(supplier, annotation, null, value);
    }

    public InstanceWrapper(Supplier<T, A> supplier, A annotation, Field field, T value) {
        this.supplier = supplier;
        this.annotation = annotation;
        this.field = field;
        this.value = value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Supplier<T, A> getSupplier() {
        return supplier;
    }

    public T getValue() {
        return value;
    }

    public A getAnnotation() {
        return annotation;
    }

    public Field getField() {
        return field;
    }

    public Set<InstanceWrapper<T, A>> getDependencies() {
        return dependencies;
    }

    public void registerDependency(InstanceWrapper<T, A> instanceWrapper) {
        dependencies.add(instanceWrapper);
    }

    public void addNote(String key, Object value) {
        notes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <N> N getNote(String key, Class<N> type) {
        return (N) notes.get(key);
    }

}
