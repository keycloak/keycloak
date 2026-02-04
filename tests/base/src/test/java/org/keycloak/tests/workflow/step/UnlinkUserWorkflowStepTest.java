package org.keycloak.tests.workflow.step;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.workflow.UnlinkUserStepProvider;
import org.keycloak.models.workflow.UnlinkUserStepProviderFactory;
import org.keycloak.models.workflow.events.UserAuthenticatedWorkflowEventFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the execution of the 'unlink-user' workflow step.
 */
@KeycloakIntegrationTest(config = UnlinkUserWorkflowStepTest.UnlinkUserWorkflowServerConf.class)
public class UnlinkUserWorkflowStepTest extends AbstractWorkflowTest {

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    private static final String USER_NAME = "user1";
    private static final String USER_PASSWORD = "passwd1";
    private static final String FIRST_IDP_OIDC_ALIAS = "oidc-idp-one";
    private static final String SECOND_IDP_OIDC_ALIAS = "oidc-idp-two";
    private static final String THIRD_IDP_OIDC_ALIAS = "oidc-idp-third";

    @Test
    @DisplayName("UnlinkUserStep should unlink federated identities")
    public void testUnlinkFederatedUserFromAllIdentities() {
        createUnlinkWorkflow(1, FIRST_IDP_OIDC_ALIAS, THIRD_IDP_OIDC_ALIAS);

        String componentId = addDummyFederationProvider();
        String userId = addFederatedUser(componentId, USER_NAME, USER_PASSWORD);

        // Add federated identities for user
        addFederatedIdentity(userId, USER_NAME, FIRST_IDP_OIDC_ALIAS);
        addFederatedIdentity(userId, USER_NAME, SECOND_IDP_OIDC_ALIAS);
        addFederatedIdentity(userId, USER_NAME, THIRD_IDP_OIDC_ALIAS);

        // Authenticate using federated user
        authenticate(USER_NAME, USER_PASSWORD);

        // Check that user exists and has 3 IDP links
        runScheduledSteps(Duration.ZERO);
        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = UserStoragePrivateUtil.userLocalStorage(session)
                    .getUserByUsername(realm, USER_NAME);
            assertNotNull(user);
            assertThat(
                    session.users().getFederatedIdentitiesStream(realm, user).toList(),
                    hasSize(3));
        });

        // Execute unlink step
        runScheduledSteps(Duration.ofDays(2));

        // Validate unlink result
        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, userId);

            assertNotNull(user);
            assertTrue(user.isEnabled());

            var identities = session.users().getFederatedIdentitiesStream(realm, user).toList();

            assertThat(identities, hasSize(1));
            assertEquals(identities.get(0).getIdentityProvider(), SECOND_IDP_OIDC_ALIAS,
                    "Remaining federated identity should be linked only to the second "
                            + "Identity Provider after unlinking the first one.");
        });
    }

    private String addFederatedUser(String componentId, String username, String password) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFirstName("Federated");
        user.setLastName("User");
        user.setEnabled(true);
        user.setFederationLink(componentId);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(List.of(credential));
        Response createResponse = managedRealm.admin().users().create(user);
        assertEquals(201, createResponse.getStatus());
        return ApiUtil.getCreatedId(createResponse);
    }

    private void createUnlinkWorkflow(int afterDaysExecuted, String... idpAliases) {
        managedRealm.admin().workflows().create(
                WorkflowRepresentation.withName("unlink-workflow-" + KeycloakModelUtils.generateId())
                        .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                        .withSteps(
                                WorkflowStepRepresentation.create()
                                        .of(UnlinkUserStepProviderFactory.ID)
                                        .after(Duration.ofDays(afterDaysExecuted))
                                        .withConfig(UnlinkUserStepProvider.CONFIG_ALIAS, idpAliases)
                                        .build())
                        .build())
                .close();
    }

    private void authenticate(String username, String password) {
        oauth.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();

        assertTrue(driver.page().getPageSource().contains("Happy days"),
                "Test user should be successfully logged in.");
    }

    private void addFederatedIdentity(String userId, String username, String idpAlias) {
        FederatedIdentityRepresentation rep = new FederatedIdentityRepresentation();
        rep.setIdentityProvider(idpAlias);
        rep.setUserId(userId + "-" + idpAlias);
        rep.setUserName(username + "-" + idpAlias);

        managedRealm.admin().users()
                .get(userId)
                .addFederatedIdentity(idpAlias, rep)
                .close();
    }

    private String addDummyFederationProvider() {
        ComponentRepresentation dummyFederationProvider = new ComponentRepresentation();
        String componentId = KeycloakModelUtils.generateId();
        dummyFederationProvider.setId(componentId);
        dummyFederationProvider.setName(DummyUserFederationProviderFactory.PROVIDER_NAME);
        dummyFederationProvider.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
        dummyFederationProvider.setProviderType(UserStorageProvider.class.getName());
        try (Response addResponse = managedRealm.admin().components().add(dummyFederationProvider)) {
            assertEquals(201, addResponse.getStatus());
        }
        return componentId;
    }

    public static class UnlinkUserWorkflowServerConf extends WorkflowsBlockingServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return super.configure(builder)
                    .dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
