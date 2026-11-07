/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.workflow.condition;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.models.workflow.WorkflowStep;
import org.keycloak.models.workflow.conditions.IdentityProviderWorkflowConditionFactory;
import org.keycloak.models.workflow.conditions.IdentityProviderWorkflowConditionProvider;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserFedIdentityAddedWorkflowEventFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests activating workflows with conditions based on presence of a federated identity linked to an IdP.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class IdpLinkConditionWorkflowTest extends AbstractWorkflowTest {

    private static final String IDP_OIDC_ALIAS = "my-idp";
    private static final String IDP_OIDC_PROVIDER_ID = KeycloakOIDCIdentityProviderFactory.PROVIDER_ID;

    @Test
    public void testActivateWorkflowForUsersLinkedToIdp() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        // create the test identity provider in the realm
        setupIdentityProvider();

        // create the workflow that triggers on IdP linking with a condition for the specific IdP
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("idp-members-workflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .onCondition(IdentityProviderWorkflowConditionFactory.ID + "(" + IDP_OIDC_ALIAS + ")")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .build()
                ).build();
        managedRealm.admin().workflows().create(workflow).close();

        // create a test user not linked to the identity provider
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("no-idp-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        // check the workflow hasn't run for this user
        UserRepresentation userRepresentation = managedRealm.admin().users().get(userId).toRepresentation();
        assertThat(userRepresentation.getAttributes(), nullValue());

        // create another user linked to the identity provider - this time the workflow should trigger
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("idp-user").federatedLink(IDP_OIDC_ALIAS, UUID.randomUUID().toString(), "fed-user-123").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        userRepresentation = managedRealm.admin().users().get(userId).toRepresentation();
        assertThat(userRepresentation.getAttributes().get("attribute"), notNullValue());
        assertThat(userRepresentation.getAttributes().get("attribute").get(0), is("attr1"));

    }

    @Test
    public void testAssignWorkflowToExistingResources() {
        // create some realm users
        for (int i = 0; i < 10; i++) {
            managedRealm.admin().users().create(UserBuilder.create().username("user-" + i).build()).close();
        }

        // create some users associated with a federated identity
        for (int i = 0; i < 10; i++) {
            managedRealm.admin().users().create(UserBuilder.create().username("idp-user-" + i)
                    .federatedLink(IDP_OIDC_ALIAS, UUID.randomUUID().toString(), "idp-user-" + i).build()).close();
        }

        setupIdentityProvider();

        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserFedIdentityAddedWorkflowEventFactory.ID)
                .onCondition(IdentityProviderWorkflowConditionFactory.ID + "(" + IDP_OIDC_ALIAS + ")")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        // now with the workflow in place, let's create a couple more idp users - these will be attached to the workflow on creation.
        for (int i = 0; i < 3; i++) {
            managedRealm.admin().users().create(UserBuilder.create().username("new-idp-user-" + i)
                    .federatedLink(IDP_OIDC_ALIAS, UUID.randomUUID().toString(), "new-idp-user-" + i).build()).close();
        }

        // new realm users created after the workflow - these should not be attached to the workflow because they are not idp users.
        for (int i = 0; i < 3; i++) {
            managedRealm.admin().users().create(UserBuilder.create().username("new-user-" + i).build()).close();
        }

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
            assertEquals(1, registeredWorkflows.size());
            Workflow workflow = registeredWorkflows.get(0);
            List<WorkflowStep> steps = workflow.getSteps().toList();
            assertEquals(2, steps.size());
            WorkflowStep notifyStep = steps.get(0);

            // check no workflows are yet attached to the previous users, only to the ones created after the workflow was in place
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow.getId()).toList();
            assertEquals(3, scheduledSteps.size());
            scheduledSteps.forEach(scheduledStep -> {
                assertEquals(notifyStep.getId(), scheduledStep.stepId());
                UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                assertNotNull(user);
                assertTrue(user.getUsername().startsWith("new-idp-user-"));
            });
        });

        // let's run the schedule steps for the new users so they transition to the next one.
        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            // check the same users are now scheduled to run the second step.
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
            assertEquals(1, registeredWorkflows.size());
            Workflow workflow = registeredWorkflows.get(0);
            WorkflowStep disableStep = workflow.getSteps().toList().get(1);
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);

            List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow.getId()).toList();
            assertEquals(3, scheduledSteps.size());
            scheduledSteps.forEach(scheduledStep -> {
                assertEquals(disableStep.getId(), scheduledStep.stepId());
                UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                assertNotNull(user);
                assertTrue(user.getUsername().startsWith("new-idp-user-"));
            });
        });

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));

        Awaitility.await()
                .timeout(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    runOnServer.run((RunOnServer) session -> {
                        RealmModel realm = session.getContext().getRealm();
                        // check the same users are now scheduled to run the second step.
                        WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
                        List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
                        assertEquals(1, registeredWorkflows.size());
                        Workflow workflow = registeredWorkflows.get(0);
                        // check workflow was correctly assigned to the old users, not affecting users already associated with the workflow.
                        WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                        List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow.getId()).toList();
                        assertEquals(13, scheduledSteps.size());

                        List<WorkflowStep> steps = workflow.getSteps().toList();
                        assertEquals(2, steps.size());
                        WorkflowStep notifyStep = steps.get(0);
                        List<ScheduledStep> scheduledToNotify = scheduledSteps.stream()
                                .filter(step -> notifyStep.getId().equals(step.stepId())).toList();
                        assertEquals(10, scheduledToNotify.size());
                        scheduledToNotify.forEach(scheduledStep -> {
                            UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                            assertNotNull(user);
                            assertTrue(user.getUsername().startsWith("idp-user-"));
                        });

                        WorkflowStep disableStep = workflow.getSteps().toList().get(1);
                        List<ScheduledStep> scheduledToDisable = scheduledSteps.stream()
                                .filter(step -> disableStep.getId().equals(step.stepId())).toList();
                        assertEquals(3, scheduledToDisable.size());
                        scheduledToDisable.forEach(scheduledStep -> {
                            UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                            assertNotNull(user);
                            assertTrue(user.getUsername().startsWith("new-idp-user-"));
                        });
                    });
                });
    }

    @Test
    public void testConditionForNonExistingIdp() {
        // create a workflow with a condition on an identity provider that does not exist - the workflow is accepted
        // as the IdP existence is not checked when creating or updating a workflow
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("missing-idp-workflow")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
                .onCondition(IdentityProviderWorkflowConditionFactory.ID + "(missing-idp)")
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .build()
                ).build()).close();

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));

        // create a user with a federated identity link to the non-existent provider - links to unknown
        // aliases are allowed, so the user exists and holds a matching federated identity
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("user-missing-idp")
                .federatedLink("missing-idp", UUID.randomUUID().toString(), "fed-user-123").build())) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            userId = ApiUtil.getCreatedId(response);
        }

        runOnServer.run((RunOnServer) session -> {
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().findFirst().orElse(null);
            assertNotNull(workflow);

            // resolving the eligible resources must not throw even though the identity provider does not exist
            provider.activateForAllEligibleResources(workflow);

            // the condition must simply not match when the identity provider does not exist, even though the
            // user holds a federated identity with the same alias; assert the result directly on the condition
            // as activate() catches and logs evaluation exceptions, which would let the state assertions
            // below pass even when the condition throws
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, userId);
            assertNotNull(user);
            IdentityProviderWorkflowConditionProvider condition = new IdentityProviderWorkflowConditionProvider(session, "missing-idp");
            assertFalse(condition.evaluate(createConditionContext(user.getId())));

            // activating the workflow for the user must not throw; the condition is simply not matched
            provider.activate(workflow, ResourceType.USERS, user.getId());

            // the user must not be activated for the workflow as the identity provider does not exist
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            assertThat(stateProvider.getScheduledStepsByWorkflow(workflow.getId()).toList(), is(empty()));
        });
    }

    private static WorkflowExecutionContext createConditionContext(String resourceId) {
        return new WorkflowExecutionContext() {
            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public WorkflowEvent getEvent() {
                return null;
            }

            @Override
            public WorkflowStep getNextStep() {
                return null;
            }
        };
    }

    private void setupIdentityProvider() {
        IdentityProviderRepresentation rep = IdentityProviderBuilder.create().alias(IDP_OIDC_ALIAS).providerId(IDP_OIDC_PROVIDER_ID)
                .displayName(IDP_OIDC_PROVIDER_ID)
                .attribute("clientId", "test-client")
                .attribute("clientSecret", "secret")
                .attribute("authorizationUrl", "http://localhost:8080/realms/" + DEFAULT_REALM_NAME + "/protocol/openid-connect/auth")
                .attribute("tokenUrl", "http://localhost:8080/realms/" + DEFAULT_REALM_NAME + "/protocol/openid-connect/token")
                .attribute("logoutUrl", "http://localhost:8080/realms/" + DEFAULT_REALM_NAME + "/protocol/openid-connect/logout")
                .attribute("userInfoUrl", "http://localhost:8080/realms/" + DEFAULT_REALM_NAME + "/protocol/openid-connect/userinfo")
                .attribute("defaultScope", "email profile")
                .attribute("backchannelSupported", "true")
                .build();

        managedRealm.admin().identityProviders().create(rep).close();
    }
}
