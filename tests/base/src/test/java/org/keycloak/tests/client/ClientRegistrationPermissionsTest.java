package org.keycloak.tests.client;


import java.util.Collections;
import java.util.List;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.Constants.REALM_MANAGEMENT_CLIENT_ID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class ClientRegistrationPermissionsTest extends AbstractClientRegistrationTest {

    @InjectUser(config = UserWithoutRoles.class)
    ManagedUser userWithoutRoles;

    @BeforeEach
    public void allowProtocolMappersInDCR() {
        // Protocol mapper types must be allowed both in the realm attribute for client admins and in the DCR policy
        managedRealm.updateWithCleanup(r -> r.attribute(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS,
                String.join(",", UserPropertyMapper.PROVIDER_ID, UserAttributeMapper.PROVIDER_ID)));
    }

    // Test for scenario when userPropertyMapper is added with admin roles being added to the token, but UserModel does not have any admin roles
    @Test
    public void testUserPropertyMapper_rolesUnavailableOnUser() throws Exception {
        List<ProtocolMapperRepresentation> protocolMappers = List.of(
                createUserPropertyRolesMapperRep("OIDC firstName to role mapper", "firstName"),
                createUserPropertyRolesMapperRep("OIDC lastName to role mapper", "lastName"));
        testMapper_rolesUnavailableOnUser(protocolMappers);
    }


    // Test for scenario when userAttributeMapper is added with admin roles being added to the token, but UserModel does not have any admin roles
    @Test
    public void testUserAttributeMapper_rolesUnavailableOnUser() throws Exception {
        List<ProtocolMapperRepresentation> protocolMappers = List.of(
                createUserAttributeRolesMapperRep("OIDC firstName to role mapper", "firstName"),
                createUserAttributeRolesMapperRep("OIDC lastName to role mapper", "lastName"));
        testMapper_rolesUnavailableOnUser(protocolMappers);
    }

    // Test for scenario when userPropertyMapper is added with admin roles being added to the token, and UserModel has admin roles
    @Test
    public void testUserPropertyMapper_rolesAvailableOnUser() throws Exception {
        List<ProtocolMapperRepresentation> protocolMappers = List.of(
                createUserPropertyRolesMapperRep("OIDC firstName to role mapper", "firstName"),
                createUserPropertyRolesMapperRep("OIDC lastName to role mapper", "lastName"));
        testMapper_rolesAvailableOnUser(protocolMappers);
    }


    // Test for scenario when userAttributeMapper is added with admin roles being added to the token, and UserModel has admin roles
    @Test
    public void testUserAttributeMapper_rolesAvailableOnUser() throws Exception {
        List<ProtocolMapperRepresentation> protocolMappers = List.of(
                createUserAttributeRolesMapperRep("OIDC firstName to role mapper", "firstName"),
                createUserAttributeRolesMapperRep("OIDC lastName to role mapper", "lastName"));
        testMapper_rolesAvailableOnUser(protocolMappers);
    }

    private void testMapper_rolesUnavailableOnUser(List<ProtocolMapperRepresentation> protocolMappers) throws Exception {
        // Create a client with mappers
        ClientRepresentation clientRep = buildClient();
        clientRep.setDirectAccessGrantsEnabled(true);
        clientRep.setProtocolMappers(protocolMappers);

        ClientRepresentation registeredClient = createClient(clientRep);
        assertNotNull(registeredClient.getRegistrationAccessToken());
        managedRealm.cleanup().add(r -> r.clients().get(registeredClient.getId()).remove());

        // Authenticate user to the client and then use as access-token for DCR
        oauth.client(CLIENT_ID, CLIENT_SECRET);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("perm-test-user", "password");
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken(), AccessToken.class);
        assertTrue(accessToken.getResourceAccess(REALM_MANAGEMENT_CLIENT_ID).getRoles().isEmpty());

        reg.auth(Auth.token(tokenResponse.getAccessToken()));
        try {
            reg.oidc().get(REALM_MANAGEMENT_CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    private void testMapper_rolesAvailableOnUser(List<ProtocolMapperRepresentation> protocolMappers) throws Exception {
        // Create a client with mappers
        ClientRepresentation clientRep = buildClient();
        clientRep.setDirectAccessGrantsEnabled(true);
        clientRep.setProtocolMappers(protocolMappers);

        ClientRepresentation registeredClient = createClient(clientRep);
        assertNotNull(registeredClient.getRegistrationAccessToken());
        managedRealm.cleanup().add(r -> r.clients().get(registeredClient.getId()).remove());

        // Add roles to the user
        String realmMgmtClientUUID = managedRealm.admin().clients().findByClientId(REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        List<RoleRepresentation> manageClientsRole = Collections
                .singletonList(managedRealm.admin().clients().get(realmMgmtClientUUID).roles().get(AdminRoles.MANAGE_CLIENTS).toRepresentation());
        userWithoutRoles.admin().roles().clientLevel(realmMgmtClientUUID).add(manageClientsRole);

        // Authenticate user to the client and then use as access-token for DCR. Should be successful as user has "manage-clients" role
        oauth.client(CLIENT_ID, CLIENT_SECRET);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("perm-test-user", "password");
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken(), AccessToken.class);
        assertTrue(accessToken.getResourceAccess(REALM_MANAGEMENT_CLIENT_ID).getRoles().contains(AdminRoles.MANAGE_CLIENTS));

        reg.auth(Auth.token(tokenResponse.getAccessToken()));
        OIDCClientRepresentation realmMgmtClient = reg.oidc().get(REALM_MANAGEMENT_CLIENT_ID);
        assertNotNull(realmMgmtClient);

        // Remove roles from the user. Make sure he does not have permissions anymore
        userWithoutRoles.admin().roles().clientLevel(realmMgmtClientUUID).remove(manageClientsRole);

        try {
            reg.oidc().get(REALM_MANAGEMENT_CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }


    private ProtocolMapperRepresentation createUserPropertyRolesMapperRep(String mapperName, String attrName) {
        ProtocolMapperRepresentation rep = ModelToRepresentation.toRepresentation(UserPropertyMapper.createClaimMapper(
                mapperName,
                attrName,
                "resource_access.realm-management.roles",
                String.class.getSimpleName(),
                true,
                true,
                true
        ));
        rep.getConfig().put(ProtocolMapperUtils.MULTIVALUED, "true");
        return rep;
    }

    private ProtocolMapperRepresentation createUserAttributeRolesMapperRep(String mapperName, String attrName) {
        ProtocolMapperRepresentation rep = ModelToRepresentation.toRepresentation(UserAttributeMapper.createClaimMapper(
                mapperName,
                attrName,
                "resource_access.realm-management.roles",
                String.class.getSimpleName(),
                true,
                true,
                true
        ));
        rep.getConfig().put(ProtocolMapperUtils.MULTIVALUED, "true");
        return rep;
    }

    public static class UserWithoutRoles implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return UserBuilder.create()
                    .username("perm-test-user")
                    .name(AdminRoles.MANAGE_USERS, AdminRoles.MANAGE_CLIENTS) // Strange firstName and lastName to be able to map those claims to roles
                    .password("password")
                    .email("perm-test-user@localhost")
                    .emailVerified(true);
        }
    }
}
