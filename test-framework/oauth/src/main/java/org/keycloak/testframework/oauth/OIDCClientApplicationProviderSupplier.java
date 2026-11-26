package org.keycloak.testframework.oauth;

import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.oauth.annotations.InjectOIDCClientApplicationProvider;

import com.sun.net.httpserver.HttpServer;

public class OIDCClientApplicationProviderSupplier implements Supplier<OIDCClientApplicationProvider, InjectOIDCClientApplicationProvider> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<OIDCClientApplicationProvider, InjectOIDCClientApplicationProvider> instanceContext) {
        return DependenciesBuilder.create(HttpServer.class).add(JwksProvider.class).build();
    }

    @Override
    public OIDCClientApplicationProvider getValue(InstanceContext<OIDCClientApplicationProvider, InjectOIDCClientApplicationProvider> instanceContext) {
        HttpServer httpServer = instanceContext.getDependency(HttpServer.class);
        JwksProvider jwksProvider = instanceContext.getDependency(JwksProvider.class);
        OIDCClientApplicationProviderConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        return new OIDCClientApplicationProvider(httpServer, jwksProvider, config.build());
    }

    @Override
    public void onBeforeEach(InstanceContext<OIDCClientApplicationProvider, InjectOIDCClientApplicationProvider> instanceContext) {
        instanceContext.getValue().reset();
    }

    @Override
    public void close(InstanceContext<OIDCClientApplicationProvider, InjectOIDCClientApplicationProvider> instanceContext) {
        instanceContext.getValue().close();
    }

    @Override
    public boolean compatible(InstanceContext<OIDCClientApplicationProvider, InjectOIDCClientApplicationProvider> a, RequestedInstance<OIDCClientApplicationProvider, InjectOIDCClientApplicationProvider> b) {
        return a.getAnnotation().equals(b.getAnnotation());
    }
}
