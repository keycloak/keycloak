package org.keycloak.tests.scim.tck;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.user.User;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that verify SCIM behavior with users backed by a user storage (federation) provider.
 *
 * SCIM read operations (GET, list, search) only ever consider locally stored users - see
 * {@link org.keycloak.scim.model.user.UserResourceTypeProvider}. Write operations (POST /Users),
 * however, go through the same {@code UserProfile.create()} -&gt; {@code session.users().addUser()}
 * path used by the regular Admin REST API, so a user storage provider that intercepts registration
 * (a "sync registration" provider, such as LDAP with write-back enabled) may end up creating the
 * user outside of local storage. If that provider also does not import users into the local
 * database, the newly created user becomes unreachable via any subsequent SCIM operation, even
 * though the initial POST succeeded. This is a documented limitation of this version of the SCIM
 * API - see the "Creating a user" and "Deleting a user" sections of the SCIM server admin guide.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/51343">Issue 51343</a>
 * @see <a href="https://github.com/keycloak/keycloak/issues/51735">Issue 51735</a>
 */
@KeycloakIntegrationTest(config = FederatedUserTest.FederatedUserServerConfig.class)
public class FederatedUserTest extends AbstractScimTest {

    private static final String PROVIDER_ID = "user-password-map-arq";

    /**
     * Verifies that SCIM filtered and unfiltered searches return consistent results
     * when non-imported federated users exist.
     *
     * Before the fix, unfiltered searches went through UserStorageManager (which
     * queries federated providers), while filtered searches used direct JPA queries
     * against the local UserEntity table - so non-imported federated users would
     * only appear in unfiltered results.
     */
    @Test
    public void testFilteredAndUnfilteredSearchConsistency() {
        // create local users first, before registering the federation provider -
        // otherwise the provider (which implements UserRegistrationProvider) intercepts
        // user creation and the users end up as federated, not local.
        createLocalUser("local-alice");
        createLocalUser("local-bob");

        // register the federation provider with import disabled and create federated users
        String federationComponentId = registerFederationProvider();
        try {
            String charlieId = createFederatedUser("fed-charlie");
            String dianaId = createFederatedUser("fed-diana");

            // unfiltered SCIM search - now uses local storage after the fix
            ListResponse<User> unfilteredResponse = client.users().getAll();
            assertNotNull(unfilteredResponse);
            List<String> unfilteredUserNames = toUserNames(unfilteredResponse);

            // filtered SCIM search (broad filter matching all users) - goes through JPA
            String broadFilter = ResourceFilter.filter().pr("userName").build();
            ListResponse<User> filteredResponse = client.users().getAll(broadFilter);
            assertNotNull(filteredResponse);
            List<String> filteredUserNames = toUserNames(filteredResponse);

            // both local users should appear in both result sets
            assertThat(unfilteredUserNames, hasItems("local-alice", "local-bob"));
            assertThat(filteredUserNames, hasItems("local-alice", "local-bob"));

            // both searches should return the same number of results
            assertEquals(unfilteredResponse.getTotalResults(), filteredResponse.getTotalResults(),
                    "Filtered and unfiltered SCIM searches should return the same number of results. "
                    + "Unfiltered returned: " + unfilteredUserNames + ", filtered returned: " + filteredUserNames);

            // non-imported federated users should not appear in SCIM results
            assertThat(unfilteredUserNames, not(hasItem("fed-charlie")));
            assertThat(unfilteredUserNames, not(hasItem("fed-diana")));
            assertThat(filteredUserNames, not(hasItem("fed-charlie")));
            assertThat(filteredUserNames, not(hasItem("fed-diana")));

            // non-imported federated users should not be retrievable by ID either
            assertNull(client.users().get(charlieId), "GET by ID should not return non-imported federated user");
            assertNull(client.users().get(dianaId), "GET by ID should not return non-imported federated user");
        } finally {
            realm.admin().components().component(federationComponentId).remove();
        }
    }

    /**
     * When the federation provider does not intercept registration (sync registration disabled),
     * SCIM user creation must behave exactly as if no federation provider was configured: the user
     * is created locally and is fully manageable (found via GET/search, and deletable) regardless
     * of the provider's import setting.
     */
    @Test
    public void testCreateWithoutSyncRegistration() {
        String federationComponentId = registerFederationProvider(false, false);
        try {
            User created = createScimUser("no-sync-user");
            assertNotNull(created);
            assertTrue(StorageId.isLocalStorage(created.getId()),
                    "User should be created in local storage when the federation provider does not synchronize registrations");

            assertNotNull(client.users().get(created.getId()), "Locally created user should be retrievable via GET");
            assertThat(toUserNames(client.users().getAll()), hasItem("no-sync-user"));

            client.users().delete(created.getId());
            assertNull(client.users().get(created.getId()), "User should no longer be found after deletion");
        } finally {
            realm.admin().components().component(federationComponentId).remove();
        }
    }

    /**
     * When the federation provider synchronizes registrations and imports users, the local database
     * row is created synchronously as part of the same call - there is no separate sync step - so the
     * created user is immediately visible, searchable, and deletable via SCIM.
     */
    @Test
    public void testCreateWithSyncRegistrationAndImportEnabled() {
        String federationComponentId = registerFederationProvider(true, true);
        try {
            User created = createScimUser("sync-import-user");
            assertNotNull(created);

            User fetched = client.users().get(created.getId());
            assertNotNull(fetched, "Federated user should be immediately visible via GET when import is enabled");
            assertEquals("sync-import-user", fetched.getUserName());
            assertThat(toUserNames(client.users().getAll()), hasItem("sync-import-user"));

            client.users().delete(created.getId());
            assertNull(client.users().get(created.getId()), "User should no longer be found after deletion");
        } finally {
            realm.admin().components().component(federationComponentId).remove();
        }
    }

    /**
     * When the federation provider synchronizes registrations but does not import users, SCIM user
     * creation still succeeds (mirroring the regular Admin API), but the user is stored only in the
     * external provider. As a result, the created user is unreachable via any subsequent SCIM
     * operation: GET and search do not find it, and DELETE returns 404 - since the local-storage-only
     * lookup used to resolve the user for deletion cannot find it either.
     */
    @Test
    public void testCreateWithSyncRegistrationAndImportDisabled() {
        String federationComponentId = registerFederationProvider(true, false);
        try {
            User created = createScimUser("sync-no-import-user");
            assertNotNull(created, "Creation succeeds even though the user is not imported locally");
            assertFalse(StorageId.isLocalStorage(created.getId()));

            assertNull(client.users().get(created.getId()), "GET should not find a non-imported federated user");
            assertThat(toUserNames(client.users().getAll()), not(hasItem("sync-no-import-user")));

            ScimClientException exception = assertThrows(ScimClientException.class, () -> client.users().delete(created.getId()));
            assertEquals(HttpStatus.SC_NOT_FOUND, exception.getError().getStatusInt(),
                    "DELETE should return 404 for a non-imported federated user");
        } finally {
            realm.admin().components().component(federationComponentId).remove();
        }
    }

    private List<String> toUserNames(ListResponse<User> response) {
        return response.getResources().stream()
                .map(User::getUserName)
                .collect(Collectors.toList());
    }

    private void createLocalUser(String username) {
        User user = new User();
        user.setUserName(username);
        user.setActive(true);
        user = client.users().create(user);
        assertNotNull(user);
    }

    private User createScimUser(String username) {
        User user = new User();
        user.setUserName(username);
        user.setActive(true);
        return client.users().create(user);
    }

    private String registerFederationProvider() {
        return registerFederationProvider(true, false);
    }

    private String registerFederationProvider(boolean syncRegistrations, boolean importEnabled) {
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("test-user-federation");
        provider.setProviderId(PROVIDER_ID);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<String, String>());
        provider.getConfig().putSingle("priority", Integer.toString(0));
        provider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(importEnabled));
        provider.getConfig().putSingle(LDAPConstants.SYNC_REGISTRATIONS, Boolean.toString(syncRegistrations));

        return ApiUtil.getCreatedId(realm.admin().components().add(provider));
    }

    private String createFederatedUser(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        return ApiUtil.getCreatedId(realm.admin().users().create(user));
    }

    public static class FederatedUserServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .features(Feature.SCIM_API)
                    .dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
