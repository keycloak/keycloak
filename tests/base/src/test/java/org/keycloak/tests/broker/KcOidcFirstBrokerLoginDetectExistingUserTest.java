package org.keycloak.tests.broker;

import java.io.IOException;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.broker.IdpAutoLinkAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpDetectExistingBrokerUserAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpEmailVerificationAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.AuthenticationExecutionBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.ProceedPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcFirstBrokerLoginDetectExistingUserTest extends AbstractKcOidcBrokerTest {

    private static final String DETECT_EXISTING_FLOW_ALIAS = "detectExistingUserFlow";

    @InjectPage
    ErrorPage errorPage;

    // Injecting the mail server also configures the SMTP settings of the realms created for the test,
    // so Keycloak can send the account linking emails to it.
    @InjectMailServer
    MailServer mail;

    // The account linking email may be confirmed from a different browser/session than the one that
    // started the first-broker-login flow.
    @InjectWebDriver(ref = "driver2")
    ManagedWebDriver secondDriver;

    @InjectPage(webDriverRef = "driver2")
    ProceedPage secondBrowserProceedPage;

    @BeforeEach
    void setupDetectExistingUserFlow() {
        RealmResource consumer = consumerRealm.admin();
        AuthenticationManagementResource authMgmtResource = consumer.flows();

        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(DETECT_EXISTING_FLOW_ALIAS);
        flow.setDescription(DETECT_EXISTING_FLOW_ALIAS);
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);
        authMgmtResource.createFlow(flow);

        AuthenticationFlowRepresentation created = authMgmtResource.getFlows().stream()
                .filter(v -> DETECT_EXISTING_FLOW_ALIAS.equals(v.getAlias()))
                .findFirst()
                .orElse(null);
        assertNotNull(created, "The authentication flow must exist");

        String flowId = created.getId();
        addExecution(authMgmtResource, flowId, IdpDetectExistingBrokerUserAuthenticatorFactory.PROVIDER_ID, 10);
        addExecution(authMgmtResource, flowId, IdpAutoLinkAuthenticatorFactory.PROVIDER_ID, 20);

        IdentityProviderResource identityConsumerResource = consumer.identityProviders().get(IDP_OIDC_ALIAS);
        IdentityProviderRepresentation identityProviderRepresentation = consumer.identityProviders().findAll().get(0);
        identityProviderRepresentation.setFirstBrokerLoginFlowAlias(DETECT_EXISTING_FLOW_ALIAS);
        identityProviderRepresentation.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.toString());
        identityConsumerResource.update(identityProviderRepresentation);

        assertEquals(2, authMgmtResource.getFlows().stream()
                        .filter(v -> DETECT_EXISTING_FLOW_ALIAS.equals(v.getAlias()))
                        .findFirst()
                        .orElseThrow()
                        .getAuthenticationExecutions()
                        .size(),
                "Two executions must have been created");
    }

    private void addExecution(AuthenticationManagementResource authMgmtResource, String flowId, String providerId, int priority) {
        AuthenticationExecutionRepresentation exec = AuthenticationExecutionBuilder.create()
                .parentFlow(flowId)
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                .authenticator(providerId)
                .priority(priority)
                .authenticatorFlow(false)
                .build();
        authMgmtResource.addExecution(exec);
    }

    private void createUser(ManagedRealm realm, String username, String password, String firstName, String lastName, String email) {
        UserBuilder builder = UserBuilder.create(username)
                .email(email)
                .password(password)
                .enabled(true);
        if (firstName != null) {
            builder.firstName(firstName);
        }
        if (lastName != null) {
            builder.lastName(lastName);
        }
        try (Response response = realm.admin().users().create(builder.build())) {
            assertEquals(201, response.getStatus());
            String userId = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.users().get(userId).remove());
        }
    }

    private void logInWithIdp(String username, String password) {
        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    // Default broker login scenarios do not apply with the custom detect-existing first-broker-login flow.
    @Override
    @Test
    @Disabled("Uses a custom detect-existing first-broker-login flow")
    public void testLogInAsUserInIDP() {
    }

    @Override
    @Test
    @Disabled("Uses a custom detect-existing first-broker-login flow")
    public void testLoginWithExistingUser() {
    }

    @Test
    public void loginWhenUserDoesNotExistOnConsumer() {
        disableUpdateProfileOnFirstLogin();

        String firstname = "Firstname";
        String lastname = "Lastname";
        String username = "firstandlastname";
        createUser(providerRealm, username, USER_PASSWORD, firstname, lastname, "firstnamelastname@example.org");

        logInWithIdp(username, USER_PASSWORD);

        errorPage.assertCurrent();
        assertEquals("User " + username + " authenticated with identity provider " + IDP_OIDC_ALIAS
                + " does not exist. Please contact your administrator.", errorPage.getError());
    }

    @Test
    public void loginWhenUserExistsOnConsumer() {
        disableUpdateProfileOnFirstLogin();

        final String firstname = "Firstname_loginWhenUserExistsOnConsumer";
        final String lastname = "Lastname_loginWhenUserExistsOnConsumer";
        final String username = "firstandlastname";
        final String email = "firstnamelastname@example.org";
        createUser(providerRealm, username, USER_PASSWORD, firstname, lastname, email);
        createUser(consumerRealm, username, "THIS PASSWORD IS USELESS", null, null, email);

        logInWithIdp(username, USER_PASSWORD);

        assertTrue(oauth.parseLoginResponse().isSuccess(), "Broker login should complete successfully");
        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(consumerRealm.admin(), username);

        assertEquals(email, userRepresentation.getEmail(), "Email is not correct");
        assertEquals(firstname, userRepresentation.getFirstName(), "Firstname is not correct");
        assertEquals(lastname, userRepresentation.getLastName(), "Lastname is not correct");
    }


    @Test
    public void testLinkAccountByEmailVerificationDifferentBrowser() throws IOException, MessagingException {
        AuthenticationManagementResource authMgmtResource = consumerRealm.admin().flows();
        addEmailVerificationExecution(authMgmtResource);

        consumerRealm.updateWithCleanup(realm -> realm.verifyEmail(true));

        // don't trust the email from the IdP, so the email verification required action is enforced
        consumerRealm.updateIdentityProvider(IDP_OIDC_ALIAS, rep -> rep.setTrustEmail(false));

        // to avoid update profile required action
        UserRepresentation brokerUser = providerRealm.admin().users().search(getUserLogin()).get(0);
        brokerUser.setFirstName("f");
        brokerUser.setLastName("l");
        providerRealm.admin().users().get(brokerUser.getId()).update(brokerUser);
        // creates a user in the consumer realm to link the account
        brokerUser.setId(null);
        try (Response response = consumerRealm.admin().users().create(brokerUser)) {
            String consumerUserId = ApiUtil.getCreatedId(response);
            consumerRealm.cleanup().add(realm -> realm.users().get(consumerUserId).remove());
        }

        // link the account
        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        // the existing consumer user is detected and, as its email is verified, an email with instructions
        // to link the account is sent
        assertTrue(mail.waitForIncomingEmail(1), "An email with instructions to link the account was expected");
        MimeMessage lastReceivedMessage = mail.getLastReceivedMessage();
        assertNotNull(lastReceivedMessage);
        assertEquals(getUserEmail(), MailUtils.getRecipient(lastReceivedMessage));
        MailUtils.EmailBody body = MailUtils.getBody(lastReceivedMessage);
        assertTrue(body.getText().contains("Someone wants to link your"),
                "The email should contain the account linking instructions");
        String verificationUrl = MailUtils.getPasswordResetEmailLink(body);

        // confirm the email using a different browser
        secondDriver.open(verificationUrl.trim());
        secondBrowserProceedPage.assertCurrent();
        secondBrowserProceedPage.clickProceedLink();

        // clear cookies to start a fresh browser instance and try to log in again
        webDriver.cookies().deleteAll();
        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillPassword(getUserPassword());
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(), "Broker login should complete successfully");
    }

    private void addEmailVerificationExecution(AuthenticationManagementResource authMgmtResource) {
        authMgmtResource.addExecution(DETECT_EXISTING_FLOW_ALIAS,
                Map.of("provider", IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID));

        AuthenticationExecutionInfoRepresentation addedExecution = authMgmtResource.getExecutions(DETECT_EXISTING_FLOW_ALIAS).stream()
                .filter(execution -> IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID.equals(execution.getProviderId()))
                .findFirst()
                .orElseThrow();

        addedExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        addedExecution.setPriority(15);
        authMgmtResource.updateExecutions(DETECT_EXISTING_FLOW_ALIAS, addedExecution);

        // revert the flow change once the test is done
        consumerRealm.cleanup().add(realm -> realm.flows().removeExecution(addedExecution.getId()));
    }
}
