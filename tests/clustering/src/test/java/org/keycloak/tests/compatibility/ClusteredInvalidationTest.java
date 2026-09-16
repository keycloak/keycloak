package org.keycloak.tests.compatibility;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class ClusteredInvalidationTest {

    @InjectRealm(config = OrganizationGroupsRealmConfig.class)
    ManagedRealm realm;

    @InjectLoadBalancer
    LoadBalancer loadBalancer;

    // we cannot reuse the database between tests with mix-cluster; Keycloak won't start.
    @InjectTestDatabase(lifecycle = LifeCycle.CLASS)
    TestDatabase database;

    @AfterEach
    public void cleanup() {
        loadBalancer.node(0);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0"})
    public void testRealmInvalidation(int writer, int reader) {
        // force caching in both nodes
        loadBalancer.node(writer);
        var writerTimeout = Objects.requireNonNullElse(realm.admin().toRepresentation().getClientSessionIdleTimeout(), 0);

        loadBalancer.node(reader);
        var readerTimeout = Objects.requireNonNullElse(realm.admin().toRepresentation().getClientSessionIdleTimeout(), 0);

        assertEquals(writerTimeout, readerTimeout);

        var newTimeout = writerTimeout + 100;

        // write in one of the nodes
        loadBalancer.node(writer);
        realm.updateWithCleanup(r -> r.clientSessionIdleTimeout(newTimeout));

        // should be visible immediately in the writer
        assertEquals(newTimeout, realm.admin().toRepresentation().getClientSessionIdleTimeout());

        loadBalancer.node(reader);
        // Should be visible immediately in the reader.
        // For clusterless the update waits for the other node to consume the event.
        // For non-clusterless, the event is transmitted via the work cache immediately.
        assertEquals(newTimeout, realm.admin().toRepresentation().getClientSessionIdleTimeout());
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0"})
    public void testOrganizationGroupRoleInvalidation(int writer, int reader) {
        loadBalancer.node(writer);
        String suffix = writer + "-" + reader;
        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setName("cluster-group-role-org-" + suffix);
        organization.setAlias("cluster-group-role-" + suffix);
        try (Response response = realm.admin().organizations().create(organization)) {
            organization.setId(ApiUtil.getCreatedId(response));
        }
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        GroupRepresentation hiddenBefore = createOrganizationGroup(organizationResource, "hidden-before-" + suffix);
        GroupRepresentation hiddenAfter = createOrganizationGroup(organizationResource, "hidden-after-" + suffix);
        GroupRepresentation publicBefore = createRealmGroup("public-before-" + suffix);
        GroupRepresentation publicAfter = createRealmGroup("public-after-" + suffix);

        RoleRepresentation roleBefore = createRealmRole("role-before-" + suffix);
        RoleRepresentation roleAfter = createRealmRole("role-after-" + suffix);
        organizationResource.groups().group(hiddenBefore.getId()).roles().realmLevel().add(List.of(roleBefore));

        UserRepresentation user = new UserRepresentation();
        user.setUsername("cluster-group-role-user-" + suffix);
        user.setEnabled(true);
        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        organizationResource.members().addMember(user.getId()).close();
        organizationResource.groups().group(hiddenBefore.getId()).addMember(user.getId());
        realm.admin().users().get(user.getId()).joinGroup(publicBefore.getId());

        // Warm the user, group and role views on both nodes before the write.
        loadBalancer.node(writer);
        assertOrganizationGroupRoleState(organizationResource, user, hiddenBefore, hiddenAfter, publicBefore,
                publicAfter, roleBefore, roleAfter, false);
        assertDefaultRoleMembership(organizationResource, user, true);
        loadBalancer.node(reader);
        assertOrganizationGroupRoleState(organizationResource, user, hiddenBefore, hiddenAfter, publicBefore,
                publicAfter, roleBefore, roleAfter, false);
        assertDefaultRoleMembership(organizationResource, user, true);

        loadBalancer.node(writer);
        organizationResource.groups().group(hiddenBefore.getId()).removeMember(user.getId());
        organizationResource.groups().group(hiddenAfter.getId()).addMember(user.getId());
        realm.admin().users().get(user.getId()).leaveGroup(publicBefore.getId());
        realm.admin().users().get(user.getId()).joinGroup(publicAfter.getId());
        organizationResource.groups().group(hiddenBefore.getId()).roles().realmLevel().remove(List.of(roleBefore));
        organizationResource.groups().group(hiddenAfter.getId()).roles().realmLevel().add(List.of(roleAfter));
        assertOrganizationGroupRoleState(organizationResource, user, hiddenBefore, hiddenAfter, publicBefore,
                publicAfter, roleBefore, roleAfter, true);
        assertDefaultRoleMembership(organizationResource, user, true);

        loadBalancer.node(reader);
        assertOrganizationGroupRoleState(organizationResource, user, hiddenBefore, hiddenAfter, publicBefore,
                publicAfter, roleBefore, roleAfter, true);
        assertDefaultRoleMembership(organizationResource, user, true);

        loadBalancer.node(writer);
        organizationResource.members().removeMember(user.getId()).close();
        assertDefaultRoleMembership(organizationResource, user, false);

        loadBalancer.node(reader);
        assertDefaultRoleMembership(organizationResource, user, false);
        organizationResource.members().addMember(user.getId()).close();
        assertDefaultRoleMembership(organizationResource, user, true);

        loadBalancer.node(writer);
        assertDefaultRoleMembership(organizationResource, user, true);
    }

    private GroupRepresentation createOrganizationGroup(OrganizationResource organization, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = organization.groups().addTopLevelGroup(group)) {
            group.setId(ApiUtil.getCreatedId(response));
        }
        return group;
    }

    private GroupRepresentation createRealmGroup(String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = realm.admin().groups().add(group)) {
            group.setId(ApiUtil.getCreatedId(response));
        }
        return group;
    }

    private RoleRepresentation createRealmRole(String name) {
        realm.admin().roles().create(new RoleRepresentation(name, null, false));
        return realm.admin().roles().get(name).toRepresentation();
    }

    private void assertOrganizationGroupRoleState(OrganizationResource organization, UserRepresentation user,
                                                  GroupRepresentation hiddenBefore, GroupRepresentation hiddenAfter,
                                                  GroupRepresentation publicBefore, GroupRepresentation publicAfter,
                                                  RoleRepresentation roleBefore, RoleRepresentation roleAfter,
                                                  boolean after) {
        Set<String> publicGroups = realm.admin().users().get(user.getId()).groups().stream()
                .map(GroupRepresentation::getId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(after ? publicAfter.getId() : publicBefore.getId()), publicGroups);

        Set<String> hiddenBeforeMembers = organization.groups().group(hiddenBefore.getId())
                .getMembers(null, null, false).stream()
                .map(MemberRepresentation::getId)
                .collect(Collectors.toSet());
        Set<String> hiddenAfterMembers = organization.groups().group(hiddenAfter.getId())
                .getMembers(null, null, false).stream()
                .map(MemberRepresentation::getId)
                .collect(Collectors.toSet());
        assertEquals(!after, hiddenBeforeMembers.contains(user.getId()));
        assertEquals(after, hiddenAfterMembers.contains(user.getId()));

        Set<String> effectiveRoles = realm.admin().users().get(user.getId()).roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        assertEquals(!after, effectiveRoles.contains(roleBefore.getName()));
        assertEquals(after, effectiveRoles.contains(roleAfter.getName()));
    }

    private void assertDefaultRoleMembership(OrganizationResource organization, UserRepresentation user,
                                             boolean member) {
        Set<String> roleMembers = organization.roles().getDefault().getUserMembers().stream()
                .map(UserRepresentation::getId)
                .collect(Collectors.toSet());
        assertEquals(member, roleMembers.contains(user.getId()));

    }

    public static final class OrganizationGroupsRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
