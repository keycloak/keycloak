package org.keycloak.testframework.scim.client;

import java.util.List;

import org.keycloak.scim.client.ScimClient;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.testframework.server.KeycloakServer;

import org.apache.http.client.HttpClient;

public class ScimClientSupplier implements Supplier<ScimClient, InjectScimClient>{

    @Override
    public ScimClient getValue(InstanceContext<ScimClient, InjectScimClient> instanceContext) {
        HttpClient httpClient = instanceContext.getDependency(HttpClient.class);
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        ManagedRealm managedRealm = instanceContext.getDependency(ManagedRealm.class);
        return ScimClient.create(httpClient).withBaseUrl(server.getBaseUrl() + "/realms/" + managedRealm.getName()).build();
    }

    @Override
    public boolean compatible(InstanceContext<ScimClient, InjectScimClient> a, RequestedInstance<ScimClient, InjectScimClient> b) {
        return true;
    }

    @Override
    public List<Dependency> getDependencies(RequestedInstance<ScimClient, InjectScimClient> instanceContext) {
        return DependenciesBuilder.create(HttpClient.class).add(KeycloakServer.class).add(ManagedRealm.class).build();
    }

    @Override
    public void close(InstanceContext<ScimClient, InjectScimClient> instanceContext) {
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_REALM;
    }
}
