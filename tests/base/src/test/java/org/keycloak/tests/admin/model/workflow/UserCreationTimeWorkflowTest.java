package org.keycloak.tests.admin.model.workflow;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.models.workflow.UserCreationTimeWorkflowProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.tests.admin.model.workflow.WorkflowManagementTest.findEmailByRecipient;

@KeycloakIntegrationTest(config = WorkflowsServerConfig.class)
public class UserCreationTimeWorkflowTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testDisableUserBasedOnCreationDate() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
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
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));

            // running the scheduled tasks now shouldn't pick up any step as none are due to run yet
            manager.runScheduledSteps();
            user = session.users().getUserByUsername(realm, "alice");
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));

            try {
                // set offset to 7 days - notify step should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledSteps();
                user = session.users().getUserByUsername(realm, "alice");
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

        // test running the scheduled steps
        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // set offset to 11 days - disable step should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledSteps();
                UserModel user = session.users().getUserByUsername(realm, "alice");
                assertFalse(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }
        }));
    }

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
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
