package org.keycloak.testframework.saml;

import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.saml.annotations.InjectTestSamlApp;

import com.sun.net.httpserver.HttpServer;

public class TestSamlAppSupplier implements Supplier<TestSamlApp, InjectTestSamlApp> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<TestSamlApp, InjectTestSamlApp> instanceContext) {
        return DependenciesBuilder.create(HttpServer.class).build();
    }

    @Override
    public TestSamlApp getValue(InstanceContext<TestSamlApp, InjectTestSamlApp> instanceContext) {
        HttpServer httpServer = instanceContext.getDependency(HttpServer.class);
        return new TestSamlApp(httpServer);
    }

    @Override
    public boolean compatible(InstanceContext<TestSamlApp, InjectTestSamlApp> a, RequestedInstance<TestSamlApp, InjectTestSamlApp> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<TestSamlApp, InjectTestSamlApp> instanceContext) {
        instanceContext.getValue().close();
    }
}
