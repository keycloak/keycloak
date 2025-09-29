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

package org.keycloak.tests.admin.model.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.resource.WorkflowResource;
import org.keycloak.admin.client.resource.WorkflowStepsResource;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.UserCreationTimeWorkflowProviderFactory;
import org.keycloak.models.workflow.WorkflowStep;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowSetRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsServerConfig.class)
public class WorkflowStepManagementTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    private WorkflowsResource workflowsResource;
    private String workflowId;

    @BeforeEach
    public void setup() {
        workflowsResource = managedRealm.admin().workflows();
        
        // Create a workflow for testing (need at least one step for persistence)
        WorkflowSetRepresentation workflows = WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.USER_ADD.toString())
                .name("Test Workflow")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build()
                )
                .build();
        
        try (Response response = workflowsResource.create(workflows)) {
            if (response.getStatus() != 201) {
                String responseBody = response.readEntity(String.class);
                System.err.println("Workflow creation failed with status: " + response.getStatus());
                System.err.println("Response body: " + responseBody);
            }
            assertEquals(201, response.getStatus());
            
            // Since we created a list of workflows, get the first one from the list
            List<WorkflowRepresentation> createdWorkflows = workflowsResource.list();
            assertNotNull(createdWorkflows);
            assertEquals(1, createdWorkflows.size());
            workflowId = createdWorkflows.get(0).getId();
        }
    }

    @Test
    public void testAddStepToWorkflow() {
        WorkflowResource workflow = workflowsResource.workflow(workflowId);
        WorkflowStepsResource steps = workflow.steps();

        WorkflowStepRepresentation stepRep = new WorkflowStepRepresentation();
        stepRep.setUses(DisableUserStepProviderFactory.ID);
        stepRep.setConfig("name", "Test Step");
        stepRep.setConfig("after", String.valueOf(Duration.ofDays(30).toMillis()));

        try (Response response = steps.create(stepRep)) {
            assertEquals(200, response.getStatus());
            WorkflowStepRepresentation addedStep = response.readEntity(WorkflowStepRepresentation.class);
            
            assertNotNull(addedStep);
            assertNotNull(addedStep.getId());
            assertEquals(DisableUserStepProviderFactory.ID, addedStep.getUses());
        }

        // Verify step is in workflow (should be 2 total: setup step + our added step)
        List<WorkflowStepRepresentation> allSteps = steps.list();
        assertEquals(2, allSteps.size());
        
        // Verify our added step is present
        boolean foundOurStep = allSteps.stream()
                .anyMatch(step -> DisableUserStepProviderFactory.ID.equals(step.getUses()) &&
                                 "Test Step".equals(step.getConfig().getFirst("name")));
        assertTrue(foundOurStep, "Our added step should be present in the workflow");
    }

    @Test
    public void testRemoveStepFromWorkflow() {
        WorkflowResource workflow = workflowsResource.workflow(workflowId);
        WorkflowStepsResource steps = workflow.steps();

        // Add one more step
        WorkflowStepRepresentation step1 = new WorkflowStepRepresentation();
        step1.setUses(DisableUserStepProviderFactory.ID);
        step1.setConfig("after", String.valueOf(Duration.ofDays(30).toMillis()));

        String step1Id;
        try (Response response = steps.create(step1)) {
            assertEquals(200, response.getStatus());
            step1Id = response.readEntity(WorkflowStepRepresentation.class).getId();
        }

        // Verify both steps exist
        List<WorkflowStepRepresentation> allSteps = steps.list();
        assertEquals(2, allSteps.size());

        // Remove the step we added
        try (Response response = steps.delete(step1Id)) {
            assertEquals(204, response.getStatus());
        }

        // Verify only the original setup step remains
        allSteps = steps.list();
        assertEquals(1, allSteps.size());
    }

    @Test
    public void testAddStepAtSpecificPosition() {
        WorkflowResource workflow = workflowsResource.workflow(workflowId);
        WorkflowStepsResource steps = workflow.steps();

        // Add first step at position 0
        WorkflowStepRepresentation step1 = new WorkflowStepRepresentation();
        step1.setUses(NotifyUserStepProviderFactory.ID);
        step1.setConfig("name", "Step 1");
        step1.setConfig("after", String.valueOf(Duration.ofDays(30).toMillis()));

        String step1Id;
        try (Response response = steps.create(step1, 0)) {
            assertEquals(200, response.getStatus());
            step1Id = response.readEntity(WorkflowStepRepresentation.class).getId();
        }
        
        // Verify step1 is at position 0
        List<WorkflowStepRepresentation> allSteps = steps.list();
        assertEquals(step1Id, allSteps.get(0).getId());

        // Add second step at position 1
        WorkflowStepRepresentation step2 = new WorkflowStepRepresentation();
        step2.setUses(DisableUserStepProviderFactory.ID);
        step2.setConfig("name", "Step 2");
        step2.setConfig("after", String.valueOf(Duration.ofDays(60).toMillis()));

        String step2Id;
        try (Response response = steps.create(step2, 1)) {
            assertEquals(200, response.getStatus());
            step2Id = response.readEntity(WorkflowStepRepresentation.class).getId();
        }
        
        // Verify step2 is at position 1
        allSteps = steps.list();
        assertEquals(step2Id, allSteps.get(1).getId());

        // Add third step at position 1 (middle)
        WorkflowStepRepresentation step3 = new WorkflowStepRepresentation();
        step3.setUses(NotifyUserStepProviderFactory.ID);
        step3.setConfig("name", "Step 3");
        step3.setConfig("after", String.valueOf(Duration.ofDays(45).toMillis())); // Between 30 and 60 days

        String step3Id;
        try (Response response = steps.create(step3, 1)) {
            assertEquals(200, response.getStatus());
            step3Id = response.readEntity(WorkflowStepRepresentation.class).getId();
        }
        
        // Verify step3 is at position 1 (inserted between step1 and step2)
        allSteps = steps.list();
        assertEquals(step1Id, allSteps.get(0).getId());
        assertEquals(step3Id, allSteps.get(1).getId());
        assertEquals(step2Id, allSteps.get(2).getId());
    }

    @Test
    public void testGetSpecificStep() {
        WorkflowResource workflow = workflowsResource.workflow(workflowId);
        WorkflowStepsResource steps = workflow.steps();

        WorkflowStepRepresentation stepRep = new WorkflowStepRepresentation();
        stepRep.setUses(NotifyUserStepProviderFactory.ID);
        stepRep.setConfig("name", "Test Step");
        stepRep.setConfig("after", String.valueOf(Duration.ofDays(15).toMillis()));

        String stepId;
        try (Response response = steps.create(stepRep)) {
            assertEquals(200, response.getStatus());
            stepId = response.readEntity(WorkflowStepRepresentation.class).getId();
        }

        // Get the specific step
        WorkflowStepRepresentation retrievedStep = steps.get(stepId);
        assertNotNull(retrievedStep);
        assertEquals(stepId, retrievedStep.getId());
        assertEquals(NotifyUserStepProviderFactory.ID, retrievedStep.getUses());
        assertEquals("Test Step", retrievedStep.getConfig().getFirst("name"));
    }

    @Test
    public void testScheduledStepTableUpdatesAfterStepManagement() {
        runOnServer.run(session -> {
            configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            Workflow workflow = manager.addWorkflow(UserCreationTimeWorkflowProviderFactory.ID, Map.of());

            WorkflowStep step1 = new WorkflowStep(NotifyUserStepProviderFactory.ID, null);
            step1.setAfter(Duration.ofDays(30).toMillis());
            WorkflowStep step2 = new WorkflowStep(DisableUserStepProviderFactory.ID, null);
            step2.setAfter(Duration.ofDays(60).toMillis());
            
            WorkflowStep addedStep1 = manager.addStepToWorkflow(workflow, step1, null);
            WorkflowStep addedStep2 = manager.addStepToWorkflow(workflow, step2, null);
            
            // Simulate scheduled steps by binding workflow to a test resource
            String testResourceId = "test-user-123";
            manager.bind(workflow, ResourceType.USERS, testResourceId);
            
            // Get scheduled steps for the workflow
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            
            var scheduledStepsBeforeRemoval = stateProvider.getScheduledStepsByWorkflow(workflow.getId());
            assertNotNull(scheduledStepsBeforeRemoval);
            
            // Remove the first step
            manager.removeStepFromWorkflow(workflow, addedStep1.getId());
            
            // Verify scheduled steps are updated
            var scheduledStepsAfterRemoval = stateProvider.getScheduledStepsByWorkflow(workflow.getId());
            assertNotNull(scheduledStepsAfterRemoval);
            
            // Verify remaining steps are still properly ordered
            List<WorkflowStep> remainingSteps = manager.getSteps(workflow.getId());
            assertEquals(1, remainingSteps.size());
            assertEquals(addedStep2.getId(), remainingSteps.get(0).getId());
            assertEquals(1, remainingSteps.get(0).getPriority()); // Should be reordered to priority 1
            
            // Add a new step and verify scheduled steps are updated
            WorkflowStep step3 = new WorkflowStep(NotifyUserStepProviderFactory.ID, null);
            step3.setAfter(Duration.ofDays(15).toMillis());
            manager.addStepToWorkflow(workflow, step3, 0); // Insert at beginning
            
            // Verify final state
            List<WorkflowStep> finalSteps = manager.getSteps(workflow.getId());
            assertEquals(2, finalSteps.size());
            assertEquals(step3.getProviderId(), finalSteps.get(0).getProviderId());
            assertEquals(1, finalSteps.get(0).getPriority());
            assertEquals(addedStep2.getId(), finalSteps.get(1).getId());
            assertEquals(2, finalSteps.get(1).getPriority());
        });
    }

    private static void configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("default");
        session.getContext().setRealm(realm);
    }
}
