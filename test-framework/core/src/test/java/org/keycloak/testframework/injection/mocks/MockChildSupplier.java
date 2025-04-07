package org.keycloak.testframework.injection.mocks;

import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;

public class MockChildSupplier implements Supplier<MockChildValue, MockChildAnnotation> {

    public static LifeCycle DEFAULT_LIFECYCLE = LifeCycle.CLASS;

    public static void reset() {
        DEFAULT_LIFECYCLE = LifeCycle.CLASS;
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
