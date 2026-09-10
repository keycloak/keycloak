package org.keycloak.tests.scim.tck;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.user.User;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that verify SCIM search behavior with non-imported federated users.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/51343">Issue 51343</a>
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

    private String registerFederationProvider() {
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("test-user-federation");
        provider.setProviderId(PROVIDER_ID);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<String, String>());
        provider.getConfig().putSingle("priority", Integer.toString(0));
        provider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));

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
