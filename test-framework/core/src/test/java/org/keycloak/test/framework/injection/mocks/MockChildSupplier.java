package org.keycloak.test.framework.injection.mocks;

import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;

public class MockChildSupplier implements Supplier<MockChildValue, MockChildAnnotation> {

    public static LifeCycle DEFAULT_LIFECYCLE = LifeCycle.CLASS;

    public static void reset() {
        DEFAULT_LIFECYCLE = LifeCycle.CLASS;
    }

    @Override
    public Class<MockChildAnnotation> getAnnotationClass() {
        return MockChildAnnotation.class;
    }

    @Override
    public Class<MockChildValue> getValueType() {
        return MockChildValue.class;
    }

    @Override
    public MockChildValue getValue(InstanceContext<MockChildValue, MockChildAnnotation> instanceContext) {
        MockParentValue mockParentValue = instanceContext.getDependency(MockParentValue.class, instanceContext.getAnnotation().parentRef());
        return new MockChildValue(mockParentValue);
    }

    @Override
    public boolean compatible(InstanceContext<MockChildValue, MockChildAnnotation> a, RequestedInstance<MockChildValue, MockChildAnnotation> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<MockChildValue, MockChildAnnotation> instanceContext) {
        instanceContext.getValue().close();
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return DEFAULT_LIFECYCLE;
    }
}
