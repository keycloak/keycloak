package org.keycloak.tests.admin.finegrainedadminv1;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@KeycloakIntegrationTest(config = AbstractFineGrainedAdminTest.FineGrainedAdminServerConf.class)
public class FineGrainedPermissionsV1UsersTest extends AbstractFineGrainedAdminTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private void assertSearchMatchesCount(RealmResource realmResource, String search, Integer expectedCount) {
        Integer count = realmResource.users().count(search);
        assertThat(count, is(expectedCount));
        assertThat(realmResource.users().search(search, null, null), hasSize(count));
    }


    @Test
    public void countUsersWithGroupViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(true);
        assertThat(testRealmResource.users().count(), is(3));
    }

    @Test
    public void countUsersBySearchWithGroupViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(true);
        //search all
        assertSearchMatchesCount(testRealmResource, "user", 3);
        //search first name
        assertSearchMatchesCount(testRealmResource, "*FirstName*", 3);
        assertSearchMatchesCount(testRealmResource, "user2FirstName", 1);
        //search last name
        assertSearchMatchesCount(testRealmResource, "*LastName*", 3);
        assertSearchMatchesCount(testRealmResource, "user2LastName", 1);
        //search in email
        assertSearchMatchesCount(testRealmResource, "*@example.com*", 3);
        assertSearchMatchesCount(testRealmResource, "user1@example.com", 1);
        //search for something not existing
        assertSearchMatchesCount(testRealmResource, "notExisting", 0);
        //search for empty string
        assertSearchMatchesCount(testRealmResource, "", 3);
        //search not specified (defaults to simply /count)
        assertSearchMatchesCount(testRealmResource, null, 3);
    }

    @Test
    public void countUsersByFiltersWithGroupViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(true);
        //search username
        assertThat(testRealmResource.users().count(null, null, null, "user"), equalTo(testRealmResource.users().search("user", null, null, null, null, null).size()));
        assertThat(testRealmResource.users().count(null, null, null, "user"), is(3));
        assertThat(testRealmResource.users().count(null, null, null, "user1"), is(1));
        assertThat(testRealmResource.users().count(null, null, null, "notExisting"), is(0));
        assertThat(testRealmResource.users().count(null, null, null, ""), is(3));
        //search first name
        assertThat(testRealmResource.users().count(null, "FirstName", null, null), is(3));
        assertThat(testRealmResource.users().count(null, "user2FirstName", null, null), is(1));
        assertThat(testRealmResource.users().count(null, "notExisting", null, null), is(0));
        assertThat(testRealmResource.users().count(null, "", null, null), is(3));
        //search last name
        assertThat(testRealmResource.users().count("LastName", null, null, null), is(3));
        assertThat(testRealmResource.users().count("user2LastName", null, null, null), is(1));
        assertThat(testRealmResource.users().count("notExisting", null, null, null), is(0));
        assertThat(testRealmResource.users().count("", null, null, null), is(3));
        //search in email
        assertThat(testRealmResource.users().count(null, null, "@example.com", null), is(3));
        assertThat(testRealmResource.users().count(null, null, "user1@example.com", null), is(1));
        assertThat(testRealmResource.users().count(null, null, "user1@test.com", null), is(0));
        assertThat(testRealmResource.users().count(null, null, "", null), is(3));
        //search for combinations
        assertThat(testRealmResource.users().count("LastName", "FirstName", null, null), is(3));
        assertThat(testRealmResource.users().count("user1LastName", "FirstName", null, null), is(1));
        assertThat(testRealmResource.users().count("user1LastName", "", null, null), is(1));
        assertThat(testRealmResource.users().count("LastName", "", null, null), is(3));
        assertThat(testRealmResource.users().count("LastName", "", null, null), is(3));
        assertThat(testRealmResource.users().count(null, null, "@example.com", "user"), is(3));
        //search not specified (defaults to simply /count)
        assertThat(testRealmResource.users().count(null, null, null, null), is(3));
        assertThat(testRealmResource.users().count("", "", "", ""), is(3));
    }

    @Test
    public void countUsersWithNoViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(false);
        assertThat(testRealmResource.users().count(), is(0));
    }

    @Test
    public void countUsersBySearchWithNoViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(false);
        //search all
        assertSearchMatchesCount(testRealmResource, "user", 0);
        //search first name
        assertSearchMatchesCount(testRealmResource, "FirstName", 0);
        assertSearchMatchesCount(testRealmResource, "user2FirstName", 0);
        //search last name
        assertSearchMatchesCount(testRealmResource, "LastName", 0);
        assertSearchMatchesCount(testRealmResource, "user2LastName", 0);
        //search in email
        assertSearchMatchesCount(testRealmResource, "@example.com", 0);
        assertSearchMatchesCount(testRealmResource, "user1@example.com", 0);
        //search for something not existing
        assertSearchMatchesCount(testRealmResource, "notExisting", 0);
        //search for empty string
        assertSearchMatchesCount(testRealmResource, "", 0);
        //search not specified (defaults to simply /count)
        assertSearchMatchesCount(testRealmResource, null, 0);
    }

    @Test
    public void countUsersByFiltersWithNoViewPermission() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        RealmResource testRealmResource = setupTestEnvironmentWithPermissions(false);
        //search username
        assertThat(testRealmResource.users().count(null, null, null, "user"), is(0));
        assertThat(testRealmResource.users().count(null, null, null, "user1"), is(0));
        assertThat(testRealmResource.users().count(null, null, null, "notExisting"), is(0));
        assertThat(testRealmResource.users().count(null, null, null, ""), is(0));
        //search first name
        assertThat(testRealmResource.users().count(null, "FirstName", null, null), is(0));
        assertThat(testRealmResource.users().count(null, "user2FirstName", null, null), is(0));
        assertThat(testRealmResource.users().count(null, "notExisting", null, null), is(0));
        assertThat(testRealmResource.users().count(null, "", null, null), is(0));
        //search last name
        assertThat(testRealmResource.users().count("LastName", null, null, null), is(0));
        assertThat(testRealmResource.users().count("user2LastName", null, null, null), is(0));
        assertThat(testRealmResource.users().count("notExisting", null, null, null), is(0));
        assertThat(testRealmResource.users().count("", null, null, null), is(0));
        //search in email
        assertThat(testRealmResource.users().count(null, null, "@example.com", null), is(0));
        assertThat(testRealmResource.users().count(null, null, "user1@example.com", null), is(0));
        assertThat(testRealmResource.users().count(null, null, "user1@test.com", null), is(0));
        assertThat(testRealmResource.users().count(null, null, "", null), is(0));
        //search for combinations
        assertThat(testRealmResource.users().count("LastName", "FirstName", null, null), is(0));
        assertThat(testRealmResource.users().count("user1LastName", "FirstName", null, null), is(0));
        assertThat(testRealmResource.users().count("user1LastName", "", null, null), is(0));
        assertThat(testRealmResource.users().count("LastName", "", null, null), is(0));
        assertThat(testRealmResource.users().count("LastName", "", null, null), is(0));
        assertThat(testRealmResource.users().count(null, null, "@example.com", "user"), is(0));
        //search not specified (defaults to simply /count)
        assertThat(testRealmResource.users().count(null, null, null, null), is(0));
        assertThat(testRealmResource.users().count("", "", "", ""), is(0));
    }

    private RealmResource setupTestEnvironmentWithPermissions(boolean grp1ViewPermissions) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        UserRepresentation user = UserConfigBuilder.create()
                .username("test-user")
                .password("password")
                .name("a", "b")
                .email("c@d.com")
                .enabled(true)
                .build();
        String testUserId = ApiUtil.getCreatedId(realm.admin().users().create(user));
        //assign 'query-users' role to test user
        ClientRepresentation clientRepresentation = realm.admin().clients().findByClientId("realm-management").get(0);
        String realmManagementId = clientRepresentation.getId();
        RoleRepresentation roleRepresentation = realm.admin().clients().get(realmManagementId).roles().get("query-users").toRepresentation();
        realm.admin().users().get(testUserId).roles().clientLevel(realmManagementId).add(Collections.singletonList(roleRepresentation));

        //create test users and groups
        List<GroupRepresentation> groups = setupUsersInGroupsWithPermissions();

        if (grp1ViewPermissions) {
            AuthorizationResource authorizationResource = realm.admin().clients().get(realmManagementId).authorization();
            //create a user policy for the test user
            UserPolicyRepresentation policy = new UserPolicyRepresentation();
            String policyName = "test-policy";
            policy.setName(policyName);
            policy.setUsers(Collections.singleton(testUserId));
            authorizationResource.policies().user().create(policy).close();
            PolicyRepresentation policyRepresentation = authorizationResource.policies().findByName(policyName);
            //add the policy to grp1
            Optional<GroupRepresentation> optional = groups.stream().filter(g -> g.getName().equals("grp1")).findFirst();
            assertThat(optional.isPresent(), is(true));
            GroupRepresentation grp1 = optional.get();
            ScopePermissionRepresentation scopePermissionRepresentation = authorizationResource.permissions().scope().findByName("view.members.permission.group." + grp1.getId());
            scopePermissionRepresentation.setPolicies(Collections.singleton(policyRepresentation.getId()));
            scopePermissionRepresentation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
            authorizationResource.permissions().scope().findById(scopePermissionRepresentation.getId()).update(scopePermissionRepresentation);
        }

        Keycloak testUserClient = KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBaseUrl().toString())
                .realm(realm.getName())
                .username("test-user")
                .password("password")
                .clientId("admin-cli")
                .clientSecret("")
                .build();

        return testUserClient.realm(realm.getCreatedRepresentation().getRealm());
    }

    private List<GroupRepresentation> setupUsersInGroupsWithPermissions() {
        //create two groups
        GroupRepresentation grp1 = createGroupWithPermissions("grp1");
        GroupRepresentation grp2 = createGroupWithPermissions("grp2");
        //create test users
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .enabled(true)
                .build();
        String user1Id = ApiUtil.getCreatedId(realm.admin().users().create(user1));

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        String user2Id = ApiUtil.getCreatedId(realm.admin().users().create(user2));

        UserRepresentation user3 = UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .enabled(true)
                .build();
        String user3Id = ApiUtil.getCreatedId(realm.admin().users().create(user3));

        UserRepresentation user4 = UserConfigBuilder.create()
                .username("user4")
                .password("password")
                .name("user4FirstName", "user4LastName")
                .email("user4@example.com")
                .enabled(true)
                .build();
        String user4Id = ApiUtil.getCreatedId(realm.admin().users().create(user4));

        //add users to groups
        realm.admin().users().get(user1Id).joinGroup(grp1.getId());
        realm.admin().users().get(user2Id).joinGroup(grp1.getId());
        realm.admin().users().get(user3Id).joinGroup(grp1.getId());
        realm.admin().users().get(user4Id).joinGroup(grp2.getId());

        List<GroupRepresentation> groups = new ArrayList<>();
        groups.add(grp1);
        groups.add(grp2);

        return groups;
    }

    private GroupRepresentation createGroupWithPermissions(String name) {
        GroupRepresentation grp = new GroupRepresentation();
        grp.setName(name);
        realm.admin().groups().add(grp);
        Optional<GroupRepresentation> optional = realm.admin().groups().groups().stream().filter(g -> g.getName().equals(name)).findFirst();
        assertThat(optional.isPresent(), is(true));
        grp = optional.get();
        String id = grp.getId();
        //enable the permissions
        realm.admin().groups().group(id).setPermissions(new ManagementPermissionRepresentation(true));
        assertThat(realm.admin().groups().group(id).getPermissions().isEnabled(), is(true));

        return grp;
    }
}
