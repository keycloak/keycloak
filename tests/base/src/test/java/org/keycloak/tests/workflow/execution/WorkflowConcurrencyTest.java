package org.keycloak.tests.workflow.execution;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.events.UserAuthenticatedWorkflowEventFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for workflows with concurrency settings that allow restarting and cancelling the workflow using the same activation event
 * or different events.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class WorkflowConcurrencyTest extends AbstractWorkflowTest {

    @InjectUser(ref = "alice", config = WorkflowConcurrencyTest.DefaultUserConfig.class, lifecycle = LifeCycle.METHOD, realmRef = DEFAULT_REALM_NAME)
    private ManagedUser userAlice;

    @Test
    public void testWorkflowIsRestartedOnSameEvent() {
        // create a workflow that can be restarted on the same event - i.e. has concurrency setting with restart-in-progress=true
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .concurrency().restartInProgress("true")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .after(Duration.ofDays(1))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // create a test group so we can use it to trigger a non-restarting event
        String testGroupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("testgroup").build())) {
            testGroupId = ApiUtil.getCreatedId(response);
        }
        this.assertWorkflowAffectedOnCorrectEvent(
                () -> managedRealm.admin().users().get(userAlice.getId()).joinGroup(testGroupId), // unrelated event
                () -> oauth.openLoginForm(), // correct event
                false);
    }

    @Test
    public void testWorkflowIsRestartedOnDifferentEvent() {
        // create a couple of test groups to trigger different group membership events
        String testGroupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("testgroup").build())) {
            testGroupId = ApiUtil.getCreatedId(response);
        }
        String anotherGroupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("anothergroup").build())) {
            anotherGroupId = ApiUtil.getCreatedId(response);
        }

        // create a workflow that can be restarted on a different event - i.e. restart-in-progress is set to an event expression
        // in this case we will use user-group-membership-added event to restart the workflow when user joins the group "testgroup"
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .concurrency().restartInProgress("user-group-membership-added(testgroup)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .after(Duration.ofDays(1))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        this.assertWorkflowAffectedOnCorrectEvent(
                () -> managedRealm.admin().users().get(userAlice.getId()).joinGroup(anotherGroupId), // unrelated event
                () -> managedRealm.admin().users().get(userAlice.getId()).joinGroup(testGroupId), // correct event
                false);
    }

    @Test
    public void testWorkflowIsCancelledOnSameEvent() {
        // create a workflow that can be cancelled on the same event - i.e. has concurrency setting with cancel-in-progress=true
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .concurrency().cancelInProgress("true")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .after(Duration.ofDays(1))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // create a test group so we can use it to trigger a non-restarting event
        String testGroupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("testgroup").build())) {
            testGroupId = ApiUtil.getCreatedId(response);
        }
        this.assertWorkflowAffectedOnCorrectEvent(
                () -> managedRealm.admin().users().get(userAlice.getId()).joinGroup(testGroupId), // unrelated event
                () -> oauth.openLoginForm(), // correct event
                true);
    }

    @Test
    public void testWorkflowIsCancelledOnDifferentEvent() {
        // create a couple of test groups to trigger different group membership events
        String testGroupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("testgroup").build())) {
            testGroupId = ApiUtil.getCreatedId(response);
        }
        String anotherGroupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("anothergroup").build())) {
            anotherGroupId = ApiUtil.getCreatedId(response);
        }

        // create a workflow that can be cancelled on a different event - i.e. cancel-in-progress is set to an event expression
        // in this case we will use user-group-membership-added event to cancel the workflow when user joins the group "testgroup"
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .concurrency().cancelInProgress("user-group-membership-added(testgroup)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .after(Duration.ofDays(1))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        this.assertWorkflowAffectedOnCorrectEvent(
                () -> managedRealm.admin().users().get(userAlice.getId()).joinGroup(anotherGroupId), // unrelated event
                () -> managedRealm.admin().users().get(userAlice.getId()).joinGroup(testGroupId), // correct event
                true);
    }

    @Test
    public void testWorkflowIsRestartedOnSameEventAndCancelledOnDifferentEvent() {
        // create a couple of test groups to trigger different group membership events
        String testGroupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("testgroup").build())) {
            testGroupId = ApiUtil.getCreatedId(response);
        }
        String anotherGroupId;
        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("anothergroup").build())) {
            anotherGroupId = ApiUtil.getCreatedId(response);
        }

        // create workflow with both settings - restart-in-progress on same event, cancel-in-progress on different event
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .concurrency().restartInProgress("true")
                              .cancelInProgress("user-group-membership-added(testgroup)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .after(Duration.ofDays(1))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // joining "anothergroup" should have no effect, so test that re-authenticating restarts the workflow
        this.assertWorkflowAffectedOnCorrectEvent(
                () -> managedRealm.admin().users().get(userAlice.getId()).joinGroup(anotherGroupId), // unrelated event
                () -> oauth.openLoginForm(), // correct event to restart
                false); // should not cancel the workflow

        // now joining "testgroup" should cancel the workflow
        this.assertWorkflowAffectedOnCorrectEvent(
                () -> {}, // do nothing as the unrelated event for this test
                () -> managedRealm.admin().users().get(userAlice.getId()).joinGroup(testGroupId), // correct event to cancel
                false, // do not attempt to login again
                true); // should cancel the workflow
    }

    private void assertWorkflowAffectedOnCorrectEvent(Runnable unrelatedEventTrigger, Runnable relatedEventTrigger, boolean cancelled) {
        this.assertWorkflowAffectedOnCorrectEvent(unrelatedEventTrigger, relatedEventTrigger, true, cancelled);
    }

    private void assertWorkflowAffectedOnCorrectEvent(Runnable unrelatedEventTrigger, Runnable relatedEventTrigger, boolean login, boolean cancelled) {

        String userId = userAlice.getId();
        String username = userAlice.getUsername();
        if (login) {
            // login with alice - this will attach the workflow to the user and schedule the first step
            oauth.openLoginForm();
            loginPage.fillLogin(username, userAlice.getPassword());
            loginPage.submit();
            assertTrue(driver.page().getPageSource() != null && driver.page().getPageSource().contains("Happy days"));
        }

        // store the first step id for later comparison
        String firstStepId = runOnServer.fetch(session-> {
            WorkflowStateProvider provider = session.getProvider(WorkflowStateProvider.class);
            List< WorkflowStateProvider.ScheduledStep> steps = provider.getScheduledStepsByResource(userId).toList();
            assertThat(steps, hasSize(1));
            return steps.get(0).stepId();
        }, String.class);

        // run the first schedule task - workflow should now be waiting to run the second step
        runScheduledSteps(Duration.ofDays(2));
        String secondStepId = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            // first step should have run and the attribute should be set
            assertThat(user.getFirstAttribute("attribute"), is("attr1"));
            assertTrue(user.isEnabled());

            WorkflowStateProvider provider = session.getProvider(WorkflowStateProvider.class);
            List< WorkflowStateProvider.ScheduledStep> steps = provider.getScheduledStepsByResource(userId).toList();
            assertThat(steps, hasSize(1));
            return steps.get(0).stepId();
        }, String.class);
        assertThat(secondStepId, is(not(firstStepId)));

        // run the non-restarting event trigger - the workflow must not be restarted
        unrelatedEventTrigger.run();

        runOnServer.run(session -> {
            WorkflowStateProvider provider = session.getProvider(WorkflowStateProvider.class);
            List< WorkflowStateProvider.ScheduledStep> steps = provider.getScheduledStepsByResource(userId).toList();
            // step id must remain the same as before
            assertThat(steps, hasSize(1));
            assertThat(steps.get(0).stepId(), is(secondStepId));
        });

        // run the restarting event trigger - this must restart the workflow
        relatedEventTrigger.run();

        // workflow should have been restarted or cancelled
        runOnServer.run(session -> {
            WorkflowStateProvider provider = session.getProvider(WorkflowStateProvider.class);
            List< WorkflowStateProvider.ScheduledStep> steps = provider.getScheduledStepsByResource(userId).toList();
            // step id must be the first one now as the workflow was restarted
            if (cancelled) {
                assertThat(steps, hasSize(0));
            } else {
                // restarted - first step must be scheduled again
                assertThat(steps, hasSize(1));
                assertThat(steps.get(0).stepId(), is(firstStepId));
            }
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
