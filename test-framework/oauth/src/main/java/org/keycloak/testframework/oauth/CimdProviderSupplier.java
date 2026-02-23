
package org.keycloak.testframework.oauth;

import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.oauth.annotations.InjectCimdProvider;

import com.sun.net.httpserver.HttpServer;

/**
 *
 * @author rmartinc
 */
public class CimdProviderSupplier implements Supplier<CimdProvider, InjectCimdProvider> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<CimdProvider, InjectCimdProvider> instanceContext) {
        return DependenciesBuilder.create(HttpServer.class).build();
    }

    @Override
    public CimdProvider getValue(InstanceContext<CimdProvider, InjectCimdProvider> instanceContext) {
        HttpServer httpServer = instanceContext.getDependency(HttpServer.class);
        OIDCClientRepresentationBuilder clientBuilder = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        return new CimdProvider(httpServer, clientBuilder.build());
    }

    @Override
    public void close(InstanceContext<CimdProvider, InjectCimdProvider> instanceContext) {
        instanceContext.getValue().close();
    }

    @Override
    public boolean compatible(InstanceContext<CimdProvider, InjectCimdProvider> a, RequestedInstance<CimdProvider, InjectCimdProvider> b) {
        return a.getAnnotation().equals(b.getAnnotation());
    }
}
