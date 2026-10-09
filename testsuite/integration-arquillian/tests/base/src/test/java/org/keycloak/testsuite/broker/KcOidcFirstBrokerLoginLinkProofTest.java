package org.keycloak.testsuite.broker;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.actiontoken.idpverifyemail.IdpVerifyAccountLinkActionTokenHandler;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FederatedIdentityBuilder;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.SecondBrowser;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * CVE-2026-92358: cross-browser account-link SUO proofs must be consumed on successful link and
 * revoked on Account self-service unlink.
 */
public class KcOidcFirstBrokerLoginLinkProofTest extends AbstractInitializedBaseBrokerTest {

    private static final String CONSUMER_USERNAME = "consumer";

    @Drone
    @SecondBrowser
    protected WebDriver driver2;

    private CloseableHttpClient httpClient;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    private void setUp() throws Exception {
        httpClient = HttpClientBuilder.create().build();
        configureSMTPServer();
    }

    @After
    public void tearDown() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Test
    public void testCrossBrowserLinkProofNotReusableAfterAccountUnlink() throws Exception {
        setUp();
        UserResource userResource = createConsumerWithPassword();
        String consumerUserId = userResource.toRepresentation().getId();

        startBrokerLinkUntilEmailSent();
        confirmLinkInSecondBrowser();

        assertTrue("Cross-browser confirmation should create the SUO proof",
                isLinkProofPresent(consumerUserId));

        idpLinkEmailPage.continueLink();
        waitForFederatedIdentity(userResource, 1);
        assertFalse("Original-session continuation must consume the SUO proof before unlink runs",
                isLinkProofPresent(consumerUserId));

        clearRequiredActions(userResource);
        removeLinkedAccountViaAccountApi();
        assertEquals(0, userResource.getFederatedIdentity().size());

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), CONSUMER_USERNAME);
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());
        driver.manage().deleteAllCookies();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        updateAccountInformation();

        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        assertFalse(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), CONSUMER_USERNAME, bc.getIDPAlias()));
    }

    @Test
    public void testOutstandingLinkProofRevokedByAccountUnlink() throws Exception {
        setUp();
        UserResource userResource = createConsumerWithPassword();
        String consumerUserId = userResource.toRepresentation().getId();

        startBrokerLinkUntilEmailSent();
        confirmLinkInSecondBrowser();

        assertTrue(isLinkProofPresent(consumerUserId));

        String federatedUserId = adminClient.realm(bc.providerRealmName()).users().search(bc.getUserLogin(), true).get(0).getId();
        FederatedIdentityRepresentation identity = FederatedIdentityBuilder.create()
                .userId(federatedUserId)
                .userName(bc.getUserLogin())
                .identityProvider(bc.getIDPAlias())
                .build();
        try (Response response = userResource.addFederatedIdentity(bc.getIDPAlias(), identity)) {
            assertEquals(204, response.getStatus());
        }
        assertEquals(1, userResource.getFederatedIdentity().size());

        clearRequiredActions(userResource);
        removeLinkedAccountViaAccountApi();
        assertEquals(0, userResource.getFederatedIdentity().size());
        assertFalse("Account unlink must revoke the outstanding SUO proof",
                isLinkProofPresent(consumerUserId));

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), CONSUMER_USERNAME);
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());
        driver.manage().deleteAllCookies();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        updateAccountInformation();

        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        assertFalse(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), CONSUMER_USERNAME, bc.getIDPAlias()));
    }

    @Test
    public void testOutstandingLinkProofRevokedWhenFederatedIdentityRemovedDirectly() throws Exception {
        setUp();
        UserResource userResource = createConsumerWithPassword();
        String consumerUserId = userResource.toRepresentation().getId();

        startBrokerLinkUntilEmailSent();
        confirmLinkInSecondBrowser();

        assertTrue(isLinkProofPresent(consumerUserId));

        String federatedUserId = adminClient.realm(bc.providerRealmName()).users().search(bc.getUserLogin(), true).get(0).getId();
        FederatedIdentityRepresentation identity = FederatedIdentityBuilder.create()
                .userId(federatedUserId)
                .userName(bc.getUserLogin())
                .identityProvider(bc.getIDPAlias())
                .build();
        try (Response response = userResource.addFederatedIdentity(bc.getIDPAlias(), identity)) {
            assertEquals(204, response.getStatus());
        }
        assertEquals(1, userResource.getFederatedIdentity().size());

        String idpAlias = bc.getIDPAlias();
        testingClient.server(bc.consumerRealmName()).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel consumer = session.users().getUserById(realm, consumerUserId);
            session.users().removeFederatedIdentity(realm, consumer, idpAlias);
        });

        assertEquals(0, userResource.getFederatedIdentity().size());
        assertFalse("Removing the federated identity via the model layer must revoke the outstanding SUO proof",
                isLinkProofPresent(consumerUserId));
    }

    @Test
    public void testOriginalSessionFailsWhenConcurrentLoginConsumesProof() throws Exception {
        setUp();
        UserResource userResource = createConsumerWithPassword();
        String consumerUserId = userResource.toRepresentation().getId();

        startBrokerLinkUntilEmailSent();
        confirmLinkInSecondBrowser();

        assertTrue(isLinkProofPresent(consumerUserId));

        assertTrue("The concurrent login must win the atomic proof consume",
                consumeLinkProofViaConcurrentLogin(consumerUserId));
        assertFalse(isLinkProofPresent(consumerUserId));
        assertFalse("The account-link proof must be consumable only once",
                consumeLinkProofViaConcurrentLogin(consumerUserId));

        idpLinkEmailPage.continueLink();
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), containsString("no longer valid"));
        assertEquals(0, userResource.getFederatedIdentity().size());
    }

    private void startBrokerLinkUntilEmailSent() {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        idpConfirmLinkPage.clickLinkAccount();
        idpLinkEmailPage.assertCurrent();
    }

    private void confirmLinkInSecondBrowser() throws IOException {
        String url = getEmailLink();

        driver2.navigate().to(url);
        assertThat(driver2.findElement(By.id("kc-page-title")).getText(), startsWith("Confirm linking the account"));
        driver2.findElement(By.linkText("» Click here to proceed")).click();
        assertThat(driver2.findElement(By.className("instruction")).getText(),
                startsWith("You successfully verified your email"));
    }

    private String getEmailLink() {
        try {
            return MailUtils.getPasswordResetEmailLink(MailServer.getLastReceivedMessage());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get email link", e);
        }
    }

    private UserResource createConsumerWithPassword() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(CONSUMER_USERNAME);
        user.setEmail(bc.getUserEmail());
        user.setFirstName("Consumer");
        user.setLastName("User");
        user.setEmailVerified(true);
        user.setEnabled(true);
        String createdUserId;
        try (Response response = adminClient.realm(bc.consumerRealmName()).users().create(user)) {
            createdUserId = org.keycloak.testsuite.admin.ApiUtil.getCreatedId(response);
        }

        UserResource userResource = adminClient.realm(bc.consumerRealmName()).users().get(createdUserId);
        clearRequiredActions(userResource);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("password");
        cred.setTemporary(false);
        userResource.resetPassword(cred);
        return userResource;
    }

    private void clearRequiredActions(UserResource userResource) {
        UserRepresentation rep = userResource.toRepresentation();
        rep.setEmailVerified(true);
        rep.setRequiredActions(List.of());
        userResource.update(rep);
    }

    private void waitForFederatedIdentity(UserResource userResource, int expectedCount) {
        int attempts = 0;
        while (attempts < 30) {
            try {
                if (userResource.getFederatedIdentity().size() == expectedCount) {
                    return;
                }
            } catch (RuntimeException ignored) {
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            attempts++;
        }
        assertEquals(expectedCount, userResource.getFederatedIdentity().size());
    }

    private String linkProofKey(String consumerUserId) {
        String federatedUserId = adminClient.realm(bc.providerRealmName()).users().search(bc.getUserLogin(), true).get(0).getId();
        return "kc.brokering.user.verified." + consumerUserId + "." + bc.getIDPAlias() + "." + federatedUserId;
    }

    private boolean isLinkProofPresent(String consumerUserId) {
        String key = linkProofKey(consumerUserId);
        return testingClient.server(bc.consumerRealmName()).fetch(session ->
                session.singleUseObjects().contains(key), Boolean.class);
    }

    private boolean consumeLinkProofViaConcurrentLogin(String consumerUserId) {
        String idpAlias = bc.getIDPAlias();
        String federatedUserId = adminClient.realm(bc.providerRealmName()).users().search(bc.getUserLogin(), true).get(0).getId();
        return testingClient.server(bc.consumerRealmName()).fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel consumer = session.users().getUserById(realm, consumerUserId);
            IdentityProviderModel broker = session.identityProviders().getByAlias(idpAlias);
            return IdpVerifyAccountLinkActionTokenHandler.runIfUserVerified(session, consumer, broker, federatedUserId, () -> {
            });
        }, Boolean.class);
    }

    private void removeLinkedAccountViaAccountApi() throws IOException {
        String token = oauth.realm(bc.consumerRealmName())
                .client(KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_CLIENT_ID, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET)
                .doPasswordGrantRequest(CONSUMER_USERNAME, "password").getAccessToken();
        String accountUrl = getConsumerRoot() + "/auth/realms/" + bc.consumerRealmName() + "/account/linked-accounts/" + bc.getIDPAlias();
        try (var response = SimpleHttpDefault.doDelete(accountUrl, httpClient).auth(token).acceptJson().asResponse()) {
            assertEquals(204, response.getStatus());
        }
    }
}
