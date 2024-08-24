package org.keycloak.test.framework.oauth;

import org.keycloak.test.framework.annotations.InjectOAuthClient;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;
import org.keycloak.test.framework.realm.ClientConfig;
import org.keycloak.test.framework.realm.ManagedRealm;

public class OAuthClientSupplier implements Supplier<OAuthClient, InjectOAuthClient> {

    @Override
    public Class<InjectOAuthClient> getAnnotationClass() {
        return InjectOAuthClient.class;
    }

    @Override
    public Class<OAuthClient> getValueType() {
        return OAuthClient.class;
    }

    @Override
    public OAuthClient getValue(InstanceContext<OAuthClient, InjectOAuthClient> instanceContext) {
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class);
        ClientConfig clientConfig = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        return new OAuthClient(realm, clientConfig);
    }

    @Override
    public boolean compatible(InstanceContext<OAuthClient, InjectOAuthClient> a, RequestedInstance<OAuthClient, InjectOAuthClient> b) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public void close(InstanceContext<OAuthClient, InjectOAuthClient> instanceContext) {
        instanceContext.getValue().close();
    }
}
