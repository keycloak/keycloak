package org.keycloak.tests.workflow.step;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.workflow.JoinGroupStepProvider;
import org.keycloak.models.workflow.JoinGroupStepProviderFactory;
import org.keycloak.models.workflow.LeaveGroupStepProvider;
import org.keycloak.models.workflow.LeaveGroupStepProviderFactory;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserGroupMembershipRemovedWorkflowEventFactory;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Tests the execution of the 'join-group' and 'leave-group' workflow steps.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class GroupBasedStepTest extends AbstractWorkflowTest {

    @BeforeEach
    public void setupRoles() {
        RealmResource admin = managedRealm.admin();
        GroupsResource groups = admin.groups();
        for (Entry<String, List<?>> group : Map.of("a", List.of("a1"), "b", List.of("b1", "b2"), "c", List.of()).entrySet()) {
            GroupRepresentation rep = GroupConfigBuilder.create().name(group.getKey()).build();
            try (Response response = groups.add(rep)) {
                rep.setId(ApiUtil.getCreatedId(response));
            }
            for (Object subGroup : group.getValue()) {
                groups.group(rep.getId()).subGroup(GroupConfigBuilder.create().name(subGroup.toString()).build()).close();
            }
        }
    }

    @Test
    public void testJoinGroup() {
        List<String> expectedGroups = List.of("/a", "/b/b1", "c");

        create(WorkflowRepresentation.withName("join-group")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(JoinGroupStepProviderFactory.ID)
                                .withConfig(JoinGroupStepProvider.CONFIG_GROUP, expectedGroups.toArray(new String[0]))
                                .build()
                ).build());

        UserResource user = getUserResource(UserConfigBuilder.create().username("myuser").build());

        Awaitility.await()
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    GroupsResource groups = managedRealm.admin().groups();
                    var actualGroups = user.groups().stream().map(g -> groups.group(g.getId()).toRepresentation().getPath()).toList();
                    assertThat(actualGroups, containsInAnyOrder(expectedGroups.stream().map(n -> {
                        if (!n.startsWith("/")) {
                            return "/" + n;
                        }
                        return n;
                    }).toArray(String[]::new)));
                });
    }

    @Test
    public void testLeaveGroup() {
        UserResource user = getUserResource(UserConfigBuilder.create()
                .username("myuser")
                .build());
        joinGroup(user, "a", "/a/a1", "b/b1", "b/b2", "/c");

        create(WorkflowRepresentation.withName("leave-group")
                .onEvent(UserGroupMembershipRemovedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(LeaveGroupStepProviderFactory.ID)
                                .withConfig(LeaveGroupStepProvider.CONFIG_GROUP, "a", "/b/b1", "/b/b2")
                                .build()
                ).build());

        user.leaveGroup(managedRealm.admin().groups().groups("c", true, null, null, true).get(0).getId());

        Awaitility.await()
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    GroupsResource groups = managedRealm.admin().groups();
                    var actualGroups = user.groups().stream().map(g -> groups.group(g.getId()).toRepresentation().getPath()).toList();
                    assertThat(actualGroups, containsInAnyOrder("/a/a1"));
                });
    }

    private UserResource getUserResource(UserRepresentation user) {
        UsersResource users = managedRealm.admin().users();

        try (Response response = users.create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        return users.get(user.getId());
    }

    private void joinGroup(UserResource user, String... groups) {
        RealmResource admin = managedRealm.admin();

        for (String name : groups) {
            String[] parts = name.split("/");

            if (parts.length == 1) {
                GroupsResource groupsResource = admin.groups();
                GroupRepresentation group = groupsResource.groups(parts[0], true, null, null, true).get(0);
                user.joinGroup(group.getId());
            } else {
                GroupsResource groupsResource = admin.groups();
                GroupRepresentation group = null;

                for (String part : parts) {
                    if (part.isEmpty()) {
                        continue;
                    }
                    if (group == null) {
                        group = groupsResource.groups(part, true, null, null, true).get(0);
                    } else {
                        group = groupsResource.group(group.getId()).getSubGroups(part, true, null, null, true).get(0);
                    }
                }

                if (group != null) {
                    user.joinGroup(group.getId());
                }
            }

        }
    }
}
