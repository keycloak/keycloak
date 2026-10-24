package org.keycloak.tests.broker;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.broker.IdpAutoLinkAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpConfirmOverrideLinkAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpDetectExistingBrokerUserAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpEmailVerificationAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
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
import org.keycloak.testframework.ui.page.IdpConfirmOverrideLinkPage;
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

    @InjectPage
    IdpConfirmOverrideLinkPage idpConfirmOverrideLinkPage;

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

    /**
     * The link on the consumer user holds the provider user ID in a different case than the ID the
     * provider sends, so the returning user is not found and the first-broker-login flow links the
     * existing user by email again. The login then fails with the already-linked error, and the
     * events must carry the ID as sent by the provider and the ID the user is already linked with,
     * so the conflict can be diagnosed from the events instead of the server log.
     */
    @Test
    public void loginWhenIncomingFederatedUserIdDiffersOnlyInCase() {
        disableUpdateProfileOnFirstLogin();
        consumerRealm.updateWithCleanup(r -> r.eventsEnabled(true));

        final String username = "casedrift";
        final String email = "casedrift@example.org";
        createUser(providerRealm, username, USER_PASSWORD, "CaseDrift", "Test", email);

        // The OIDC sub sent by the provider is the provider user ID.
        UserRepresentation providerUser = providerRealm.admin().users().search(username).get(0);
        String incomingFederatedUserId = providerUser.getId();

        String consumerUserId;
        try (Response response = consumerRealm.admin().users().create(UserBuilder.create(username)
                .email(email)
                .password("THIS PASSWORD IS USELESS")
                .enabled(true)
                .build())) {
            assertEquals(201, response.getStatus());
            consumerUserId = ApiUtil.getCreatedId(response);
            consumerRealm.cleanup().add(r -> r.users().get(consumerUserId).remove());
        }

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setIdentityProvider(IDP_OIDC_ALIAS);
        link.setUserId(incomingFederatedUserId.toUpperCase());
        link.setUserName(username);
        try (Response response = consumerRealm.admin().users().get(consumerUserId).addFederatedIdentity(IDP_OIDC_ALIAS, link)) {
            assertEquals(204, response.getStatus());
        }

        logInWithIdp(username, USER_PASSWORD);

        errorPage.assertCurrent();
        assertTrue(errorPage.getError().contains("Your account is already linked to the identity provider"),
                "Unexpected error: " + errorPage.getError());

        List<EventRepresentation> events = consumerRealm.admin().getEvents();

        EventRepresentation firstBrokerLogin = findEvent(events, EventType.IDENTITY_PROVIDER_FIRST_LOGIN.name());
        assertEquals(incomingFederatedUserId, firstBrokerLogin.getDetails().get(Details.IDENTITY_PROVIDER_USER_ID),
                "The first-broker-login event should carry the federated user ID as sent by the provider");

        EventRepresentation loginError = findEvent(events, EventType.IDENTITY_PROVIDER_LOGIN_ERROR.name());
        assertEquals(Errors.IDENTITY_PROVIDER_ERROR, loginError.getError());
        assertEquals(incomingFederatedUserId, loginError.getDetails().get(Details.IDENTITY_PROVIDER_USER_ID),
                "The login error event should carry the federated user ID as sent by the provider");
        assertEquals(incomingFederatedUserId.toUpperCase(),
                loginError.getDetails().get(Details.PREF_PREVIOUS + Details.IDENTITY_PROVIDER_USER_ID),
                "The login error event should carry the ID the user is already linked with");
    }

    /**
     * The link on the consumer user holds the provider user ID in a different case than the ID the
     * provider sends, and the user confirms overriding the existing link. The events must then carry
     * the ID of the removed link as well, as the previous username alone does not reveal an ID that
     * differs from the incoming one only in case.
     */
    @Test
    public void loginWhenIncomingFederatedUserIdDiffersOnlyInCaseAndOverrideIsConfirmed() {
        disableUpdateProfileOnFirstLogin();
        consumerRealm.updateWithCleanup(r -> r.eventsEnabled(true));
        addConfirmOverrideLinkExecution(consumerRealm.admin().flows());

        final String username = "casedriftoverride";
        final String email = "casedriftoverride@example.org";
        createUser(providerRealm, username, USER_PASSWORD, "CaseDrift", "Override", email);

        // The OIDC sub sent by the provider is the provider user ID.
        UserRepresentation providerUser = providerRealm.admin().users().search(username).get(0);
        String incomingFederatedUserId = providerUser.getId();

        String consumerUserId;
        try (Response response = consumerRealm.admin().users().create(UserBuilder.create(username)
                .email(email)
                .password("THIS PASSWORD IS USELESS")
                .enabled(true)
                .build())) {
            assertEquals(201, response.getStatus());
            consumerUserId = ApiUtil.getCreatedId(response);
            consumerRealm.cleanup().add(r -> r.users().get(consumerUserId).remove());
        }

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setIdentityProvider(IDP_OIDC_ALIAS);
        link.setUserId(incomingFederatedUserId.toUpperCase());
        link.setUserName(username);
        try (Response response = consumerRealm.admin().users().get(consumerUserId).addFederatedIdentity(IDP_OIDC_ALIAS, link)) {
            assertEquals(204, response.getStatus());
        }

        logInWithIdp(username, USER_PASSWORD);

        idpConfirmOverrideLinkPage.assertCurrent();
        idpConfirmOverrideLinkPage.clickConfirmOverride();

        assertTrue(oauth.parseLoginResponse().isSuccess(), "Broker login should complete successfully");

        List<EventRepresentation> events = consumerRealm.admin().getEvents();

        EventRepresentation overrideLink = findEvent(events, EventType.FEDERATED_IDENTITY_OVERRIDE_LINK.name());
        assertEquals(username,
                overrideLink.getDetails().get(Details.PREF_PREVIOUS + Details.IDENTITY_PROVIDER_USERNAME),
                "The override-link event should carry the username of the removed link");
        assertEquals(incomingFederatedUserId.toUpperCase(),
                overrideLink.getDetails().get(Details.PREF_PREVIOUS + Details.IDENTITY_PROVIDER_USER_ID),
                "The override-link event should carry the ID of the removed link");

        EventRepresentation login = findEvent(events, EventType.LOGIN.name());
        assertEquals(incomingFederatedUserId, login.getDetails().get(Details.IDENTITY_PROVIDER_USER_ID),
                "The login event should carry the federated user ID as sent by the provider");
        assertEquals(incomingFederatedUserId.toUpperCase(),
                login.getDetails().get(Details.PREF_PREVIOUS + Details.IDENTITY_PROVIDER_USER_ID),
                "The login event should carry the ID of the removed link");

        // The override replaced the link, so the user is now linked with the ID as sent by the provider.
        FederatedIdentityRepresentation updatedLink = consumerRealm.admin().users().get(consumerUserId)
                .getFederatedIdentity().stream()
                .filter(fi -> IDP_OIDC_ALIAS.equals(fi.getIdentityProvider()))
                .findFirst().orElseThrow();
        assertEquals(incomingFederatedUserId, updatedLink.getUserId(),
                "The confirmed override should replace the stored link");
    }

    private void addConfirmOverrideLinkExecution(AuthenticationManagementResource authMgmtResource) {
        authMgmtResource.addExecution(DETECT_EXISTING_FLOW_ALIAS,
                Map.of("provider", IdpConfirmOverrideLinkAuthenticatorFactory.PROVIDER_ID));

        AuthenticationExecutionInfoRepresentation addedExecution = authMgmtResource.getExecutions(DETECT_EXISTING_FLOW_ALIAS).stream()
                .filter(execution -> IdpConfirmOverrideLinkAuthenticatorFactory.PROVIDER_ID.equals(execution.getProviderId()))
                .findFirst()
                .orElseThrow();

        addedExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        addedExecution.setPriority(15);
        authMgmtResource.updateExecutions(DETECT_EXISTING_FLOW_ALIAS, addedExecution);

        // revert the flow change once the test is done
        consumerRealm.cleanup().add(realm -> realm.flows().removeExecution(addedExecution.getId()));
    }

    private EventRepresentation findEvent(List<EventRepresentation> events, String type) {
        return events.stream()
                .filter(e -> type.equals(e.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected event " + type + ", but events were: " + events));
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
