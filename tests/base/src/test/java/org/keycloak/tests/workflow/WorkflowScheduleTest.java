package org.keycloak.tests.workflow;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class WorkflowScheduleTest extends AbstractWorkflowTest {

    @InjectUser(ref = "alice", config = DefaultUserConfig.class, lifecycle = LifeCycle.METHOD, realmRef = DEFAULT_REALM_NAME)
    private ManagedUser userAlice;

    @Test
    public void testSchedule() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ADMIN_VIEW);
        managedRealm.admin().users().userProfile().update(upConfig);
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").batchSize(10).build())
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("test", "test")
                                .build()
                ).build();

        managedRealm.admin().workflows().create(expectedWorkflow).close();

        Awaitility.await()
                .timeout(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    UserResource user = managedRealm.admin().users().get(userAlice.getId());
                    Map<String, List<String>> attributes = user.getUnmanagedAttributes();
                    assertThat(attributes, notNullValue());
                    assertThat(attributes.get("test"), containsInAnyOrder("test"));
                });
    }

    @Test
    public void testScheduleWithDisabledWorkflow() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ADMIN_VIEW);
        managedRealm.admin().users().userProfile().update(upConfig);
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .withConfig("enabled", "false")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").batchSize(10).build())
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("test", "test")
                                .build()
                ).build();

        managedRealm.admin().workflows().create(expectedWorkflow).close();

        // wait for 10 seconds to make sure the schedule task would have had time to run
        // however, the workflow is disabled so no task should have been fired and the attribute should not be set - we expect a timeout exception here
        assertThrows(ConditionTimeoutException.class, () -> Awaitility.await()
                .timeout(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    UserResource user = managedRealm.admin().users().get(userAlice.getId());
                    Map<String, List<String>> attributes = user.getUnmanagedAttributes();
                    assertThat(attributes, notNullValue());
                    assertThat(attributes.get("test"), containsInAnyOrder("test"));
                }));
    }

    @Test
    public void testInvalidSchedule() {
        assertInvalidSchedule(WorkflowScheduleRepresentation.create().after("abc").build(), "Schedule 'after' configuration is not valid: abc.");
        assertInvalidSchedule(WorkflowScheduleRepresentation.create().after("30000d").build(), "Schedule 'after' configuration is not valid: 30000d.");
        assertInvalidSchedule(WorkflowScheduleRepresentation.create().after("").build(), "Schedule 'after' configuration must be at least 1 second.");
        assertInvalidSchedule(WorkflowScheduleRepresentation.create().after("0").build(), "Schedule 'after' configuration must be at least 1 second.");
        assertInvalidSchedule(WorkflowScheduleRepresentation.create().after("500ms").build(), "Schedule 'after' configuration must be at least 1 second.");
        assertInvalidSchedule(WorkflowScheduleRepresentation.create().after("-5s").build(), "Schedule 'after' configuration must be at least 1 second.");
        assertInvalidSchedule(WorkflowScheduleRepresentation.create().after("1d").batchSize(0).build(), "Schedule 'batch-size' configuration must be greater than 0.");
        assertInvalidSchedule(WorkflowScheduleRepresentation.create().after("1d").batchSize(-1).build(), "Schedule 'batch-size' configuration must be greater than 0.");
    }

    @Test
    public void testUpdateWithInvalidSchedule() {
        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(workflowWithSchedule(WorkflowScheduleRepresentation.create().after("1d").batchSize(1).build()))) {
            workflowId = ApiUtil.getCreatedId(response);
        }

        WorkflowRepresentation workflow = managedRealm.admin().workflows().workflow(workflowId).toRepresentation();
        workflow.setSchedule(WorkflowScheduleRepresentation.create().after("abc").build());
        try (Response response = managedRealm.admin().workflows().workflow(workflowId).update(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertThat(error.getErrorMessage(), is("Schedule 'after' configuration is not valid: abc."));
        }
    }

    private void assertInvalidSchedule(WorkflowScheduleRepresentation schedule, String expectedError) {
        try (Response response = managedRealm.admin().workflows().create(workflowWithSchedule(schedule))) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertThat(error.getErrorMessage(), is(expectedError));
        }
    }

    private WorkflowRepresentation workflowWithSchedule(WorkflowScheduleRepresentation schedule) {
        return WorkflowRepresentation.withName("myworkflow")
                .schedule(schedule)
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("test", "test")
                                .build()
                ).build();
    }

    private static class DefaultUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            user.username("alice");
            user.password("alice");
            user.name("alice", "alice");
            user.email("master-admin@email.org");
            return user;
        }
    }
}
