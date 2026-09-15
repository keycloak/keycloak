package org.keycloak.testframework.oauth;

import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.oauth.annotations.InjectCibaProvider;

import com.sun.net.httpserver.HttpServer;

/**
 *
 * @author rmartinc
 */
public class CibaProviderSupplier implements Supplier<CibaProvider, InjectCibaProvider>{

    @Override
    public List<Dependency> getDependencies(RequestedInstance<CibaProvider, InjectCibaProvider> instanceContext) {
        return DependenciesBuilder.create(HttpServer.class).build();
    }

    @Override
    public CibaProvider getValue(InstanceContext<CibaProvider, InjectCibaProvider> instanceContext) {
        HttpServer httpServer = instanceContext.getDependency(HttpServer.class);
        return new CibaProvider(httpServer);
    }

    @Override
    public boolean compatible(InstanceContext<CibaProvider, InjectCibaProvider> a, RequestedInstance<CibaProvider, InjectCibaProvider> b) {
        return a.getAnnotation().equals(b.getAnnotation());
    }

    @Override
    public void close(InstanceContext<CibaProvider, InjectCibaProvider> instanceContext) {
        instanceContext.getValue().close();
    }
}
