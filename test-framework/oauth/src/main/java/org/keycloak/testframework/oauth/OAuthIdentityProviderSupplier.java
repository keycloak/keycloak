package org.keycloak.testframework.oauth;

import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;

import com.sun.net.httpserver.HttpServer;

public class OAuthIdentityProviderSupplier implements Supplier<OAuthIdentityProvider, InjectOAuthIdentityProvider> {

    @Override
    public OAuthIdentityProvider getValue(InstanceContext<OAuthIdentityProvider, InjectOAuthIdentityProvider> instanceContext) {
        HttpServer httpServer = instanceContext.getDependency(HttpServer.class);
        OAuthIdentityProviderConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        OAuthIdentityProviderConfigBuilder configBuilder = new OAuthIdentityProviderConfigBuilder();
        OAuthIdentityProviderConfigBuilder.OAuthIdentityProviderConfiguration configuration = config.configure(configBuilder).build();

        return new OAuthIdentityProvider(httpServer, configuration);
    }

    @Override
    public void close(InstanceContext<OAuthIdentityProvider, InjectOAuthIdentityProvider> instanceContext) {
        instanceContext.getValue().close();
    }

    @Override
    public boolean compatible(InstanceContext<OAuthIdentityProvider, InjectOAuthIdentityProvider> a, RequestedInstance<OAuthIdentityProvider, InjectOAuthIdentityProvider> b) {
        return a.getAnnotation().equals(b.getAnnotation());
    }

}
