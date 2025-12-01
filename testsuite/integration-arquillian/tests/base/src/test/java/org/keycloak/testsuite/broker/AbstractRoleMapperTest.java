package org.keycloak.testsuite.broker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RoleBuilder;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * @author hmlnarik,
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public abstract class AbstractRoleMapperTest extends AbstractIdentityProviderMapperTest {

    protected static final String CLIENT_ID = "mapper-test-client";
    protected static final String CLIENT_ROLE = "test-role";
    protected static final String CLIENT_ROLE_MAPPER_REPRESENTATION = createClientRoleString(CLIENT_ID, CLIENT_ROLE);
    protected static final String ROLE_USER = "user";

    private static final String REALM_ROLE = "test-realm-role";

    protected String clientUuid;

    private Map<String, String> mapperIdsWithName;

    protected static String createClientRoleString(final String clientId, final String roleName) {
        return clientId + "." + roleName;
    }

    protected abstract void createMapperInIdp(IdentityProviderMapperSyncMode syncMode, String roleValue);

    protected abstract Map<String, List<String>> createUserConfigForRole(String roleValue);

    protected void updateUser() {
    }

    @Before
    public void init() {
        mapperIdsWithName = new ConcurrentHashMap<>();

        RealmResource consumerRealmResource = adminClient.realm(bc.consumerRealmName());

        clientUuid = CreatedResponseUtil.getCreatedId(
                consumerRealmResource.clients().create(ClientBuilder.create().clientId(CLIENT_ID).build()));
        consumerRealmResource.clients().get(clientUuid).roles().create(RoleBuilder.create().name(CLIENT_ROLE).build());

        consumerRealmResource.roles().create(RoleBuilder.create().name(REALM_ROLE).build());
    }

    @Test
    public void tryToCreateBrokeredUserWithNonExistingClientRoleDoesNotBreakLogin() throws IOException {
        String clientRoleStringWithMissingRole = createClientRoleString(CLIENT_ID, "does-not-exist");
        setup(clientRoleStringWithMissingRole);

        assertLoginSucceedsWithoutRoleAssignment();
    }

    /**
     * This test checks that the mapper can also be applied to realm roles (other tests mostly use client roles).
     */
    @Test
    public void mapperCanBeAppliedToRealmRoles() throws IOException {
        setup(REALM_ROLE);

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        assertThatRoleHasBeenAssignedInConsumerRealm(REALM_ROLE);
    }

    @Test
    public void mapperStillWorksWhenClientRoleIsRenamed() throws IOException {
        setup(CLIENT_ROLE_MAPPER_REPRESENTATION);

        String newRoleName = "new-name-" + CLIENT_ROLE;
        RoleRepresentation mappedRole = realm.clients().get(clientUuid).roles().get(CLIENT_ROLE).toRepresentation();
        mappedRole.setName(newRoleName);
        realm.clients().get(clientUuid).roles().get(CLIENT_ROLE).update(mappedRole);

        String expectedNewClientRoleName = createClientRoleString(CLIENT_ID, newRoleName);

        // mapper(s) should have been updated to the new client role name
        assertMappersAreConfiguredWithRole(expectedNewClientRoleName);

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        assertThatRoleHasBeenAssignedInConsumerRealm(CLIENT_ID, newRoleName);
    }

    @Test
    public void mapperStillWorksWhenClientIdIsChanged() throws IOException {
        setup(CLIENT_ROLE_MAPPER_REPRESENTATION);

        String newClientId = "new-name-" + CLIENT_ID;
        ClientRepresentation mappedClient = realm.clients().get(clientUuid).toRepresentation();
        mappedClient.setClientId(newClientId);
        realm.clients().get(clientUuid).update(mappedClient);

        String expectedNewClientRoleName = createClientRoleString(newClientId, CLIENT_ROLE);

        // mapper(s) should have been updated to the new client role name
        assertMappersAreConfiguredWithRole(expectedNewClientRoleName);

        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        assertThatRoleHasBeenAssignedInConsumerRealm(newClientId, CLIENT_ROLE);
    }

    @Test
    public void mapperStillWorksWhenRealmRoleIsRenamed() throws IOException {
        setup(REALM_ROLE);

        String newRoleName = "new-name-" + REALM_ROLE;
        RoleRepresentation mappedRole = realm.roles().get(REALM_ROLE).toRepresentation();
        mappedRole.setName(newRoleName);
        realm.roles().get(REALM_ROLE).update(mappedRole);

        // mapper(s) should have been updated to the new realm role name
        assertMappersAreConfiguredWithRole(newRoleName);

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        assertThatRoleHasBeenAssignedInConsumerRealm(newRoleName);
    }

    private void assertMappersAreConfiguredWithRole(String expectedRoleQualifier) {
        mapperIdsWithName.forEach((mapperId, mapperName) -> {
            try {
                IdentityProviderMapperRepresentation mapper =
                        realm.identityProviders().get(bc.getIDPAlias()).getMapperById(mapperId);
                Map<String, String> config = mapper.getConfig();
                assertThat(config.get(ConfigConstants.ROLE), equalTo(expectedRoleQualifier));
            } catch (final AssertionError | RuntimeException t) {
                throw new AssertionError(
                        String.format(
                                "Failed assertion for mapper with ID '%s' and name '%s'. See the cause for details.",
                                mapperId, mapperName),
                        t);
            }
        });
    }

    protected final void persistMapper(IdentityProviderMapperRepresentation idpMapper) {
        String idpAlias = bc.getIDPAlias();
        IdentityProviderResource idpResource = realm.identityProviders().get(idpAlias);
        idpMapper.setIdentityProviderAlias(idpAlias);

        String mapperId = CreatedResponseUtil.getCreatedId(idpResource.addMapper(idpMapper));
        mapperIdsWithName.put(mapperId, idpMapper.getName());
    }

    protected void loginAsUserTwiceWithMapper(IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin,
            Map<String, List<String>> userConfig) {
        setupIdentityProvider();
        if (!createAfterFirstLogin) {
            createMapperInIdp(syncMode, CLIENT_ROLE_MAPPER_REPRESENTATION);
        }
        setupUser(userConfig);

        logInAsUserInIDPForFirstTime();

        if (!createAfterFirstLogin) {
            assertThatRoleHasBeenAssignedInConsumerRealm();
        } else {
            assertThatRoleHasNotBeenAssignedInConsumerRealm();
        }

        if (createAfterFirstLogin) {
            createMapperInIdp(syncMode, CLIENT_ROLE_MAPPER_REPRESENTATION);
        }
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        updateUser();

        logInAsUserInIDP();
    }

    private void setup(String roleValue) {
        setupIdentityProvider();
        createMapperInIdp(IdentityProviderMapperSyncMode.IMPORT, roleValue);
        setupUser(createUserConfigForRole(roleValue));
    }

    private void setupUser(Map<String, List<String>> userConfig) {
        createUserInProviderRealm(userConfig);
        createUserRoleAndGrantToUserInProviderRealm();
    }

    protected void createUserRoleAndGrantToUserInProviderRealm() {
        RoleRepresentation userRole = new RoleRepresentation(ROLE_USER, null, false);
        adminClient.realm(bc.providerRealmName()).roles().create(userRole);
        RoleRepresentation role = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();
        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(role));
    }

    private void assertLoginSucceedsWithoutRoleAssignment() throws IOException {
        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        assertThatNoRolesHaveBeenAssignedInConsumerRealm();
    }

    protected void assertThatRoleHasBeenAssignedInConsumerRealm() {
        assertThatRoleHasBeenAssignedInConsumerRealm(CLIENT_ID, CLIENT_ROLE);
    }

    protected void assertThatRoleHasNotBeenAssignedInConsumerRealm() {
        UserRepresentation user = getConsumerUser();
        assertThat(user.getClientRoles().get(CLIENT_ID), not(contains(CLIENT_ROLE)));
    }

    protected void assertThatRoleHasBeenAssignedInConsumerRealm(String clientId, String roleName) {
        UserRepresentation user = getConsumerUser();
        assertThat(user.getClientRoles().get(clientId), contains(roleName));
    }

    protected void assertThatRoleHasBeenAssignedInConsumerRealm(String roleName) {
        UserRepresentation user = getConsumerUser();
        assertThat(roleName, is(in(user.getRealmRoles())));
    }

    /**
     * Check that just initial (default) roles are assigned to the user.
     */
    protected void assertThatNoRolesHaveBeenAssignedInConsumerRealm() {
        UserRepresentation user = getConsumerUser();

        Map<String, List<String>> clientRoles = user.getClientRoles();
        assertThat(clientRoles, Matchers.anyOf(aMapWithSize(0),
                hasEntry(Constants.BROKER_SERVICE_CLIENT_ID, Collections.singletonList(Constants.READ_TOKEN_ROLE))));

        List<String> realmRoles = user.getRealmRoles();
        assertThat(realmRoles, hasSize(1));
        assertThat(realmRoles, contains(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + bc.consumerRealmName()));
    }

    private UserRepresentation getConsumerUser() {
        return findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
    }

}
