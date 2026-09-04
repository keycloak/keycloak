package org.keycloak.tests.workflow.activation;

import java.time.Duration;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.client.DisableClientStepProviderFactory;
import org.keycloak.models.workflow.events.ClientActivityWorkflowEventFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests activation of client workflows based on user-driven activity (login) events.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class ClientActivityWorkflowTest extends AbstractWorkflowTest {

    @InjectUser(ref = "alice", config = DefaultUserConfig.class, lifecycle = LifeCycle.METHOD, realmRef = DEFAULT_REALM_NAME)
    private ManagedUser userAlice;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void testActivateWorkflowOnClientActivity() {
        createWorkflow();

        // login with alice - the login event carries the client, so this attaches the workflow to the
        // client used for the login and schedules the first step
        login();

        // running the scheduled tasks now shouldn't pick up any step as none are due to run yet
        runScheduledSteps(Duration.ZERO);

        assertClientEnabled(true, "phase1: nothing due yet");

        // move the clock 4 days ahead and login again - the activity resets the workflow, so the
        // disable step is now due 9 days after the first login
        timeOffSet.set(Duration.ofDays(4));
        try {
            login();
        } finally {
            timeOffSet.set(0);
        }

        // without the reset the disable step would be due now
        runScheduledSteps(Duration.ofDays(6));

        assertClientEnabled(true, "phase2: reset moved the step past day 6");

        // setting the offset past the reset schedule should disable the client
        runScheduledSteps(Duration.ofDays(10));

        assertClientEnabled(false, "phase3: step due after day 9");
    }

    @Test
    public void testFailedLoginDoesNotActivateWorkflow() {
        createWorkflow();

        // a failed attempt carries the client on the error event but must not activate the workflow -
        // otherwise spamming bad credentials against a dormant client would keep its idle clock alive
        oauth.openLoginForm();
        loginPage.fillLogin(userAlice.getUsername(), "wrong-password");
        loginPage.submit();

        runOnServer.run((session -> {
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            provider.getWorkflows().forEach(workflow ->
                    assertThat(stateProvider.getScheduledStepsByWorkflow(workflow.getId()).toList(), empty()));
        }));
    }

    private void createWorkflow() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ClientActivityWorkflowEventFactory.ID)
                .concurrency().restartInProgress("true") // this setting enables restarting the workflow
                .withSteps(
                        WorkflowStepRepresentation.create().of(DisableClientStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();
    }

    private void login() {
        oauth.openLoginForm();
        loginPage.fillLogin(userAlice.getUsername(), userAlice.getPassword());
        loginPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    private void assertClientEnabled(boolean expectedEnabled, String phase) {
        String clientId = oauth.getClientId();

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, clientId);
            if (expectedEnabled) {
                assertTrue(client.isEnabled(), phase);
            } else {
                assertFalse(client.isEnabled(), phase);
            }
        }));
    }

    private static class DefaultUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            user.username("alice");
            user.password("alice");
            user.name("alice", "alice");
            user.email("alice@example.org");
            return user;
        }
    }
}
