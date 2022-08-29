package org.keycloak.testsuite.model.role;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(RoleProvider.class)
public class RoleModelTest extends KeycloakModelTest {

    private String realmId;
    private String mainRoleId;
    private static List<String> rolesSubset;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("realm");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();

        createRoles(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @FunctionalInterface
    public interface GetResult {
        List<RoleModel> getResult(String search, Integer first, Integer max);
    }


    private void createRoles(KeycloakSession session, RealmModel realm) {
        RoleModel mainRole = session.roles().addRealmRole(realm, "main-role");
        mainRoleId = mainRole.getId();

        ClientModel clientModel = session.clients().addClient(realm, "client-with-roles");

        // Create 10 realm roles that are composites of main role
        rolesSubset = IntStream.range(0, 10)
                .boxed()
                .map(i -> session.roles().addRealmRole(realm, "main-role-composite-" + i))
                .peek(role -> role.setDescription("This is a description for " + role.getName() + " realm role."))
                .peek(mainRole::addCompositeRole)
                .map(RoleModel::getId)
                .collect(Collectors.toList());

        // Create 10 client roles that are composites of main role
        rolesSubset.addAll(IntStream.range(10, 20)
                .boxed()
                .map(i -> session.roles().addClientRole(clientModel, "main-role-composite-" + i))
                .peek(role -> role.setDescription("This is a description for " + role.getName() + " client role."))
                .peek(mainRole::addCompositeRole)
                .map(RoleModel::getId)
                .collect(Collectors.toList()));

        // add some additional roles that won't fulfill condition
        IntStream.range(0, 20)
                .forEach(i -> session.roles().addRealmRole(realm, "non-returned-role-" + i));
    }

    private List<RoleModel> getResult(String search, Integer first, Integer max) {
        return withRealm(realmId, (session, realm) -> session.roles().getRolesStream(realm, rolesSubset.stream(), search, first, max).collect(Collectors.toList()));
    }

    private RoleModel getMainRole() {
        return withRealm(realmId, (session, realm) -> session.roles().getRoleById(realm, mainRoleId));
    }

    private List<RoleModel> getModelResult(String search, Integer first, Integer max) {
        return withRealm(realmId, ((session, realm) -> session.roles().getRoleById(realm, mainRoleId).getCompositesStream(search, first, max).collect(Collectors.toList())));
    }

    @Test
    public void testRolesWithIdsQueries() {
        // should return all roles from the subset
        List<RoleModel> result = getResult(null, null, null);
        assertThat(result, hasSize(rolesSubset.size()));
        assertIndexValues(result, contains(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9));

        // test non-existing role ids
        result = withRealm(realmId, (session, realm) -> session.roles()
                .getRolesStream(realm, IntStream.range(0, 10).boxed()
                        .map(i -> UUID.randomUUID().toString()), null, null, null)
                .collect(Collectors.toList()));
        assertThat(result, is(empty()));

        // test mixed non-existing with existing
        result = withRealm(realmId, (session, realm) -> session.roles()
                .getRolesStream(realm, Stream.concat(rolesSubset.subList(0, 10).stream(),
                        IntStream.range(0, 10).boxed().map(i -> UUID.randomUUID().toString())), null, null, null)
                .collect(Collectors.toList()));
        assertThat(result, hasSize(10));
        assertIndexValues(result, contains(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void testCompositeRoles() {
        List<RoleModel> result = getModelResult(null, null, null);
        assertThat(result, hasSize(rolesSubset.size()));
        assertIndexValues(result, contains(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9));

        result = withRealm(realmId, (session, realm) -> session.roles().getRoleById(realm, mainRoleId).getCompositesStream().collect(Collectors.toList()));
        assertThat(result, hasSize(rolesSubset.size()));
        assertIndexValues(result, containsInAnyOrder(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void testRolesWithIdsSearchQueries() {
        testRolesWithIdsSearchQueries(this::getResult);
    }

    @Test
    public void testCompositeRolesSearchQueries() {
        testRolesWithIdsSearchQueries(this::getModelResult);
    }

    public void testRolesWithIdsSearchQueries(GetResult resultProvider) {
        // should return all roles from the subset
        List<RoleModel> result = resultProvider.getResult("", null, null);
        assertThat(result, hasSize(rolesSubset.size()));
        assertIndexValues(result, contains(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9));

        // test string that all contains
        result = resultProvider.getResult("role-composite", null, null);
        assertThat(result, hasSize(rolesSubset.size()));
        assertIndexValues(result, contains(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9));

        // test string that some contain
        result = resultProvider.getResult("role-composite-1", null, null);
        assertThat(result, hasSize(11));
        assertIndexValues(result, contains(1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19));

        // test string none contain
        result = resultProvider.getResult("nonsense-string", null, null);
        assertThat(result, is(empty()));
    }

    @Test
    public void testRolesWithIdsPaginationQueries() {
        testRolesWithIdsPaginationQueries(this::getResult);
    }

    @Test
    public void testCompositeRolesPaginationQueries() {
        testRolesWithIdsPaginationQueries(this::getResult);
    }

    public void testRolesWithIdsPaginationQueries(GetResult resultProvider) {
        // should return all roles from the subset
        List<RoleModel> result = resultProvider.getResult(null, null, rolesSubset.size());
        assertThat(result, hasSize(rolesSubset.size()));
        assertIndexValues(result, contains(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9));

        // test max parameter
        result = resultProvider.getResult(null, null, 5);
        assertThat(result, hasSize(5));
        assertIndexValues(result, contains(0, 1, 10, 11, 12));

        // test first parameter
        result = resultProvider.getResult(null, 10, null);
        assertThat(result, hasSize(rolesSubset.size() - 10));
        assertIndexValues(result, contains(18, 19, 2, 3, 4, 5, 6, 7, 8, 9));

        // test first and max
        result = resultProvider.getResult(null, 10, 5);
        assertThat(result, hasSize(5));
        assertIndexValues(result, contains(18, 19, 2, 3, 4));
    }

    @Test
    public void testRolesWithIdsPaginationSearchQueries() {
        testRolesWithIdsPaginationSearchQueries(this::getResult);
    }

    @Test
    public void testCompositeRolesPaginationSearchQueries() {
        testRolesWithIdsPaginationSearchQueries(this::getModelResult);
    }

    @Test
    public void testSearchRolesByDescription() {
        withRealm(realmId, (session, realm) -> {
            List<RoleModel> realmRolesByDescription = session.roles().searchForRolesStream(realm, "This is a", null, null).collect(Collectors.toList());
            assertThat(realmRolesByDescription, hasSize(10));
            realmRolesByDescription = session.roles().searchForRolesStream(realm, "realm role.", 5, null).collect(Collectors.toList());
            assertThat(realmRolesByDescription, hasSize(5));
            realmRolesByDescription = session.roles().searchForRolesStream(realm, "DESCRIPTION FOR", 3, 9).collect(Collectors.toList());
            assertThat(realmRolesByDescription, hasSize(7));

            ClientModel client = session.clients().getClientByClientId(realm, "client-with-roles");

            List<RoleModel> clientRolesByDescription = session.roles().searchForClientRolesStream(client, "this is a", 0, 10).collect(Collectors.toList());
            assertThat(clientRolesByDescription, hasSize(10));

            clientRolesByDescription = session.roles().searchForClientRolesStream(client, "role-composite-13 client role", null, null).collect(Collectors.toList());
            assertThat(clientRolesByDescription, hasSize(1));
            assertThat(clientRolesByDescription.get(0).getDescription(), is("This is a description for main-role-composite-13 client role."));

            return null;
        });
    }

    @Test
    public void testCompositeRolesUpdateOnChildRoleRemoval() {
        final AtomicReference<String> parentRealmRoleId = new AtomicReference<>();
        final AtomicReference<String> parentClientRoleId = new AtomicReference<>();

        final AtomicReference<String> childRealmRoleId = new AtomicReference<>();
        final AtomicReference<String> childClientRoleId = new AtomicReference<>();

        withRealm(realmId, (session, realm) -> {
            // Create realm role
            RoleModel parentRealmRole = session.roles().addRealmRole(realm, "parentRealmRole");
            parentRealmRoleId.set(parentRealmRole.getId());

            // Create client role
            ClientModel client = session.clients().addClient(realm,"clientWithRole");

            RoleModel parentClientRole = session.roles().addClientRole(client, "parentClientRole");
            parentClientRoleId.set(parentClientRole.getId());

            // Create realm child role
            RoleModel childRealmRole = session.roles().addRealmRole(realm, "childRealmRole");
            childRealmRoleId.set(childRealmRole.getId());

            RoleModel childClientRole = session.roles().addClientRole(client, "childClientRole");
            childClientRoleId.set(childClientRole.getId());

            // Add composites
            parentRealmRole.addCompositeRole(childRealmRole);
            parentRealmRole.addCompositeRole(childClientRole);

            parentClientRole.addCompositeRole(childRealmRole);
            parentClientRole.addCompositeRole(childClientRole);
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RoleModel parentRealmRole = session.roles().getRoleById(realm, parentRealmRoleId.get());
            RoleModel parentClientRole = session.roles().getRoleById(realm, parentClientRoleId.get());
            assertThat(parentRealmRole.getCompositesStream().collect(Collectors.toSet()), hasSize(2));
            assertThat(parentClientRole.getCompositesStream().collect(Collectors.toSet()), hasSize(2));

            session.roles().removeRole(session.roles().getRoleById(realm, childRealmRoleId.get()));
            session.roles().removeRole(session.roles().getRoleById(realm, childClientRoleId.get()));
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RoleModel parentRealmRole = session.roles().getRoleById(realm, parentRealmRoleId.get());
            RoleModel parentClientRole = session.roles().getRoleById(realm, parentClientRoleId.get());
            assertThat(parentRealmRole.getCompositesStream().collect(Collectors.toSet()), empty());
            assertThat(parentClientRole.getCompositesStream().collect(Collectors.toSet()), empty());
            return null;
        });
    }

    public void testRolesWithIdsPaginationSearchQueries(GetResult resultProvider) {
        // test all parameters together
        List<RoleModel> result = resultProvider.getResult("1", 4, 3);
        assertThat(result, hasSize(3));
        assertIndexValues(result, contains(13, 14, 15));
    }

    private void assertIndexValues(List<RoleModel> roles, Matcher<? super Collection<? extends Integer>> matcher) {
        assertThat(roles.stream().map(RoleModel::getName).map(s -> s.substring("main-role-composite-".length())).map(Integer::parseInt).collect(Collectors.toList()), matcher);
    }
}
