package org.keycloak.tests.admin.realm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmRolesSearchTest extends AbstractRealmRolesTest {

    // issue #9587
    @Test
    public void testSearchForRealmRoles() {
        managedRealm.admin().roles().list("role-", true).forEach(role -> assertThat("There is client role '" + role.getName() + "' among realm roles.", role.getClientRole(), is(false)));
    }

    @Test
    public void testSearchForRoles() {

        for(int i = 0; i<15; i++) {
            String roleName = "testrole"+i;
            RoleRepresentation role = makeRole(roleName);
            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        String roleNameA = "abcdefg";
        RoleRepresentation roleA = makeRole(roleNameA);
        managedRealm.admin().roles().create(roleA);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleNameA), roleA, ResourceType.REALM_ROLE);

        String roleNameB = "defghij";
        RoleRepresentation roleB = makeRole(roleNameB);
        managedRealm.admin().roles().create(roleB);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleNameB), roleB, ResourceType.REALM_ROLE);

        List<RoleRepresentation> resultSearch = managedRealm.admin().roles().list("defg", -1, -1);
        assertEquals(2,resultSearch.size());

        List<RoleRepresentation> resultSearch2 = managedRealm.admin().roles().list("testrole", -1, -1);
        assertEquals(15,resultSearch2.size());

        List<RoleRepresentation> resultSearchPagination = managedRealm.admin().roles().list("testrole", 1, 5);
        assertEquals(5,resultSearchPagination.size());
    }

    @Test
    public void testPaginationRoles() {

        for(int i = 0; i<15; i++) {
            String roleName = "role"+i;
            RoleRepresentation role = makeRole(roleName);
            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> resultSearchPagination = managedRealm.admin().roles().list(1, 5);
        assertEquals(5,resultSearchPagination.size());

        List<RoleRepresentation> resultSearchPagination2 = managedRealm.admin().roles().list(5, 5);
        assertEquals(5,resultSearchPagination2.size());

        List<RoleRepresentation> resultSearchPagination3 = managedRealm.admin().roles().list(1, 5);
        assertEquals(5,resultSearchPagination3.size());

        List<RoleRepresentation> resultSearchPaginationIncoherentParams = managedRealm.admin().roles().list(1, null);
        assertTrue(resultSearchPaginationIncoherentParams.size() > 15);
    }

    @Test
    public void testPaginationRolesCache() {

        for(int i = 0; i<5; i++) {
            String roleName = "paginaterole"+i;
            RoleRepresentation role = makeRole(roleName);
            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> resultBeforeAddingRoleToTestCache = managedRealm.admin().roles().list(1, 1000);

        // after a first call which init the cache, we add a new role to see if the result change

        RoleRepresentation role = makeRole("anewrole");
        managedRealm.admin().roles().create(role);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath("anewrole"), role, ResourceType.REALM_ROLE);

        List<RoleRepresentation> resultafterAddingRoleToTestCache = managedRealm.admin().roles().list(1, 1000);

        assertEquals(resultBeforeAddingRoleToTestCache.size()+1, resultafterAddingRoleToTestCache.size());
    }

    @Test
    public void getRolesWithFullRepresentation() {
        for(int i = 0; i<5; i++) {
            String roleName = "attributesrole"+i;
            RoleRepresentation role = makeRole(roleName);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("attribute1", Arrays.asList("value1","value2"));
            role.setAttributes(attributes);

            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> roles = managedRealm.admin().roles().list("attributesrole", false);
        assertTrue(roles.get(0).getAttributes().containsKey("attribute1"));
    }

    @Test
    public void getRolesWithBriefRepresentation() {
        for(int i = 0; i<5; i++) {
            String roleName = "attributesrolebrief"+i;
            RoleRepresentation role = makeRole(roleName);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("attribute1", Arrays.asList("value1","value2"));
            role.setAttributes(attributes);

            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> roles = managedRealm.admin().roles().list("attributesrolebrief", true);
        assertNull(roles.get(0).getAttributes());
    }

    private static RoleRepresentation makeRole(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }
}
