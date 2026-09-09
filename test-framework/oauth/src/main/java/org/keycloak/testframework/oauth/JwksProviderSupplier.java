package org.keycloak.testframework.oauth;

import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.oauth.annotations.InjectJwksProvider;

import com.sun.net.httpserver.HttpServer;

public class JwksProviderSupplier implements Supplier<JwksProvider, InjectJwksProvider> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<JwksProvider, InjectJwksProvider> instanceContext) {
        return DependenciesBuilder.create(HttpServer.class).build();
    }

    @Override
    public JwksProvider getValue(InstanceContext<JwksProvider, InjectJwksProvider> instanceContext) {
        HttpServer httpServer = instanceContext.getDependency(HttpServer.class);
        return new JwksProvider(httpServer);
    }

    @Override
    public void close(InstanceContext<JwksProvider, InjectJwksProvider> instanceContext) {
        instanceContext.getValue().close();
    }

    @Override
    public boolean compatible(InstanceContext<JwksProvider, InjectJwksProvider> a, RequestedInstance<JwksProvider, InjectJwksProvider> b) {
        return true;
    }
}
