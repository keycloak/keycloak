package org.keycloak.tests.workflow;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class WorkflowScheduleTest extends AbstractWorkflowTest {

    @InjectUser(ref = "alice", config = DefaultUserConfig.class, lifecycle = LifeCycle.METHOD, realmRef = DEFAULT_REALM_NAME)
    private ManagedUser userAlice;

    @Test
    public void testSchedule() {
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

    private static class DefaultUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            user.username("alice");
            user.password("alice");
            user.name("alice", "alice");
            user.email("master-admin@email.org");
            return user;
        }
    }
}
