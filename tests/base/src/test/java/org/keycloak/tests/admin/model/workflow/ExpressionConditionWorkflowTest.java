package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class ExpressionConditionWorkflowTest extends AbstractWorkflowTest {

    @BeforeEach
    public void onBefore() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);
    }

    @Test
    public void testExpressionCondition() {

        // create a couple of groups
        managedRealm.admin().groups().add(GroupConfigBuilder.create().name("engineering").build()).close();
        managedRealm.admin().groups().add(GroupConfigBuilder.create().name("contractors").build()).close();

        // create a few users with different attributes, roles and group memberships
        addUser("bwayne", "Bruce", "Wayne", List.of("developer", "admin"), List.of("engineering"),
                Map.of("title", List.of("manager"), "status", List.of("active"), "key", List.of("value1", "value2")));
        addUser("lfox", "Lucius", "Fox", List.of("developer"), List.of("contractors"),
                Map.of("title", List.of("partner engineer"), "status", List.of("active"), "key", List.of("value1")));
        addUser("jgordon", "Jim", "Gordon", List.of("admin", "tester"), List.of("engineering"),
                Map.of("title", List.of("SRE admin"), "status", List.of("inactive"), "key", List.of("value1", "value2")));
        addUser("hdent", "Harvey", "Dent", List.of("developer", "tester"), List.of("engineering", "contractors"),
                Map.of("title", List.of("partner engineer"), "status", List.of("active"), "key", List.of("value2")));

        // we want to match members of engineering group OR users with admin role, but not those who are members of contractors group OR have attribute status=inactive
        // so only user bwayne should match this condition
        String expression = "(is-member-of(engineering) OR has-role(admin)) AND !(is-member-of(contractors) OR has-user-attribute(status:inactive))";
        String workflowId = createWorkflow(expression);

        checkWorkflowRunsForUser("bwayne", true); // matches all criteria
        checkWorkflowRunsForUser("lfox", false); // is member of contractors group
        checkWorkflowRunsForUser("jgordon", false); // has attribute status=inactive
        checkWorkflowRunsForUser("hdent", false); // is member of contractors group
        managedRealm.admin().workflows().workflow(workflowId).delete().close();

        // now we want to match users with attribute title=partner engineer OR users in the role tester
        expression = "has-user-attribute(title:partner engineer) OR has-role(tester)";
        workflowId = createWorkflow(expression);

        checkWorkflowRunsForUser("bwayne", false); // is not a partner engineer nor has role tester
        checkWorkflowRunsForUser("lfox", true); // is a partner engineer
        checkWorkflowRunsForUser("jgordon", true); // has role tester
        checkWorkflowRunsForUser("hdent", true); // is a partner engineer and has role tester
        managedRealm.admin().workflows().workflow(workflowId).delete().close();

        // now we want to match users who are tester and have attribute key=value1, value2
        expression = "has-role(tester) AND has-user-attribute(key:value1,value2)";
        workflowId = createWorkflow(expression);

        checkWorkflowRunsForUser("bwayne", false); // is not a tester
        checkWorkflowRunsForUser("lfox", false); // is not a tester
        checkWorkflowRunsForUser("jgordon", true); // is a tester and has both values for attribute key
        checkWorkflowRunsForUser("hdent", false); // is a tester but has only value2 for attribute key
        managedRealm.admin().workflows().workflow(workflowId).delete().close();

        // now we want to match users who are not testers and also are not managers
        expression = "!has-role(tester) AND !has-user-attribute(title:manager)";
        workflowId = createWorkflow(expression);

        checkWorkflowRunsForUser("bwayne", false); // is a manager
        checkWorkflowRunsForUser("lfox", true); // is not a tester nor a manager
        checkWorkflowRunsForUser("jgordon", false); // is a tester
        checkWorkflowRunsForUser("hdent", false); // is a tester
        managedRealm.admin().workflows().workflow(workflowId).delete().close();

        // same thing but using the OR condition with negation - results should be equivalent
        expression = "!(has-role(tester) OR has-user-attribute(title:manager))";
        workflowId = createWorkflow(expression);

        checkWorkflowRunsForUser("bwayne", false);
        checkWorkflowRunsForUser("lfox", true);
        checkWorkflowRunsForUser("jgordon", false);
        checkWorkflowRunsForUser("hdent", false);
        managedRealm.admin().workflows().workflow(workflowId).delete().close();

        // a malformed expression should cause the condition to evaluate to false and the workflow should not be created
        expression = ")(has-role(tester) AND OR has-user-attribute(key, value1,value2)";
        workflowId = createWorkflow(expression, false);
        assertThat(workflowId, nullValue());
    }

    public void checkWorkflowRunsForUser(String username, boolean shouldHaveAttribute) {

        // step 1 - login with the user
        oauth.openLoginForm();
        loginPage.fillLogin(username, username);
        loginPage.submit();
        assertTrue(driver.getPageSource().contains("Happy days"));

        // step 2 - use time offset to trigger the scheduled step for those users who match the condition
        // set offset to 6 days to trigger the scheduled step (which is set to run after 5 days)
        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertNotNull(user, username + " not found");

            if (shouldHaveAttribute) {
                assertTrue(user.getAttributes().containsKey("notified"));
                user.removeAttribute("notified");
            } else {
                assertFalse(user.getAttributes().containsKey("notified"));
            }

            // terminate the user session so we can open login form again
            session.sessions().removeUserSessions(realm, user);
        }));
    }

    public void addUser(String username, String firstName, String lastName, List<String> roles, List<String> groups, Map<String, List<String>> attributes) {
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username(username)
                .email(username + "@gotham.com")
                .name(firstName, lastName)
                .groups(groups.toArray(new String[0]))
                .password(username)
                .attributes(attributes)
                .build())) {
            String id = ApiUtil.getCreatedId(response);

            for (String roleName : roles) {
                RoleRepresentation role = createRoleIfNotExists(roleName);

                if (role.getClientRole()) {
                    managedRealm.admin().users().get(id).roles().clientLevel(role.getContainerId()).add(List.of(role));
                } else {
                    managedRealm.admin().users().get(id).roles().realmLevel().add(List.of(role));
                }
            }
        }
    }

    private String createWorkflow(String expression) {
        return this.createWorkflow(expression, true);
    }

    private String createWorkflow(String expression, boolean creationExpectedToSucceed) {
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent("user-logged-in(test-app)")
                .onCondition(expression)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("notified", "true")
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        try (Response response = workflows.create(expectedWorkflow)) {
            if (creationExpectedToSucceed) {
                assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            } else {
                assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
                return null;
            }
            return ApiUtil.getCreatedId(response);
        }
    }

    private RoleRepresentation createRoleIfNotExists(String roleName) {
        if (roleName.indexOf('/') != -1) {
            String[] parts = roleName.split("/");
            String clientId = parts[0];
            String clientRoleName = parts[1];
            List<ClientRepresentation> clients = managedRealm.admin().clients().findByClientId(clientId);

            if (clients.isEmpty()) {
                ClientRepresentation client = new ClientRepresentation();
                client.setClientId(clientId);
                client.setName(clientId);
                client.setProtocol("openid-connect");
                managedRealm.admin().clients().create(client).close();
                clients = managedRealm.admin().clients().findByClientId(clientId);
            }

            assertThat(clients.isEmpty(), is(false));

            RolesResource roles = managedRealm.admin().clients().get(clients.get(0).getId()).roles();

            if (roles.list(clientRoleName, -1, -1).isEmpty()) {
                roles.create(RoleConfigBuilder.create()
                        .name(clientRoleName)
                        .build());
            }

            return roles.get(clientRoleName).toRepresentation();
        } else {
            RolesResource roles = managedRealm.admin().roles();

            if (roles.list(roleName, -1, -1).isEmpty()) {
                roles.create(RoleConfigBuilder.create()
                        .name(roleName)
                        .build());
            }

            return roles.get(roleName).toRepresentation();
        }
    }
}
