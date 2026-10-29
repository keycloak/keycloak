package org.keycloak.tests.broker.oidc;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.actiontoken.idpverifyemail.IdpVerifyAccountLinkActionTokenHandler;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.FederatedIdentityBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.IdpConfirmLinkPage;
import org.keycloak.testframework.ui.page.IdpLinkEmailPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.ProceedPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.MailServerConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2026-92358: cross-browser account-link SUO proofs must be consumed on successful link and
 * revoked on Account self-service unlink.
 */
@KeycloakIntegrationTest
public class KcOidcFirstBrokerLoginLinkProofTest extends AbstractKcOidcBrokerTest {

    private static final String CONSUMER_USERNAME = "consumer";

    @InjectMailServer
    MailServer mailServer;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectWebDriver(ref = "driver2", lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver2;

    @InjectPage
    IdpConfirmLinkPage idpConfirmLinkPage;

    @InjectPage
    IdpLinkEmailPage idpLinkEmailPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage(ref = "proceedPage2", webDriverRef = "driver2")
    ProceedPage proceedPage2;

    @InjectPage(ref = "infoPage2", webDriverRef = "driver2")
    InfoPage infoPage2;

    @BeforeEach
    void configureSmtp() {
        getConsumerRealm().updateWithCleanup(r -> r.smtp(
                MailServerConfiguration.HOST,
                Integer.parseInt(MailServerConfiguration.PORT),
                MailServerConfiguration.FROM));
    }

    @Test
    public void testCrossBrowserLinkProofNotReusableAfterAccountUnlink() throws Exception {
        UserResource userResource = createConsumerWithPassword();
        String consumerUserId = userResource.toRepresentation().getId();

        startBrokerLinkUntilEmailSent();
        confirmLinkInSecondBrowser();

        assertTrue(isLinkProofPresent(consumerUserId), "Cross-browser confirmation should create the SUO proof");

        idpLinkEmailPage.continueLink();
        waitForFederatedIdentity(userResource, 1);
        assertFalse(isLinkProofPresent(consumerUserId),
                "Original-session continuation must consume the SUO proof before unlink runs");

        clearRequiredActions(userResource);
        removeLinkedAccountViaAccountApi();
        assertEquals(0, userResource.getFederatedIdentity().size());

        AccountHelper.logout(getConsumerRealm().admin(), CONSUMER_USERNAME);
        AccountHelper.logout(getProviderRealm().admin(), getUserLogin());
        webDriver.cookies().deleteAll();

        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
        updateAccountInformation();

        idpConfirmLinkPage.assertCurrent();
        assertFalse(AccountHelper.isIdentityProviderLinked(getConsumerRealm().admin(), CONSUMER_USERNAME, getIdpAlias()));
    }

    @Test
    public void testOutstandingLinkProofRevokedByAccountUnlink() throws Exception {
        UserResource userResource = createConsumerWithPassword();
        String consumerUserId = userResource.toRepresentation().getId();

        startBrokerLinkUntilEmailSent();
        confirmLinkInSecondBrowser();

        assertTrue(isLinkProofPresent(consumerUserId));

        String federatedUserId = getProviderRealm().admin().users().search(getUserLogin(), true).get(0).getId();
        FederatedIdentityRepresentation identity = FederatedIdentityBuilder.create()
                .userId(federatedUserId)
                .userName(getUserLogin())
                .identityProvider(getIdpAlias())
                .build();
        try (Response response = userResource.addFederatedIdentity(getIdpAlias(), identity)) {
            assertEquals(204, response.getStatus());
        }
        assertEquals(1, userResource.getFederatedIdentity().size());

        clearRequiredActions(userResource);
        removeLinkedAccountViaAccountApi();
        assertEquals(0, userResource.getFederatedIdentity().size());
        assertFalse(isLinkProofPresent(consumerUserId),
                "Account unlink must revoke the outstanding SUO proof");

        AccountHelper.logout(getConsumerRealm().admin(), CONSUMER_USERNAME);
        AccountHelper.logout(getProviderRealm().admin(), getUserLogin());
        webDriver.cookies().deleteAll();

        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
        updateAccountInformation();

        idpConfirmLinkPage.assertCurrent();
        assertFalse(AccountHelper.isIdentityProviderLinked(getConsumerRealm().admin(), CONSUMER_USERNAME, getIdpAlias()));
    }

    @Test
    public void testOutstandingLinkProofRevokedWhenFederatedIdentityRemovedDirectly() throws Exception {
        UserResource userResource = createConsumerWithPassword();
        String consumerUserId = userResource.toRepresentation().getId();

        startBrokerLinkUntilEmailSent();
        confirmLinkInSecondBrowser();

        assertTrue(isLinkProofPresent(consumerUserId));

        String federatedUserId = getProviderRealm().admin().users().search(getUserLogin(), true).get(0).getId();
        FederatedIdentityRepresentation identity = FederatedIdentityBuilder.create()
                .userId(federatedUserId)
                .userName(getUserLogin())
                .identityProvider(getIdpAlias())
                .build();
        try (Response response = userResource.addFederatedIdentity(getIdpAlias(), identity)) {
            assertEquals(204, response.getStatus());
        }
        assertEquals(1, userResource.getFederatedIdentity().size());

        // Remove the federated identity through the model layer directly, bypassing any REST endpoint:
        // the FederatedIdentityRemovedEvent listener must still revoke the outstanding proof.
        String idpAlias = getIdpAlias();
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel consumer = session.users().getUserById(realm, consumerUserId);
            session.users().removeFederatedIdentity(realm, consumer, idpAlias);
        });

        assertEquals(0, userResource.getFederatedIdentity().size());
        assertFalse(isLinkProofPresent(consumerUserId),
                "Removing the federated identity via the model layer must revoke the outstanding SUO proof");
    }

    @Test
    public void testOriginalSessionFailsWhenConcurrentLoginConsumesProof() throws Exception {
        UserResource userResource = createConsumerWithPassword();
        String consumerUserId = userResource.toRepresentation().getId();

        startBrokerLinkUntilEmailSent();
        confirmLinkInSecondBrowser();

        assertTrue(isLinkProofPresent(consumerUserId));

        // A concurrent fresh broker login reaches IdpCreateUserIfUniqueAuthenticator and wins the
        // atomic consume through the production path; this caller represents that winning login.
        assertTrue(consumeLinkProofViaConcurrentLogin(consumerUserId),
                "The concurrent login must win the atomic proof consume");
        assertFalse(isLinkProofPresent(consumerUserId));
        // The proof is single-use: the original session (the losing consumer) must not consume it again.
        assertFalse(consumeLinkProofViaConcurrentLogin(consumerUserId),
                "The account-link proof must be consumable only once");

        idpLinkEmailPage.continueLink();
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), containsString("no longer valid"));
        assertEquals(0, userResource.getFederatedIdentity().size());
    }

    private void startBrokerLinkUntilEmailSent() {
        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
        updateAccountInformation();

        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();
        idpLinkEmailPage.assertCurrent();
    }

    private void confirmLinkInSecondBrowser() throws IOException {
        assertTrue(mailServer.waitForIncomingEmail(1));
        String url = MailUtils.getPasswordResetEmailLink(mailServer.getLastReceivedMessage());

        driver2.open(url);
        proceedPage2.assertCurrent();
        proceedPage2.clickProceedLink();
        infoPage2.assertCurrent();
        assertThat(infoPage2.getInfo(), startsWith("You successfully confirmed linking your account"));
    }

    private UserResource createConsumerWithPassword() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(CONSUMER_USERNAME);
        user.setEmail(getUserEmail());
        user.setFirstName("Consumer");
        user.setLastName("User");
        user.setEmailVerified(true);
        user.setEnabled(true);
        String userId = ApiUtil.getCreatedId(getConsumerRealm().admin().users().create(user));

        UserResource userResource = getConsumerRealm().admin().users().get(userId);
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
        webDriver.waiting().until(driver -> {
            try {
                return userResource.getFederatedIdentity().size() == expectedCount ? true : null;
            } catch (RuntimeException ex) {
                return null;
            }
        });
    }

    private String linkProofKey(String consumerUserId) {
        String federatedUserId = getProviderRealm().admin().users().search(getUserLogin(), true).get(0).getId();
        return "kc.brokering.user.verified." + consumerUserId + "." + getIdpAlias() + "." + federatedUserId;
    }

    private boolean isLinkProofPresent(String consumerUserId) {
        String key = linkProofKey(consumerUserId);
        return runOnServer.fetch(session -> session.singleUseObjects().contains(key), Boolean.class);
    }

    private boolean consumeLinkProofViaConcurrentLogin(String consumerUserId) {
        String idpAlias = getIdpAlias();
        String federatedUserId = getProviderRealm().admin().users().search(getUserLogin(), true).get(0).getId();
        return runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel consumer = session.users().getUserById(realm, consumerUserId);
            IdentityProviderModel broker = session.identityProviders().getByAlias(idpAlias);
            return IdpVerifyAccountLinkActionTokenHandler.runIfUserVerified(session, consumer, broker, federatedUserId, () -> {
            });
        }, Boolean.class);
    }

    private void removeLinkedAccountViaAccountApi() throws IOException {
        String token = oauth.doPasswordGrantRequest(CONSUMER_USERNAME, "password").getAccessToken();
        String accountUrl = getConsumerRealm().getBaseUrl() + "/account/linked-accounts/" + getIdpAlias();
        try (var response = simpleHttp.doDelete(accountUrl).auth(token).acceptJson().asResponse()) {
            assertEquals(204, response.getStatus(), response.asString());
        }
    }
}
