package org.keycloak.testsuite.broker;

import java.util.Map;

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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.AuthenticationExecutionBuilder;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.SecondBrowser;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.AdminEventPaths.authAddExecutionPath;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KcOidcFirstBrokerLoginDetectExistingUserTest extends AbstractInitializedBaseBrokerTest {

    @Drone
    @SecondBrowser
    protected WebDriver driver2;

    @Rule
    public MailServer mail = new MailServer();

    private static final String DETECT_EXISTING_FLOW_ALIAS = "detectExistingUserFlow";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Override
    @Before
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        log.debug("creating detect existing user flow for realm " + bc.providerRealmName());

        final RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        AuthenticationManagementResource authMgmtResource = consumerRealm.flows();

        // Creates detectExistingUserFlow
        String detectExistingFlowAlias = DETECT_EXISTING_FLOW_ALIAS;
        final AuthenticationFlowRepresentation authenticationFlowRepresentation = newFlow(detectExistingFlowAlias, detectExistingFlowAlias, "basic-flow", true, false);
        authMgmtResource.createFlow(authenticationFlowRepresentation);

        AuthenticationFlowRepresentation authenticationFlowRepresentation1 = getFlow(authMgmtResource, detectExistingFlowAlias);
        assertNotNull(authenticationFlowRepresentation1, "The authentication flow must exist");

        String flowId = authenticationFlowRepresentation1.getId(); // retrieves the id of the newly created flow

        // Adds executions to the flow
        addExecution(authMgmtResource, flowId, IdpDetectExistingBrokerUserAuthenticatorFactory.PROVIDER_ID, 10);
        addExecution(authMgmtResource, flowId, IdpAutoLinkAuthenticatorFactory.PROVIDER_ID, 20);

        // Updates the FirstBrokerLoginFlowAlias for the identity provider
        IdentityProviderResource identityConsumerResource = consumerRealm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation identityProviderRepresentation = consumerRealm.identityProviders().findAll().get(0);
        identityProviderRepresentation.setFirstBrokerLoginFlowAlias(detectExistingFlowAlias);
        identityProviderRepresentation.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.toString());
        identityConsumerResource.update(identityProviderRepresentation);

        assertEquals(2, getFlow(authMgmtResource, detectExistingFlowAlias).getAuthenticationExecutions().size(), "Two executions must have been created");
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

    private AuthenticationFlowRepresentation getFlow(AuthenticationManagementResource authMgmtResource, String detectExistingFlowAlias) {
        return authMgmtResource.getFlows().stream()
                .filter(v -> detectExistingFlowAlias.equals(v.getAlias()))
                .findFirst().get();
    }


    private AuthenticationFlowRepresentation newFlow(String alias, String description,
                                             String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(alias);
        flow.setDescription(description);
        flow.setProviderId(providerId);
        flow.setTopLevel(topLevel);
        flow.setBuiltIn(builtIn);
        return flow;
    }

    @Test
    public void loginWhenUserDoesNotExistOnConsumer() {

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        String firstname = "Firstname";
        String lastname = "Lastname";
        String username = "firstandlastname";
        createUser(bc.providerRealmName(), username, BrokerTestConstants.USER_PASSWORD, firstname, lastname, "firstnamelastname@example.org");

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithIdp(bc.getIDPAlias(), username, BrokerTestConstants.USER_PASSWORD);

        errorPage.assertCurrent();

        assertEquals("User " +  username + " authenticated with identity provider " + bc.getIDPAlias() + " does not exist. Please contact your administrator.", loginPage.getInstruction());
    }

    @Test
    public void loginWhenUserExistsOnConsumer() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        final String firstname = "Firstname_loginWhenUserExistsOnConsumer";
        final String lastname = "Lastname_loginWhenUserExistsOnConsumer";
        final String username = "firstandlastname";
        final String email = "firstnamelastname@example.org";
        createUser(bc.providerRealmName(), username, BrokerTestConstants.USER_PASSWORD, firstname, lastname, email);
        createUser(bc.consumerRealmName(), username, "THIS PASSWORD IS USELESS", null, null, email);

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithIdp(bc.getIDPAlias(), username, BrokerTestConstants.USER_PASSWORD);

        assertTrue(driver.getTitle().contains("AUTH_RESPONSE"));
        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(
                adminClient.realm(bc.consumerRealmName()), username);

        assertEquals(userRepresentation.getEmail(), email, "Email is not correct");
        assertEquals(userRepresentation.getFirstName(), firstname, "Firstname is not correct");
        assertEquals(userRepresentation.getLastName(), lastname, "Lastname is not correct");
    }

    @Test
    public void testLinkAccountByEmailVerificationDifferentBrowser() {
        AuthenticationManagementResource authMgmtResource = adminClient.realm(bc.consumerRealmName()).flows();
        String emailVerificationExecutionId = addEmailVerificationExecution(authMgmtResource);

        try {
            RealmResource realm = adminClient.realm(bc.consumerRealmName());
            RealmRepresentation realmRep = realm.toRepresentation();

            realmRep.setVerifyEmail(true);

            realm.update(realmRep);

            IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();

            idpRep.setTrustEmail(false);

            identityProviderResource.update(idpRep);

            configureSMTPServer();

            // to avoid update profile required action
            RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
            UserRepresentation brokerUser = providerRealm.users().search(bc.getUserLogin()).get(0);
            brokerUser.setFirstName("f");
            brokerUser.setLastName("l");
            providerRealm.users().get(brokerUser.getId()).update(brokerUser);
            // creates a user in the consumer realm to link the account
            brokerUser.setId(null);
            realm.users().create(brokerUser).close();

            // link the account
            oauth.client("broker-app");
            oauth.realm(bc.consumerRealmName());
            oauth.openLoginForm();
            logInWithBroker(bc);
            String verificationUrl = assertEmailAndGetUrl(mail.getLastReceivedMessage(), MailServerConfiguration.FROM, USER_EMAIL,
                    "Someone wants to link your");
            assertNotNull(verificationUrl);

            // confirm the email using a different browser
            driver2.navigate().to(verificationUrl.trim());
            driver2.findElement(By.linkText("» Click here to proceed")).click();

            // clear cookies to start a fresh browser instance and try to log in again
            driver.manage().deleteAllCookies();
            oauth.realm(bc.consumerRealmName());
            oauth.openLoginForm();
            waitForPage(driver, "sign in to", true);
            loginPage.clickSocial(bc.getIDPAlias());
            waitForPage(driver, "sign in to", true);
            loginPage.login(bc.getUserPassword());
            Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        } finally {
            if (emailVerificationExecutionId != null) {
                authMgmtResource.removeExecution(emailVerificationExecutionId);
            }
        }
    }

    private String addEmailVerificationExecution(AuthenticationManagementResource authMgmtResource) {
        // Uses the same endpoint resolved by AdminEventPaths.authAddExecutionPath(flowAlias).
        String addExecutionPath = authAddExecutionPath(DETECT_EXISTING_FLOW_ALIAS);
        assertNotNull(addExecutionPath);

        authMgmtResource.addExecution(DETECT_EXISTING_FLOW_ALIAS, Map.of("provider", IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID));

        AuthenticationExecutionInfoRepresentation addedExecution = authMgmtResource.getExecutions(DETECT_EXISTING_FLOW_ALIAS).stream()
                .filter(execution -> IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID.equals(execution.getProviderId()))
                .findFirst()
                .orElse(null);

        if (addedExecution != null) {
            addedExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
            authMgmtResource.updateExecutions(DETECT_EXISTING_FLOW_ALIAS, addedExecution);
            return addedExecution.getId();
        }

        return null;
    }

}
