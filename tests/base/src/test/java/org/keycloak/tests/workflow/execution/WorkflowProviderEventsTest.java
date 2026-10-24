package org.keycloak.tests.workflow.execution;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowProviderEvent;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserGroupMembershipAddedWorkflowEventFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.representations.workflows.WorkflowConstants;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for workflow provider events.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class WorkflowProviderEventsTest extends AbstractWorkflowTest {

    /**
     * Singleton event collector that survives serialization.
     */
    private static class WorkflowEventCollector implements ProviderEventListener, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final WorkflowEventCollector INSTANCE = new WorkflowEventCollector();

        private final List<ProviderEvent> events = new CopyOnWriteArrayList<>();

        private WorkflowEventCollector() {}

        public static WorkflowEventCollector getInstance() {
            return INSTANCE;
        }

        @Override
        public void onEvent(ProviderEvent event) {
            if (event instanceof WorkflowProviderEvent) {
                events.add(event);
            }
        }

        public List<ProviderEvent> getEvents() {
            return List.copyOf(events);
        }

        public <T extends ProviderEvent> List<T> getEvents(Class<T> eventType) {
            return events.stream()
                    .filter(eventType::isInstance)
                    .map(eventType::cast)
                    .toList();
        }

        public void clear() {
            events.clear();
        }
    }

    @AfterEach
    public void cleanup() {
        unregisterTestListener();
    }

    @Test
    public void testEventsFiredOnSimpleWorkflow() {

        // register the event listener/collector.
        registerTestListener();

        // create a simple non-scheduling workflow that activates on user creation
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .build()
                ).build();

        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(201));
            workflowId = ApiUtil.getCreatedId(response);
        }

        // create a test user to trigger the workflow
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // workflow should have fired an activated event
            List<WorkflowProviderEvent.WorkflowActivatedEvent> events = collector.getEvents(WorkflowProviderEvent.WorkflowActivatedEvent.class);
            assertThat(events, hasSize(1));
            WorkflowProviderEvent.WorkflowActivatedEvent event = events.get(0);
            String executionId = event.getExecutionId();
            assertThat(event.getWorkflowName(), equalTo("myworkflow"));
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(event.getResourceType(), equalTo(ResourceType.USERS));
            assertThat(event.getResourceId(), equalTo(userId));
            assertThat(executionId, notNullValue());
            assertThat(event.getTriggerEventType(), equalTo(UserCreatedWorkflowEventFactory.ID));

            // the step, being non-scheduling, should have executed successfully
            List<WorkflowProviderEvent.WorkflowStepExecutedEvent> stepEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepExecutedEvent.class);
            assertThat(stepEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepExecutedEvent stepEvent = stepEvents.get(0);
            assertThat(stepEvent.getWorkflowName(), equalTo("myworkflow"));
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(stepEvent.getResourceType(), equalTo(ResourceType.USERS));
            assertThat(stepEvent.getResourceId(), equalTo(userId));
            assertThat(stepEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));
            assertThat(stepEvent.getExecutionId(), equalTo(executionId));

            // execution should have completed the workflow
            List<WorkflowProviderEvent.WorkflowCompletedEvent> completedEvents = collector.getEvents(WorkflowProviderEvent.WorkflowCompletedEvent.class);
            assertThat(completedEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowCompletedEvent completedEvent = completedEvents.get(0);
            assertThat(completedEvent.getWorkflowName(), equalTo("myworkflow"));
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(completedEvent.getResourceType(), equalTo(ResourceType.USERS));
            assertThat(completedEvent.getResourceId(), equalTo(userId));
            assertThat(completedEvent.getExecutionId(), equalTo(executionId));
        });
    }

    @Test
    public void testEventsFiredOnWorkflowActivatedByAPI() {
        registerTestListener();

        // create a simple non-scheduling workflow
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("workflow-to-activate")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .build()
                ).build();

        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(201));
            workflowId = ApiUtil.getCreatedId(response);
        }

        // create a test user - should not trigger workflow and thus no events should have been fired
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();
            assertThat(collector.getEvents(), hasSize(0));
        });

        // now activate the workflow via API for the test user
        managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), userId);

        // events should have fired for activation, step execution, and completion
        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // workflow should have fired an activated event
            List<WorkflowProviderEvent.WorkflowActivatedEvent> events = collector.getEvents(WorkflowProviderEvent.WorkflowActivatedEvent.class);
            assertThat(events, hasSize(1));
            WorkflowProviderEvent.WorkflowActivatedEvent event = events.get(0);
            String executionId = event.getExecutionId();
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(event.getResourceId(), equalTo(userId));
            assertThat(event.getTriggerEventType(), equalTo(WorkflowConstants.AD_HOC));

            // the step, being non-scheduling, should have executed successfully
            List<WorkflowProviderEvent.WorkflowStepExecutedEvent> stepEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepExecutedEvent.class);
            assertThat(stepEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepExecutedEvent stepEvent = stepEvents.get(0);
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(stepEvent.getResourceId(), equalTo(userId));
            assertThat(stepEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));
            assertThat(stepEvent.getExecutionId(), equalTo(executionId));

            // execution should have completed the workflow
            List<WorkflowProviderEvent.WorkflowCompletedEvent> completedEvents = collector.getEvents(WorkflowProviderEvent.WorkflowCompletedEvent.class);
            assertThat(completedEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowCompletedEvent completedEvent = completedEvents.get(0);
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(completedEvent.getResourceId(), equalTo(userId));
            assertThat(completedEvent.getExecutionId(), equalTo(executionId));
        });
    }

    @Test
    public void testEventsFiredOnSimpleScheduledWorkflow() {
        registerTestListener();

        // create a scheduling workflow that activates on user creation
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("my-scheduling-workflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after("5d")
                                .build()
                ).build();

        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(201));
            workflowId = ApiUtil.getCreatedId(response);
        }

        // create a test user to trigger the workflow
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        String workflowExecutionId = runOnServer.fetch(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // workflow should have fired an activated event
            List<WorkflowProviderEvent.WorkflowActivatedEvent> events = collector.getEvents(WorkflowProviderEvent.WorkflowActivatedEvent.class);
            assertThat(events, hasSize(1));
            WorkflowProviderEvent.WorkflowActivatedEvent event = events.get(0);
            String executionId = event.getExecutionId();
            assertThat(event.getWorkflowName(), equalTo("my-scheduling-workflow"));
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(event.getResourceType(), equalTo(ResourceType.USERS));
            assertThat(event.getResourceId(), equalTo(userId));
            assertThat(executionId, notNullValue());
            assertThat(event.getTriggerEventType(), equalTo(UserCreatedWorkflowEventFactory.ID));

            // scheduled step should have fired a step scheduled event
            List<WorkflowProviderEvent.WorkflowStepScheduledEvent> stepScheduledEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepScheduledEvent.class);
            assertThat(stepScheduledEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepScheduledEvent stepScheduledEvent = stepScheduledEvents.get(0);
            assertThat(stepScheduledEvent.getWorkflowName(), equalTo("my-scheduling-workflow"));
            assertThat(stepScheduledEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(stepScheduledEvent.getResourceType(), equalTo(ResourceType.USERS));
            assertThat(stepScheduledEvent.getResourceId(), equalTo(userId));
            assertThat(stepScheduledEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));
            assertThat(stepScheduledEvent.getExecutionId(), equalTo(executionId));
            assertThat(stepScheduledEvent.getDelay(), equalTo("5d"));
            assertThat(stepScheduledEvent.getScheduledTime(), greaterThanOrEqualTo(System.currentTimeMillis()));

            // no other events should have fired yet
            assertThat(collector.getEvents(), hasSize(2));

            // let's clear the collector for next phase
            collector.clear();

            return executionId;
        }, String.class);

        // force execution of the scheduled step, and therefore completion of the workflow
        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // step should have executed successfully
            List<WorkflowProviderEvent.WorkflowStepExecutedEvent> stepEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepExecutedEvent.class);
            assertThat(stepEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepExecutedEvent stepEvent = stepEvents.get(0);
            assertThat(stepEvent.getWorkflowName(), equalTo("my-scheduling-workflow"));
            assertThat(stepEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(stepEvent.getResourceId(), equalTo(userId));
            assertThat(stepEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));
            assertThat(stepEvent.getExecutionId(), equalTo(workflowExecutionId));

            // execution should have completed the workflow
            List<WorkflowProviderEvent.WorkflowCompletedEvent> completedEvents = collector.getEvents(WorkflowProviderEvent.WorkflowCompletedEvent.class);
            assertThat(completedEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowCompletedEvent completedEvent = completedEvents.get(0);
            assertThat(completedEvent.getWorkflowName(), equalTo("my-scheduling-workflow"));
            assertThat(completedEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(completedEvent.getResourceId(), equalTo(userId));
            assertThat(completedEvent.getExecutionId(), equalTo(workflowExecutionId));
        });
    }

    @Test
    public void testEventsFiredWhenWorkflowIsDeactivatedByAPI() {
        registerTestListener();

        // create a scheduling workflow that activates on user creation
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("workflow-to-deactivate")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after("5d")
                                .build()
                ).build();

        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(201));
            workflowId = ApiUtil.getCreatedId(response);
        }

        // create a test user to trigger the workflow
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        String workflowExecutionId = runOnServer.fetch(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // workflow should have fired an activated event
            List<WorkflowProviderEvent.WorkflowActivatedEvent> events = collector.getEvents(WorkflowProviderEvent.WorkflowActivatedEvent.class);
            assertThat(events, hasSize(1));
            WorkflowProviderEvent.WorkflowActivatedEvent event = events.get(0);
            String executionId = event.getExecutionId();
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(event.getResourceId(), equalTo(userId));

            // scheduled step should have fired a step scheduled event
            List<WorkflowProviderEvent.WorkflowStepScheduledEvent> stepScheduledEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepScheduledEvent.class);
            assertThat(stepScheduledEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepScheduledEvent stepScheduledEvent = stepScheduledEvents.get(0);
            assertThat(stepScheduledEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(stepScheduledEvent.getResourceId(), equalTo(userId));
            assertThat(stepScheduledEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));
            assertThat(stepScheduledEvent.getExecutionId(), equalTo(executionId));

            // let's clear the collector for next phase
            collector.clear();

            return executionId;
        }, String.class);

        // deactivate the workflow via API
        managedRealm.admin().workflows().workflow(workflowId).deactivate(ResourceType.USERS.name(), userId);

        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // deactivation should have fired a workflow deactivated event
            List<WorkflowProviderEvent.WorkflowDeactivatedEvent> deactivatedEvents = collector.getEvents(WorkflowProviderEvent.WorkflowDeactivatedEvent.class);
            assertThat(deactivatedEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowDeactivatedEvent deactivatedEvent = deactivatedEvents.get(0);
            assertThat(deactivatedEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(deactivatedEvent.getResourceId(), equalTo(userId));
            assertThat(deactivatedEvent.getExecutionId(), equalTo(workflowExecutionId));
            assertThat(deactivatedEvent.getReason(), equalTo("manual deactivation"));

            // no other events should have fired
            assertThat(collector.getEvents(), hasSize(1));
        });
    }

    @Test
    public void testEventsFiredWhenWorkflowIsRestartedByEvent() {
        registerTestListener();

        // create a test group in the realm
        String groupId;
        try (Response response = managedRealm.admin().groups().add(GroupBuilder.create().name("mygroup").build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        // create a scheduling workflow that activates on user creation amd restarts when joining a group
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("workflow-to-deactivate")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .restartInProgress(UserGroupMembershipAddedWorkflowEventFactory.ID + "(mygroup)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after("5d")
                                .build()
                ).build();

        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(201));
            workflowId = ApiUtil.getCreatedId(response);
        }

        // create a test user to trigger the workflow
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        // check that workflow activated and step scheduled events were fired
        String workflowExecutionId = runOnServer.fetch(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // workflow should have fired an activated event
            List<WorkflowProviderEvent.WorkflowActivatedEvent> events = collector.getEvents(WorkflowProviderEvent.WorkflowActivatedEvent.class);
            assertThat(events, hasSize(1));
            WorkflowProviderEvent.WorkflowActivatedEvent event = events.get(0);
            String executionId = event.getExecutionId();
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(event.getResourceId(), equalTo(userId));

            // scheduled step should have fired a step scheduled event
            List<WorkflowProviderEvent.WorkflowStepScheduledEvent> stepScheduledEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepScheduledEvent.class);
            assertThat(stepScheduledEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepScheduledEvent stepScheduledEvent = stepScheduledEvents.get(0);
            assertThat(stepScheduledEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(stepScheduledEvent.getResourceId(), equalTo(userId));
            assertThat(stepScheduledEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));
            assertThat(stepScheduledEvent.getExecutionId(), equalTo(executionId));

            // let's clear the collector for next phase
            collector.clear();

            return executionId;
        }, String.class);

        // now make user join the group to trigger a restart of the workflow
        managedRealm.admin().users().get(userId).joinGroup(groupId);

        // check that workflow restarted and step rescheduled events were fired
        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // restart should have fired a workflow restarted event
            List<WorkflowProviderEvent.WorkflowRestartedEvent> restartedEvents = collector.getEvents(WorkflowProviderEvent.WorkflowRestartedEvent.class);
            assertThat(restartedEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowRestartedEvent restartedEvent = restartedEvents.get(0);
            assertThat(restartedEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(restartedEvent.getResourceId(), equalTo(userId));
            assertThat(restartedEvent.getExecutionId(), equalTo(workflowExecutionId));

            // step should have been rescheduled, so a new event should have fired
            List<WorkflowProviderEvent.WorkflowStepScheduledEvent> stepRescheduledEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepScheduledEvent.class);
            assertThat(stepRescheduledEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepScheduledEvent stepRescheduledEvent = stepRescheduledEvents.get(0);
            assertThat(stepRescheduledEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(stepRescheduledEvent.getResourceId(), equalTo(userId));
            assertThat(stepRescheduledEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));
            assertThat(stepRescheduledEvent.getExecutionId(), equalTo(workflowExecutionId));
        });
    }

    @Test
    public void testEventsFiredWhenWorkflowIsDeactivatedByEvent() {
        registerTestListener();

        // create a test group in the realm
        String groupId;
        try (Response response = managedRealm.admin().groups().add(GroupBuilder.create().name("mygroup").build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        // create a scheduling workflow that activates on user creation amd is cancelled when joining a group
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("workflow-to-deactivate")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .cancelInProgress(UserGroupMembershipAddedWorkflowEventFactory.ID + "(mygroup)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after("5d")
                                .build()
                ).build();

        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(201));
            workflowId = ApiUtil.getCreatedId(response);
        }

        // create a test user to trigger the workflow
        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        // check that workflow activated and step scheduled events were fired
        String workflowExecutionId = runOnServer.fetch(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // workflow should have fired an activated event
            List<WorkflowProviderEvent.WorkflowActivatedEvent> events = collector.getEvents(WorkflowProviderEvent.WorkflowActivatedEvent.class);
            assertThat(events, hasSize(1));
            WorkflowProviderEvent.WorkflowActivatedEvent event = events.get(0);
            String executionId = event.getExecutionId();
            assertThat(event.getWorkflowId(), equalTo(workflowId));
            assertThat(event.getResourceId(), equalTo(userId));

            // scheduled step should have fired a step scheduled event
            List<WorkflowProviderEvent.WorkflowStepScheduledEvent> stepScheduledEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepScheduledEvent.class);
            assertThat(stepScheduledEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepScheduledEvent stepScheduledEvent = stepScheduledEvents.get(0);
            assertThat(stepScheduledEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(stepScheduledEvent.getResourceId(), equalTo(userId));
            assertThat(stepScheduledEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));
            assertThat(stepScheduledEvent.getExecutionId(), equalTo(executionId));

            // let's clear the collector for next phase
            collector.clear();

            return executionId;
        }, String.class);

        // now make user join the group to cancel of the workflow
        managedRealm.admin().users().get(userId).joinGroup(groupId);

        // check that workflow cancelled event was fired
        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            // we should have a single event fired as the workflow was cancelled
            assertThat(collector.getEvents(), hasSize(1));

            // cancellation should have fired a workflow deactivated event
            List<WorkflowProviderEvent.WorkflowDeactivatedEvent> cancelledEvents = collector.getEvents(WorkflowProviderEvent.WorkflowDeactivatedEvent.class);
            assertThat(cancelledEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowDeactivatedEvent deactivatedEvent = cancelledEvents.get(0);
            assertThat(deactivatedEvent.getWorkflowId(), equalTo(workflowId));
            assertThat(deactivatedEvent.getResourceId(), equalTo(userId));
            assertThat(deactivatedEvent.getExecutionId(), equalTo(workflowExecutionId));
            assertThat(deactivatedEvent.getReason(), equalTo("event-based deactivation"));
        });
    }

    @Test
    public void testEventsFiredWhenResourcesMigrateToAnotherWorkflow() {
        registerTestListener();

        var response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow-1")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String firstWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow-2")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String secondWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        // create a user to trigger the first workflow
        response = managedRealm.admin().users().create(UserBuilder.create()
                .username("generic-user").email("generic-user@example.com").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        // check we received the scheduled event for the first workflow
        String workflowExecutionId = runOnServer.fetch(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            List<WorkflowProviderEvent.WorkflowStepScheduledEvent> stepScheduledEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepScheduledEvent.class);
            assertThat(stepScheduledEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepScheduledEvent stepScheduledEvent = stepScheduledEvents.get(0);
            assertThat(stepScheduledEvent.getWorkflowId(), equalTo(firstWorkflowId));
            assertThat(stepScheduledEvent.getResourceId(), equalTo(userId));
            assertThat(stepScheduledEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));

            collector.clear();
            return stepScheduledEvent.getExecutionId();
        }, String.class);

        // migrate the user from the first workflow to the second
        WorkflowRepresentation firstWorkflow = managedRealm.admin().workflows().workflow(firstWorkflowId).toRepresentation();
        String fromStepId = firstWorkflow.getSteps().get(0).getId();
        WorkflowRepresentation secondWorkflow = managedRealm.admin().workflows().workflow(secondWorkflowId).toRepresentation();
        String toStepId = secondWorkflow.getSteps().get(0).getId();

        managedRealm.admin().workflows().migrate(fromStepId, toStepId).close();

        // check we received the migration event
        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();

            List<WorkflowProviderEvent.WorkflowResourceMigratedEvent> migratedEvents = collector.getEvents(WorkflowProviderEvent.WorkflowResourceMigratedEvent.class);
            assertThat(migratedEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowResourceMigratedEvent migratedEvent = migratedEvents.get(0);
            assertThat(migratedEvent.getSourceWorkflowId(), equalTo(firstWorkflowId));
            assertThat(migratedEvent.getDestinationWorkflowId(), equalTo(secondWorkflowId));
            assertThat(migratedEvent.getSourceStepId(), equalTo(fromStepId));
            assertThat(migratedEvent.getDestinationStepId(), equalTo(toStepId));
            assertThat(migratedEvent.getResourceId(), equalTo(userId));
            assertThat(migratedEvent.getOldExecutionId(), equalTo(workflowExecutionId));
            assertThat(migratedEvent.getNewExecutionId(), not(equalTo(workflowExecutionId)));

            // destination workflow has only a non-scheduling step, so we should also have step executed and workflow completed events
            List<WorkflowProviderEvent.WorkflowStepExecutedEvent> stepExecutedEvents = collector.getEvents(WorkflowProviderEvent.WorkflowStepExecutedEvent.class);
            assertThat(stepExecutedEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowStepExecutedEvent stepExecutedEvent = stepExecutedEvents.get(0);
            assertThat(stepExecutedEvent.getWorkflowId(), equalTo(secondWorkflowId));
            assertThat(stepExecutedEvent.getResourceId(), equalTo(userId));
            assertThat(stepExecutedEvent.getExecutionId(), equalTo(migratedEvent.getNewExecutionId()));
            assertThat(stepExecutedEvent.getStepProviderId(), equalTo(DisableUserStepProviderFactory.ID));

            List<WorkflowProviderEvent.WorkflowCompletedEvent> completedEvents = collector.getEvents(WorkflowProviderEvent.WorkflowCompletedEvent.class);
            assertThat(completedEvents, hasSize(1));
            WorkflowProviderEvent.WorkflowCompletedEvent completedEvent = completedEvents.get(0);
            assertThat(completedEvent.getWorkflowId(), equalTo(secondWorkflowId));
            assertThat(completedEvent.getResourceId(), equalTo(userId));
            assertThat(completedEvent.getExecutionId(), equalTo(migratedEvent.getNewExecutionId()));
        });
    }

    @Test
    public void testUserEventsTriggerWorkflows() {
        registerTestListener();

        String wCredentialId = createSimpleWorkflow("w-credential", "user-update-credential(password)");
        String wRemoveCredentialId = createSimpleWorkflow("w-remove-credential", "user-remove-credential");
        String wUpdateEmailId = createSimpleWorkflow("w-update-email", "user-update-email");
        String wUpdateProfileId = createSimpleWorkflow("w-update-profile", "user-update-profile(firstName)");
        String wUpdateConsentId = createSimpleWorkflow("w-update-consent", "user-update-consent");

        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("test-trigger-user").email("trigger-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        // clear the collector so that the user creation events are discarded
        runOnServer.run(session -> WorkflowEventCollector.getInstance().clear());

        // updating a credential of a type other than the one in the filter should not activate the workflow
        fireUserEvent(EventType.UPDATE_CREDENTIAL, userId, Map.of(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE));
        assertNotActivated(wCredentialId);

        fireUserEvent(EventType.UPDATE_CREDENTIAL, userId, Map.of(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE));
        fireUserEvent(EventType.REMOVE_CREDENTIAL, userId, Map.of(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE));
        fireUserEvent(EventType.UPDATE_EMAIL, userId, Map.of());
        fireUserEvent(EventType.UPDATE_PROFILE, userId, Map.of(Details.UPDATED_FIRST_NAME, "NewFirstName"));
        fireUserEvent(EventType.UPDATE_CONSENT, userId, Map.of());
        assertActivated(wCredentialId, wRemoveCredentialId, wUpdateEmailId, wUpdateProfileId, wUpdateConsentId);

        // updating an attribute other than the one in the filter should not activate the workflow
        fireUserEvent(EventType.UPDATE_PROFILE, userId, Map.of(Details.UPDATED_LAST_NAME, "NewLastName"));
        assertNotActivated(wUpdateProfileId);
    }

    @Test
    public void testAdminOperationsTriggerWorkflows() {
        registerTestListener();

        // allow custom attributes, so that admin updates send them back along with the root attributes
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);

        String wCredentialId = createSimpleWorkflow("admin-w-credential", "user-update-credential(password)");
        String wOtpCredentialId = createSimpleWorkflow("admin-w-otp-credential", "user-update-credential(otp)");
        String wRemoveCredentialId = createSimpleWorkflow("admin-w-remove-credential", "user-remove-credential(otp)");
        String wUpdateEmailId = createSimpleWorkflow("admin-w-update-email", "user-update-email");
        String wUpdateProfileId = createSimpleWorkflow("admin-w-update-profile", "user-update-profile(firstName)");
        String wUpdateDepartmentId = createSimpleWorkflow("admin-w-update-department", "user-update-profile(department)");

        String userId;
        try (Response response = managedRealm.admin().users().create(UserBuilder.create()
                .username("admin-trigger-user").email("admin-trigger@example.com")
                .firstName("OldFirst").lastName("OldLast")
                .attribute("department", "sales").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        UserResource user = managedRealm.admin().users().get(userId);

        // clear the collector so that the user creation events are discarded
        runOnServer.run(session -> WorkflowEventCollector.getInstance().clear());

        // an update that only changes the lastName should not activate workflows listening for other attributes
        UserRepresentation userRep = user.toRepresentation();
        userRep.setLastName("NewLast");
        user.update(userRep);
        assertNotActivated(wUpdateEmailId, wUpdateProfileId, wUpdateDepartmentId);

        // an update that changes the email, the firstName and a custom attribute should activate all of them
        userRep = user.toRepresentation();
        userRep.setEmail("admin-trigger-new@example.com");
        userRep.setFirstName("NewFirst");
        userRep.singleAttribute("department", "engineering");
        user.update(userRep);
        assertActivated(wUpdateEmailId, wUpdateProfileId, wUpdateDepartmentId);

        // resetting the password should activate the workflow listening for password updates only
        CredentialRepresentation newPassword = new CredentialRepresentation();
        newPassword.setType(CredentialRepresentation.PASSWORD);
        newPassword.setValue("new-password");
        newPassword.setTemporary(false);
        user.resetPassword(newPassword);
        assertActivated(wCredentialId);
        assertNotActivated(wOtpCredentialId);

        // removing an OTP credential should activate the workflow listening for OTP credential removals
        String credentialId = runOnServer.fetch(session -> {
            UserModel userModel = session.users().getUserById(session.getContext().getRealm(), userId);
            CredentialModel credential = new CredentialModel();
            credential.setType(OTPCredentialModel.TYPE);
            credential.setUserLabel("My OTP");
            return userModel.credentialManager().createStoredCredential(credential).getId();
        }, String.class);
        user.removeCredential(credentialId);
        assertActivated(wRemoveCredentialId);
    }

    private String createSimpleWorkflow(String name, String onEvent) {
        WorkflowRepresentation workflow = WorkflowRepresentation.withName(name)
                .onEvent(onEvent)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .build()
                ).build();

        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(201));
            return ApiUtil.getCreatedId(response);
        }
    }

    /**
     * Sends a user event to the workflow event listener. Workflows are only activated once the session is committed, so
     * activations must be checked from another {@code runOnServer} call.
     */
    private void fireUserEvent(EventType type, String userId, Map<String, String> details) {
        runOnServer.run(session -> {
            EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection())
                    .event(type)
                    .user(userId);
            details.forEach((key, value) -> event.detail(key, value));
            session.getProvider(EventListenerProvider.class, "workflow-event-listener").onEvent(event.getEvent());
        });
    }

    /**
     * Asserts that the given workflows were activated since the collector was last cleared, and clears it.
     */
    private void assertActivated(String... workflowIds) {
        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();
            List<String> activated = collector.getEvents(WorkflowProviderEvent.WorkflowActivatedEvent.class).stream()
                    .map(WorkflowProviderEvent.WorkflowActivatedEvent::getWorkflowId)
                    .toList();
            assertThat(activated, hasItems(workflowIds));
            collector.clear();
        });
    }

    /**
     * Asserts that none of the given workflows were activated since the collector was last cleared, and clears it.
     */
    private void assertNotActivated(String... workflowIds) {
        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();
            List<String> activated = collector.getEvents(WorkflowProviderEvent.WorkflowActivatedEvent.class).stream()
                    .map(WorkflowProviderEvent.WorkflowActivatedEvent::getWorkflowId)
                    .toList();
            for (String workflowId : workflowIds) {
                assertThat(activated, not(hasItem(workflowId)));
            }
            collector.clear();
        });
    }

    /**
     * We need to call this in every test because the runOnServer is not available yet when @BeforeEach is called.
     */
    private void registerTestListener() {
        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();
            session.getKeycloakSessionFactory().register(collector);
            collector.clear();
        });
    }

    /**
     * Called automatically after each test via @AfterEach to unregister the test listener.
     */
    private void unregisterTestListener() {
        runOnServer.run(session -> {
            WorkflowEventCollector collector = WorkflowEventCollector.getInstance();
            session.getKeycloakSessionFactory().unregister(collector);
            collector.clear();
        });
    }
}
