package org.keycloak.tests.admin.model.policy;

import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.UserActionBuilder;
import org.keycloak.models.policy.UserCreationTimeResourcePolicyProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
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

@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class UserCreationTimePolicyTest {

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

    @Test
    public void testDisableUserBasedOnCreationDate() {
        runOnServer.run((RunOnServer) session -> {
            configureSessionContext(session);
            PolicyBuilder.create()
                    .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                    .withActions(
                            UserActionBuilder.builder(NotifyUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(5))
                                    .build(),
                            UserActionBuilder.builder(DisableUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(10))
                                    .build()
                    ).build(session);
        });

        // create a new user - this will trigger the association with the policy
        managedRealm.admin().users().create(
                this.getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"));

        // test running the scheduled actions
        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));

            // running the scheduled tasks now shouldn't pick up any action as none are due to run yet
            manager.runScheduledTasks();
            user = session.users().getUserByUsername(realm, "alice");
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));

            try {
                // set offset to 7 days - notify action should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledTasks();
                user = session.users().getUserByUsername(realm, "alice");
                assertTrue(user.isEnabled());
                assertNotNull(user.getAttributes().get("message"));
            } finally {
                Time.setOffset(0);
            }
        }));

        // logging-in with alice should not reset the policy - we should still run the disable action next
        oauth.openLoginForm();
        loginPage.fillLogin("alice", "alice");
        loginPage.submit();
        assertTrue(driver.getPageSource().contains("Happy days"));

        // test running the scheduled actions
        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            try {
                // set offset to 11 days - disable action should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledTasks();
                UserModel user = session.users().getUserByUsername(realm, "alice");
                assertFalse(user.isEnabled());
                assertNotNull(user.getAttributes().get("message"));
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
