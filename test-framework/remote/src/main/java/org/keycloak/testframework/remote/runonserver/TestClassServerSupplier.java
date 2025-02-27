package org.keycloak.testframework.remote.runonserver;

import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;

public class TestClassServerSupplier implements Supplier<TestClassServer, InjectTestClassServer> {

    @Override
    public TestClassServer getValue(InstanceContext<TestClassServer, InjectTestClassServer> instanceContext) {
        return new TestClassServer();
    }

    @Override
    public boolean compatible(InstanceContext<TestClassServer, InjectTestClassServer> a, RequestedInstance<TestClassServer, InjectTestClassServer> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<TestClassServer, InjectTestClassServer> instanceContext) {
        instanceContext.getValue().close();
    }
}
