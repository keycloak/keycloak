package org.keycloak.testsuite.model.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.federation.UserPropertyFileStorage;
import org.keycloak.testsuite.federation.UserPropertyFileStorage.UserPropertyFileStorageCall;
import org.keycloak.testsuite.federation.UserPropertyFileStorageFactory;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assume.assumeThat;

/**
 * @author mhajas
 */
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
@RequireProvider(value = UserStorageProvider.class, only = UserPropertyFileStorageFactory.PROVIDER_ID)
public class UserPaginationTest extends KeycloakModelTest {

    private String realmId;
    private String userFederationId1;
    private String userFederationId2;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();

        getParameters(UserStorageProviderModel.class).forEach(fs -> inComittedTransaction(session -> {
            assumeThat("Cannot handle more than 2 user federation provider", userFederationId2, Matchers.nullValue());

            fs.setParentId(realmId);

            ComponentModel res = realm.addComponentModel(fs);
            if (userFederationId1 == null) {
                userFederationId1 = res.getId();
            } else {
                userFederationId2 = res.getId();
            }

            log.infof("Added %s user federation provider: %s", fs.getName(), res.getId());
        }));
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testNoPaginationCalls() {
        List<UserModel> list = withRealm(realmId, (session, realm) ->
                session.users().searchForUserStream(realm, Map.of(UserModel.SEARCH, ""), 0, Constants.DEFAULT_MAX_RESULTS) // Default values used in UsersResource
                        .collect(Collectors.toList()));

        assertThat(list, hasSize(8));

        expectedStorageCalls(
                Collections.singletonList(new UserPropertyFileStorageCall(UserPropertyFileStorage.SEARCH_METHOD, 0, Constants.DEFAULT_MAX_RESULTS)),
                Collections.singletonList(new UserPropertyFileStorageCall(UserPropertyFileStorage.SEARCH_METHOD, 0, Constants.DEFAULT_MAX_RESULTS - 4))
        );
    }

    @Test
    public void testPaginationStarting0() {
        List<UserModel> list = withRealm(realmId, (session, realm) ->
                session.users().searchForUserStream(realm, Map.of(UserModel.SEARCH, ""), 0, 6)
                        .collect(Collectors.toList()));

        assertThat(list, hasSize(6));


        expectedStorageCalls(
                Collections.singletonList(new UserPropertyFileStorageCall(UserPropertyFileStorage.SEARCH_METHOD, 0, 6)),
                Collections.singletonList(new UserPropertyFileStorageCall(UserPropertyFileStorage.SEARCH_METHOD, 0, 2))
        );
    }

    @Test
    public void testPaginationFirstResultInFirstProvider() {
        List<UserModel> list = withRealm(realmId, (session, realm) ->
                session.users().searchForUserStream(realm, Map.of(UserModel.SEARCH, ""), 1, 6)
                        .collect(Collectors.toList()));
        assertThat(list, hasSize(6));

        expectedStorageCalls(
                Arrays.asList(new UserPropertyFileStorageCall(UserPropertyFileStorage.COUNT_SEARCH_METHOD, null, null), new UserPropertyFileStorageCall(UserPropertyFileStorage.SEARCH_METHOD, 1, 6)),
                Collections.singletonList(new UserPropertyFileStorageCall(UserPropertyFileStorage.SEARCH_METHOD, 0, 3))
        );
    }

    @Test
    public void testPaginationFirstResultIsExactlyTheAmountOfUsersInTheFirstProvider() {
        List<UserModel> list = withRealm(realmId, (session, realm) ->
                session.users().searchForUserStream(realm, Map.of(UserModel.SEARCH, ""), 4, 6)
                        .collect(Collectors.toList()));
        assertThat(list, hasSize(4));

        expectedStorageCalls(
                Collections.singletonList(new UserPropertyFileStorageCall(UserPropertyFileStorage.COUNT_SEARCH_METHOD, null, null)),
                Collections.singletonList(new UserPropertyFileStorageCall(UserPropertyFileStorage.SEARCH_METHOD, 0, 6))
        );
    }

    @Test
    public void testPaginationFirstResultIsInSecondProvider() {
        List<UserModel> list = withRealm(realmId, (session, realm) ->
                session.users().searchForUserStream(realm, Map.of(UserModel.SEARCH, ""), 5, 6)
                .collect(Collectors.toList()));

        assertThat(list, hasSize(3));

        expectedStorageCalls(
                Collections.singletonList(new UserPropertyFileStorageCall(UserPropertyFileStorage.COUNT_SEARCH_METHOD, null, null)),
                Arrays.asList(new UserPropertyFileStorageCall(UserPropertyFileStorage.COUNT_SEARCH_METHOD, null, null), new UserPropertyFileStorageCall(UserPropertyFileStorage.SEARCH_METHOD, 1, 6))
        );
    }

    private void expectedStorageCalls(final List<UserPropertyFileStorageCall> roCalls, final List<UserPropertyFileStorageCall> rwCalls) {
        assertThat(UserPropertyFileStorage.storageCalls.get(userFederationId1), hasSize(roCalls.size()));

        int i = 0;
        for (UserPropertyFileStorageCall call : roCalls) {
            assertThat(UserPropertyFileStorage.storageCalls.get(userFederationId1).get(i).getMethod(), equalTo(call.getMethod()));
            assertThat(UserPropertyFileStorage.storageCalls.get(userFederationId1).get(i).getFirst(), equalTo(call.getFirst()));
            assertThat(UserPropertyFileStorage.storageCalls.get(userFederationId1).get(i).getMax(), equalTo(call.getMax()));
            i++;
        }

        assertThat(UserPropertyFileStorage.storageCalls.get(userFederationId2), hasSize(rwCalls.size()));

        i = 0;
        for (UserPropertyFileStorageCall call : rwCalls) {
            assertThat(UserPropertyFileStorage.storageCalls.get(userFederationId2).get(i).getMethod(), equalTo(call.getMethod()));
            assertThat(UserPropertyFileStorage.storageCalls.get(userFederationId2).get(i).getFirst(), equalTo(call.getFirst()));
            assertThat(UserPropertyFileStorage.storageCalls.get(userFederationId2).get(i).getMax(), equalTo(call.getMax()));
            i++;
        }

        UserPropertyFileStorage.storageCalls.clear();
    }

}
