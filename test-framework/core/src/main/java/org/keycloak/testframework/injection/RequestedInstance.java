package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequestedInstance<T, A extends Annotation> {

    private final int instanceId;
    private final Supplier<T, A> supplier;
    private final A annotation;
    private final Set<InstanceContext<?, ?>> dependents = new HashSet<>();
    private final Class<? extends T> valueType;
    private final LifeCycle lifeCycle;
    private final String ref;
    private List<Dependency> declaredDependencies;

    public RequestedInstance(Supplier<T, A> supplier, A annotation, Class<? extends T> valueType) {
        this.instanceId = this.hashCode();
        this.supplier = supplier;
        this.annotation = annotation;
        this.valueType = valueType;
        this.lifeCycle = supplier.getLifeCycle(annotation);
        this.ref = StringUtil.convertEmptyToNull(supplier.getRef(annotation));
    }

    public int getInstanceId() {
        return instanceId;
    }

    public Supplier<T, A> getSupplier() {
        return supplier;
    }

    public A getAnnotation() {
        return annotation;
    }

    public Class<? extends T> getValueType() {
        return valueType;
    }

    public LifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public String getRef() {
        return ref;
    }

    public void registerDependent(InstanceContext<?, ?> instanceContext) {
        dependents.add(instanceContext);
    }

    public Set<InstanceContext<?, ?>> getDependents() {
        return dependents;
    }

    public List<Dependency> getDeclaredDependencies() {
        return declaredDependencies;
    }

    public void setDeclaredDependencies(List<Dependency> declaredDependencies) {
        this.declaredDependencies = declaredDependencies;
    }
}
