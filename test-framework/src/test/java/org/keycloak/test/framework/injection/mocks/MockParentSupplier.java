package org.keycloak.test.framework.injection.mocks;

import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;

public class MockParentSupplier implements Supplier<MockParentValue, MockParentAnnotation> {

    public static LifeCycle DEFAULT_LIFECYCLE = LifeCycle.CLASS;
    public static boolean COMPATIBLE = true;

    public static void reset() {
        DEFAULT_LIFECYCLE = LifeCycle.CLASS;
        COMPATIBLE = true;
    }

    @Override
    public Class<MockParentAnnotation> getAnnotationClass() {
        return MockParentAnnotation.class;
    }

    @Override
    public Class<MockParentValue> getValueType() {
        return MockParentValue.class;
    }

    @Override
    public MockParentValue getValue(InstanceContext<MockParentValue, MockParentAnnotation> instanceContext) {
        return new MockParentValue();
    }

    @Override
    public boolean compatible(InstanceContext<MockParentValue, MockParentAnnotation> a, RequestedInstance<MockParentValue, MockParentAnnotation> b) {
        return COMPATIBLE;
    }

    @Override
    public void close(InstanceContext<MockParentValue, MockParentAnnotation> instanceContext) {
        instanceContext.getValue().close();
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return DEFAULT_LIFECYCLE;
    }
}
