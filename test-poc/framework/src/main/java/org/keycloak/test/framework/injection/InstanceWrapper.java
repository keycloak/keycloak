package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstanceWrapper<T, A extends Annotation> {

    private final Supplier<T, A> supplier;
    private final A annotation;
    private final Set<InstanceWrapper<T, A>> dependencies = new HashSet<>();
    private T value;
    private LifeCycle lifeCycle;
    private final Map<String, Object> notes = new HashMap<>();

    public InstanceWrapper(Supplier<T, A> supplier, A annotation) {
        this.supplier = supplier;
        this.annotation = annotation;
    }

    public InstanceWrapper(Supplier<T, A> supplier, A annotation, T value, LifeCycle lifeCycle) {
        this.supplier = supplier;
        this.annotation = annotation;
        this.value = value;
        this.lifeCycle = lifeCycle;
    }

    public void setValue(T value, LifeCycle lifeCycle) {
        this.value = value;
        this.lifeCycle = lifeCycle;
    }

    public Supplier<T, A> getSupplier() {
        return supplier;
    }

    public T getValue() {
        return value;
    }

    public LifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public A getAnnotation() {
        return annotation;
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
