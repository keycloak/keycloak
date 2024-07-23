package org.keycloak.test.framework.admin;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.test.framework.annotations.AdminClient;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.server.KeycloakTestServer;

public class KeycloakAdminClientSupplier implements Supplier<Keycloak, AdminClient> {

    @Override
    public Class<AdminClient> getAnnotationClass() {
        return AdminClient.class;
    }

    @Override
    public Class<Keycloak> getValueType() {
        return Keycloak.class;
    }

    @Override
    public Keycloak getValue(InstanceContext<Keycloak, AdminClient> instanceContext) {
        KeycloakTestServer testServer = instanceContext.getDependency(KeycloakTestServer.class);
        return Keycloak.getInstance(testServer.getBaseUrl(), "master", "admin", "admin", "admin-cli");
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<Keycloak, AdminClient> a, RequestedInstance<Keycloak, AdminClient> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<Keycloak, AdminClient> instanceContext) {
        instanceContext.getValue().close();
    }

}
