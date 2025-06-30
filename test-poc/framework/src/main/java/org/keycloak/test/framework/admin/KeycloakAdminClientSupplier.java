package org.keycloak.test.framework.admin;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.test.framework.TestAdminClient;
import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.server.KeycloakTestServer;

public class KeycloakAdminClientSupplier implements Supplier<Keycloak, TestAdminClient> {

    @Override
    public Class<TestAdminClient> getAnnotationClass() {
        return TestAdminClient.class;
    }

    @Override
    public Class<Keycloak> getValueType() {
        return Keycloak.class;
    }

    @Override
    public InstanceWrapper<Keycloak, TestAdminClient> getValue(Registry registry, TestAdminClient annotation) {
        InstanceWrapper<Keycloak, TestAdminClient> wrapper = new InstanceWrapper<>(this, annotation);

        KeycloakTestServer testServer = registry.getDependency(KeycloakTestServer.class, wrapper);

        Keycloak keycloak = Keycloak.getInstance(testServer.getBaseUrl(), "master", "admin", "admin", "admin-cli");
        wrapper.setValue(keycloak);

        return wrapper;
    }

    @Override
    public LifeCycle getLifeCycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceWrapper<Keycloak, TestAdminClient> a, InstanceWrapper<Keycloak, TestAdminClient> b) {
        return true;
    }

    @Override
    public void close(Keycloak keycloak) {
        keycloak.close();
    }

}
