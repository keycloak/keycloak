package org.keycloak.tests.workflow.execution;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowProviderEvent;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserGroupMembershipAddedWorkflowEventFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.representations.workflows.WorkflowConstants;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
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
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
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
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
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
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
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
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create().name("mygroup").build())) {
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
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
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
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create().name("mygroup").build())) {
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
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
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
        response = managedRealm.admin().users().create(UserConfigBuilder.create()
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
