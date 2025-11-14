package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;
import java.util.List;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.common.util.Time;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_ADDED;
import static org.keycloak.tests.admin.model.workflow.WorkflowManagementTest.findEmailByRecipient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class UserResourceTypeSelectorTest extends AbstractWorkflowTest {

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testDisableUserBasedOnCreationDate() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_ADDED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // create a new user - this will trigger the association with the workflow
        managedRealm.admin().users().create(
                this.getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org")).close();

        // test running the scheduled steps
        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));
        }));

        // running the scheduled tasks now shouldn't pick up any step as none are due to run yet
        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));
        }));

        // set offset to 6 days - notify step should run now
        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run((session -> {
            try {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, "alice");
                assertTrue(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }
        }));

        // Verify that the notify step was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "alice@wornderland.org");
        assertNotNull(testUserMessage, "The first step (notify) should have sent an email.");

        mailServer.runCleanup();

        // logging-in with alice should not reset the workflow - we should still run the disable step next
        oauth.openLoginForm();
        loginPage.fillLogin("alice", "alice");
        loginPage.submit();
        assertTrue(driver.getPageSource().contains("Happy days"));

        // set offset to 11 days - disable step should run now
        runScheduledSteps(Duration.ofDays(12));

        // test running the scheduled steps
        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertFalse(user.isEnabled());
        }));
    }

    private UserRepresentation getUserRepresentation(String username, String firstName, String lastName, String email) {
        UserRepresentation representation = new UserRepresentation();
        representation.setUsername(username);
        representation.setFirstName(firstName);
        representation.setLastName(lastName);
        representation.setEmail(email);
        representation.setEnabled(true);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        representation.setCredentials(List.of(credential));
        return representation;
    }
}
