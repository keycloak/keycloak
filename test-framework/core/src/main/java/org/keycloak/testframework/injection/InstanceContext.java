package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstanceContext<T, A extends Annotation> {

    private final int instanceId;
    private final Registry registry;
    private final Supplier<T, A> supplier;
    private final A annotation;
    private final Set<InstanceContext<?, ?>> dependents = new HashSet<>();
    private T value;
    private Class<? extends T> requestedValueType;
    private LifeCycle lifeCycle;
    private final String ref;
    private final Map<String, Object> notes = new HashMap<>();
    private final List<Dependency> declaredDependencies;

    public InstanceContext(int instanceId, Registry registry, Supplier<T, A> supplier, A annotation, Class<? extends T> requestedValueType, List<Dependency> declaredDependencies) {
        this.instanceId = instanceId != -1 ? instanceId : hashCode();
        this.registry = registry;
        this.supplier = supplier;
        this.annotation = annotation;
        this.requestedValueType = requestedValueType;
        this.lifeCycle = supplier.getLifeCycle(annotation);
        this.ref = StringUtil.convertEmptyToNull(supplier.getRef(annotation));
        this.declaredDependencies = declaredDependencies;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public <D> D getDependency(Class<D> typeClazz) {
        return getDependency(typeClazz, null);
    }

    public <D> D getDependency(Class<D> typeClazz, String ref) {
        return registry.getDependency(typeClazz, ref, this);
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

    public LifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public String getRef() {
        return ref;
    }

    public A getAnnotation() {
        return annotation;
    }

    public Set<InstanceContext<?, ?>> getDependents() {
        return dependents;
    }

    public List<Dependency> getDeclaredDependencies() {
        return declaredDependencies;
    }

    public void registerDependent(InstanceContext<?, ?> instanceContext) {
        dependents.add(instanceContext);
    }

    public void addNote(String key, Object value) {
        notes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <N> N getNote(String key, Class<N> type) {
        return (N) notes.get(key);
    }

}
