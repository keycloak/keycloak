package org.keycloak.testsuite.federation.storage;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.concurrency.AbstractConcurrencyTest;
import org.keycloak.testsuite.federation.UserMapStorage;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.BeforeClass;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractUserStorageDirtyDeletionTest extends AbstractConcurrencyTest {

    protected abstract ComponentRepresentation getFederationProvider();

    private static final int NUM_USERS = 200;
    private static final int REMOVED_USERS_COUNT = NUM_USERS / 2;

    private List<Creator<UserResource>> createdUsers;

    @BeforeClass
    public static void checkNotMapStorage() {
        ProfileAssume.assumeFeatureDisabled(Feature.MAP_STORAGE);
    }

    public static void remove20UsersFromStorageProvider(KeycloakSession session) {
        assertThat(REMOVED_USERS_COUNT, Matchers.lessThan(NUM_USERS));
        final RealmModel realm = session.realms().getRealmByName(TEST_REALM_NAME);
        UserStorageProvidersTestUtils.getEnabledStorageProviders(session, realm, UserMapStorage.class)
          .forEachOrdered((UserMapStorage userMapStorage) -> {
              Set<String> users = new HashSet<>(userMapStorage.getUsernames());
              users.stream()
                .sorted()
                .limit(REMOVED_USERS_COUNT)
                .forEach(userMapStorage::removeUserByName);
          });
    }

    protected ComponentRepresentation getFederationProvider(UserStorageProvider.EditMode editMode, boolean importEnabled) {
        ComponentRepresentation provider = new ComponentRepresentation();

        provider.setName(getClass().getSimpleName());
        provider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle("priority", Integer.toString(0));
        provider.getConfig().putSingle(LDAPConstants.EDIT_MODE, editMode.name());
        provider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(importEnabled));

        return provider;
    }

    private Creator<UserResource> addFederatedUser(int sequenceId) {
        try {
            final Creator<UserResource> creator = Creator.create(testRealm(), UserBuilder.create().username("test-user-" + sequenceId).build());
            return creator;
        } catch (Throwable ex) {
            throw new RuntimeException("Failed for test-user-" + sequenceId, ex);
        }
    }

    private List<Creator<UserResource>> createUsers() {
        log.debugf("Adding users test-user-%d .. test-user-%d", 0, NUM_USERS);
        return IntStream.range(0, NUM_USERS)
          .mapToObj(this::addFederatedUser)
          .collect(Collectors.toList());
    }

    @Before
    public void before() {
        getCleanup().addCleanup(Creator.create(testRealm(), getFederationProvider()));

        // create all users
        createdUsers = createUsers();
        createdUsers.forEach(getCleanup()::addCleanup);
    }

    @Test
    public void testConcurrentDelete() {
        // Once all users are created, remove them in parallel
        createdUsers.stream().parallel().forEach(Creator::close);
    }

    @Test
    public void testConcurrentDeleteCachedUsers() {
        // Cache the users in the local cache
        createdUsers.stream().parallel().map(Creator::resource).forEach(UserResource::toRepresentation);

        // Remove them in parallel
        createdUsers.stream().parallel().forEach(Creator::close);
    }

    @Test
    public void testMembersWhenCachedUsersRemovedFromBackend() {
        try (Creator<GroupResource> group = Creator.create(testRealm(), GroupBuilder.create().name("g").build())) {
            // Cache the users in the local server cache and add to a group
            createdUsers.stream().parallel().map(Creator::resource).forEach(r -> {
                r.joinGroup(group.id());
                r.toRepresentation();
            });
            assertThat(group.resource().members(0, 2 * NUM_USERS), hasSize(NUM_USERS));

            // Remove the users from underlying provider
            testingClient.server().run(AbstractUserStorageDirtyDeletionTest::remove20UsersFromStorageProvider);

            IntStream.range(0, 7).parallel()
              .mapToObj(i -> group.resource().members(0, 2 * NUM_USERS))
              .forEach(members -> assertThat(members, hasSize(NUM_USERS - REMOVED_USERS_COUNT)));
            
            assertThat(group.resource().members(0, 5), hasSize(5));

            // Remove them in parallel
            createdUsers.stream().parallel().forEach(Creator::close);

            assertThat(group.resource().members(0, 5), hasSize(0));

        }
    }

}
