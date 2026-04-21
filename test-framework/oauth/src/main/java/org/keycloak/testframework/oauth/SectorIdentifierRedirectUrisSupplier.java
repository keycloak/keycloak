package org.keycloak.testframework.oauth;

import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.oauth.annotations.InjectSectorIdentifierRedirectUrisProvider;

import com.sun.net.httpserver.HttpServer;

/**
 *
 * @author rmartinc
 */
public class SectorIdentifierRedirectUrisSupplier implements Supplier<SectorIdentifierRedirectUrisProvider, InjectSectorIdentifierRedirectUrisProvider> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<SectorIdentifierRedirectUrisProvider, InjectSectorIdentifierRedirectUrisProvider> instanceContext) {
        return DependenciesBuilder.create(HttpServer.class).build();
    }

    @Override
    public SectorIdentifierRedirectUrisProvider getValue(InstanceContext<SectorIdentifierRedirectUrisProvider, InjectSectorIdentifierRedirectUrisProvider> instanceContext) {
        HttpServer httpServer = instanceContext.getDependency(HttpServer.class);
        String[] uris = instanceContext.getAnnotation().value();
        return new SectorIdentifierRedirectUrisProvider(httpServer, uris);
    }

    @Override
    public boolean compatible(InstanceContext<SectorIdentifierRedirectUrisProvider, InjectSectorIdentifierRedirectUrisProvider> a, RequestedInstance<SectorIdentifierRedirectUrisProvider, InjectSectorIdentifierRedirectUrisProvider> b) {
        return a.getAnnotation().equals(b.getAnnotation());
    }

    @Override
    public void close(InstanceContext<SectorIdentifierRedirectUrisProvider, InjectSectorIdentifierRedirectUrisProvider> instanceContext) {
        instanceContext.getValue().close();
    }
}
