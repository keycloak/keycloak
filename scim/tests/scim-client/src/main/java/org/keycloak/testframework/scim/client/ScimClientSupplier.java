package org.keycloak.testframework.scim.client;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.authorization.OAuth2Bearer;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.testframework.server.KeycloakServer;
import org.keycloak.testframework.util.ApiUtil;

import org.apache.http.client.HttpClient;

public class ScimClientSupplier implements Supplier<ScimClient, InjectScimClient>{

    @Override
    public ScimClient getValue(InstanceContext<ScimClient, InjectScimClient> instanceContext) {
        HttpClient httpClient = instanceContext.getDependency(HttpClient.class);
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        ManagedRealm managedRealm = instanceContext.getDependency(ManagedRealm.class);
        InjectScimClient config = instanceContext.getAnnotation();
        List<ClientRepresentation> scimClient = managedRealm.admin().clients().findByClientId(config.clientId());

        if (scimClient.isEmpty() && config.attachTo().isEmpty()) {
            try (Response response = managedRealm.admin().clients().create(ClientConfigBuilder.create()
                            .clientId(config.clientId())
                            .secret(config.clientSecret())
                            .serviceAccountsEnabled(true)
                            .enabled(true)
                    .build())) {
                String id = ApiUtil.getCreatedId(response);
                UserRepresentation serviceAccountUser = managedRealm.admin().clients().get(id).getServiceAccountUser();
                ClientRepresentation realmMgmtClient = managedRealm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
                RoleResource manageUsersRole = managedRealm.admin().clients().get(realmMgmtClient.getId()).roles().get(AdminRoles.MANAGE_USERS);
                managedRealm.admin().users().get(serviceAccountUser.getId()).roles()
                        .clientLevel(realmMgmtClient.getId())
                        .add(List.of(manageUsersRole.toRepresentation()));
            }
        }

        String serverBaseUrl = server.getBaseUrl();
        String tokenEndpoint = serverBaseUrl + "/realms/" + managedRealm.getName() + "/protocol/openid-connect/token";
        return ScimClient.create(httpClient)
                .withBaseUrl(serverBaseUrl + "/realms/" + managedRealm.getName())
                .withAuthorization(new OAuth2Bearer(tokenEndpoint, config.clientId(), config.clientSecret()))
                .build();
    }

    @Override
    public boolean compatible(InstanceContext<ScimClient, InjectScimClient> a, RequestedInstance<ScimClient, InjectScimClient> b) {
        return a.getAnnotation().clientId().equals(b.getAnnotation().clientId());
    }

    @Override
    public List<Dependency> getDependencies(RequestedInstance<ScimClient, InjectScimClient> instanceContext) {
        return DependenciesBuilder.create(HttpClient.class).add(KeycloakServer.class).add(ManagedRealm.class).build();
    }

    @Override
    public String getRef(InjectScimClient annotation) {
        return annotation.clientId();
    }

    @Override
    public void close(InstanceContext<ScimClient, InjectScimClient> instanceContext) {
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_REALM;
    }
}
