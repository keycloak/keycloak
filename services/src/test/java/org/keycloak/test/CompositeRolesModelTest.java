package org.keycloak.test;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CompositeRolesModelTest extends AbstractKeycloakTest {

    public CompositeRolesModelTest(String providerId) {
        super(providerId);
    }

    @Before
    public void before() throws Exception {
        super.before();
        RealmManager manager = realmManager;
        RealmRepresentation rep = AbstractKeycloakServerTest.loadJson("testcomposites.json");
        RealmModel realm = manager.createRealm("Test", rep.getRealm());
        manager.importRealm(rep, realm);
    }

    @Test
    public void testAppComposites() {
        Set<RoleModel> requestedRoles = getRequestedRoles("APP_COMPOSITE_APPLICATION", "APP_COMPOSITE_USER");
        Assert.assertEquals(2, requestedRoles.size());

        RoleModel expectedRole1 = getRole("APP_ROLE_APPLICATION", "APP_ROLE_1");
        RoleModel expectedRole2 = getRole("realm", "REALM_ROLE_1");

        assertContains(requestedRoles, expectedRole1);
        assertContains(requestedRoles, expectedRole2);
    }

    // TODO: more tests...

    // Same algorithm as in TokenManager.createAccessCode
    private Set<RoleModel> getRequestedRoles(String applicationName, String username) {
        Set<RoleModel> requestedRoles = new HashSet<RoleModel>();

        RealmModel realm = realmManager.getRealm("Test");
        UserModel user = realm.getUser(username);
        ApplicationModel application = realm.getApplicationByName(applicationName);

        Set<RoleModel> roleMappings = realm.getRoleMappings(user);
        Set<RoleModel> scopeMappings = realm.getScopeMappings(application.getApplicationUser());
        Set<RoleModel> appRoles = application.getRoles();
        if (appRoles != null) scopeMappings.addAll(appRoles);

        for (RoleModel role : roleMappings) {
            if (role.getContainer().equals(application)) requestedRoles.add(role);
            for (RoleModel desiredRole : scopeMappings) {
                Set<RoleModel> visited = new HashSet<RoleModel>();
                applyScope(role, desiredRole, visited, requestedRoles);
            }
        }

        return requestedRoles;
    }

    private static void applyScope(RoleModel role, RoleModel scope, Set<RoleModel> visited, Set<RoleModel> requested) {
        if (visited.contains(scope)) return;
        visited.add(scope);
        if (role.hasRole(scope)) {
            requested.add(scope);
            return;
        }
        if (!scope.isComposite()) return;

        for (RoleModel contained : scope.getComposites()) {
            applyScope(role, contained, visited, requested);
        }
    }

    private RoleModel getRole(String appName, String roleName) {
        RealmModel realm = realmManager.getRealm("Test");
        if ("realm".equals(appName)) {
            return realm.getRole(roleName);
        }  else {
            return realm.getApplicationByName(appName).getRole(roleName);
        }
    }

    private void assertContains(Set<RoleModel> requestedRoles, RoleModel expectedRole) {
        Assert.assertTrue(requestedRoles.contains(expectedRole));

        // Check if requestedRole has correct role container
        for (RoleModel role : requestedRoles) {
            if (role.equals(expectedRole)) {
                Assert.assertEquals(role.getContainer(), expectedRole.getContainer());
            }
        }
    }
}
