package org.keycloak.tests.admin.realm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class RealmRolesCRUDTest extends AbstractRealmRolesTest {

    @InjectClient(ref = "client-a", attachTo = "client-a")
    ManagedClient clientA;

    @Test
    public void getRole() {
        RoleRepresentation role = managedRealm.admin().roles().get("role-a").toRepresentation();
        assertNotNull(role);
        assertEquals("role-a", role.getName());
        assertEquals("Role A", role.getDescription());
        assertEquals(Map.of("role-a-attr-key1", List.of("role-a-attr-val1")), role.getAttributes());
        assertFalse(role.isComposite());
    }

    @Test
    public void createRoleWithSameName() {
        Assertions.assertThrows(ClientErrorException.class, () -> {
            managedRealm.admin().roles().create(RoleConfigBuilder.create().name("role-a").build());
        });
    }

    @Test
    public void updateRole() {
        RoleRepresentation role = managedRealm.admin().roles().get("role-a").toRepresentation();

        role.setName("role-a-new");
        role.setDescription("Role A New");
        Map<String, List<String>> newAttributes = Collections.singletonMap("attrKeyNew", Collections.singletonList("attrValueNew"));
        role.setAttributes(newAttributes);

        managedRealm.admin().roles().get("role-a").update(role);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.roleResourcePath("role-a"), role, ResourceType.REALM_ROLE);

        role = managedRealm.admin().roles().get("role-a-new").toRepresentation();

        assertNotNull(role);
        assertEquals("role-a-new", role.getName());
        assertEquals("Role A New", role.getDescription());
        assertEquals(newAttributes, role.getAttributes());
        assertFalse(role.isComposite());

        managedRealm.cleanup().add(r -> r.roles().create(RoleConfigBuilder.create().name("role-a")
                .description("Role A").attributes(Map.of("role-a-attr-key1", List.of("role-a-attr-val1"))).build()));
        managedRealm.cleanup().add(r -> r.roles().get("role-a-new").remove());
    }

    @Test
    public void deleteRole() {
        assertNotNull(managedRealm.admin().roles().get("role-a"));
        managedRealm.admin().roles().deleteRole("role-a");
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleResourcePath("role-a"), ResourceType.REALM_ROLE);

        try {
            managedRealm.admin().roles().get("role-a").toRepresentation();
            fail("Expected 404");
        } catch (NotFoundException e) {
            // expected
        }
    }

    @Test
    public void composites() {
        assertFalse(managedRealm.admin().roles().get("role-a").toRepresentation().isComposite());
        assertEquals(0, managedRealm.admin().roles().get("role-a").getRoleComposites().size());

        List<RoleRepresentation> l = new LinkedList<>();
        l.add(RoleConfigBuilder.create().id(managedRealm.admin().roles().get("role-b").toRepresentation().getId()).build());
        l.add(RoleConfigBuilder.create().id(managedRealm.admin().clients().get(clientA.getId()).roles().get("role-c").toRepresentation().getId()).build());
        managedRealm.admin().roles().get("role-a").addComposites(l);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath("role-a"), l, ResourceType.REALM_ROLE);

        Set<RoleRepresentation> composites = managedRealm.admin().roles().get("role-a").getRoleComposites();

        assertTrue(managedRealm.admin().roles().get("role-a").toRepresentation().isComposite());
        Assert.assertNames(composites, "role-b", "role-c");

        Set<RoleRepresentation> realmComposites = managedRealm.admin().roles().get("role-a").getRealmRoleComposites();
        Assert.assertNames(realmComposites, "role-b");

        Set<RoleRepresentation> clientComposites = managedRealm.admin().roles().get("role-a").getClientRoleComposites(clientA.getId());
        Assert.assertNames(clientComposites, "role-c");

        managedRealm.admin().roles().get("role-a").deleteComposites(l);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleResourceCompositesPath("role-a"), l, ResourceType.REALM_ROLE);

        assertFalse(managedRealm.admin().roles().get("role-a").toRepresentation().isComposite());
        assertEquals(0, managedRealm.admin().roles().get("role-a").getRoleComposites().size());
    }

    @Test
    public void testDefaultRoles() {
        RoleResource defaultRole = managedRealm.admin().roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());

        UserRepresentation user = managedRealm.admin().users().search("test-role-member").get(0);

        UserResource userResource = managedRealm.admin().users().get(user.getId());
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listAll()), hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()));
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listEffective()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()),
                hasItem(Constants.OFFLINE_ACCESS_ROLE),
                hasItem(Constants.AUTHZ_UMA_AUTHORIZATION)
        ));

        defaultRole.addComposites(Collections.singletonList(managedRealm.admin().roles().get("role-a").toRepresentation()));

        userResource = managedRealm.admin().users().get(user.getId());
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listAll()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()),
                not(hasItem("role-a"))
        ));
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listEffective()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()),
                hasItem(Constants.OFFLINE_ACCESS_ROLE),
                hasItem(Constants.AUTHZ_UMA_AUTHORIZATION),
                hasItem("role-a")
        ));

        assertThat(userResource.roles().clientLevel(clientA.getId()).listAll(), empty());
        assertThat(userResource.roles().clientLevel(clientA.getId()).listEffective(), empty());

        defaultRole.addComposites(Collections.singletonList(managedRealm.admin().clients().get(clientA.getId()).roles().get("role-c").toRepresentation()));

        userResource = managedRealm.admin().users().get(user.getId());

        assertThat(userResource.roles().clientLevel(clientA.getId()).listAll(), empty());
        assertThat(convertRolesToNames(userResource.roles().clientLevel(clientA.getId()).listEffective()),
                hasItem("role-c")
        );
    }

    @Test
    public void testDeleteDefaultRole() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            managedRealm.admin().roles().deleteRole(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        });
    }

    private List<String> convertRolesToNames(List<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }
}
