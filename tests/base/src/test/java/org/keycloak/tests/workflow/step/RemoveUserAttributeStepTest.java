package org.keycloak.tests.workflow.step;

import java.time.Duration;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_CREATED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class RemoveUserAttributeStepTest extends AbstractWorkflowTest {

    @Test
    public void testRemoveAttributes() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        // create workflow that removes attributes 'a' and 'b' on user creation
        create(WorkflowRepresentation.withName("remove-attrs")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(org.keycloak.models.workflow.RemoveUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "a", "b", "another-key")
                                .build()
                ).build());

        // create user with attributes a, b and c
        UserRepresentation user = UserConfigBuilder.create().username("attruser").attribute("a", "1").attribute("b", "2").attribute("c", "3").build();

        String userId;
        try (Response response = managedRealm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }

        UserResource userResource = managedRealm.admin().users().get(userId);

        Awaitility.await()
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    UserRepresentation rep = userResource.toRepresentation();
                    // attributes 'a' and 'b' should be removed, 'c' should remain
                    assertThat(rep.getAttributes().containsKey("a"), is(false));
                    assertThat(rep.getAttributes().containsKey("b"), is(false));
                    assertThat(rep.getAttributes().containsKey("c"), is(true));
                });
    }
}
