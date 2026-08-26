package org.keycloak.tests.admin.authz;

import java.util.Set;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class UMAGrantTypeTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectClient(config = AuthzResourceServerConfig.class)
    ManagedClient resourceServer;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void testAdminRolesIgnoredWhenUsingIdTokenAsClaimToken() {
        ClientResource clientResource = resourceServer.admin();
        String clientId = resourceServer.getClientId();
        String clientSecret = resourceServer.getSecret();

        ProtocolMapperRepresentation mapper = toRepresentation(
                HardcodedRole.create("inject-view-clients",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.VIEW_CLIENTS)
        );
        clientResource.getProtocolMappers().createMapper(mapper).close();

        ClientRepresentation realmMgmt = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation viewClientsRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation();

        RolePolicyRepresentation rolePolicy = new RolePolicyRepresentation();
        rolePolicy.setName("require-view-clients");
        rolePolicy.addRole(viewClientsRole.getId(), true);
        try (Response response = clientResource.authorization().policies().role().create(rolePolicy)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }
        rolePolicy = clientResource.authorization().policies().role().findByName("require-view-clients");

        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setName("protected-resource");
        try (Response response = clientResource.authorization().resources().create(resource)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }
        resource = clientResource.authorization().resources().findByName("protected-resource").get(0);

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        permission.setName("protect-resource");
        permission.setResources(Set.of(resource.getId()));
        permission.setPolicies(Set.of(rolePolicy.getId()));
        try (Response response = clientResource.authorization().permissions().resource().create(permission)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        clientResource.authorization().resources().findByName("Default Resource")
                .forEach(r -> clientResource.authorization().resources().resource(r.getId()).remove());

        AccessTokenResponse tokenResponse = oauth.client(clientId, clientSecret)
                .doPasswordGrantRequest(user.getUsername(), user.getPassword());
        String idToken = tokenResponse.getIdToken();
        assertNotNull(idToken);

        tokenResponse = oauth.client(clientId, clientSecret)
                .permissionGrantRequest()
                .claimToken(idToken)
                .send();
        assertNull(tokenResponse.getAccessToken(),
                "Authorization should be denied when admin role is only mapper-injected via IDToken claim_token");
        assertNotNull(tokenResponse.getError(),
                "Response should contain an error when admin role is only mapper-injected");
    }

    public static class AuthzResourceServerConfig implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client
                    .secret("secret")
                    .serviceAccountsEnabled(true)
                    .authorizationServicesEnabled(true)
                    .directAccessGrantsEnabled(true);
        }
    }
}
