package org.keycloak.testsuite.broker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;

public class KcOidcBrokerLogoutTest extends AbstractBaseBrokerTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + bc.providerRealmName());

        final UserRepresentation user = new UserRepresentation();
        user.setUsername(bc.getUserLogin());
        user.setEmail(bc.getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        final RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        final String userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), bc.getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + bc.consumerRealmName());

        final RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().create(bc.setUpIdentityProvider()).close();
    }

    @Before
    public void addClients() {
        addClientsToProviderAndConsumer();
    }

    @Test
    public void logoutWithoutInitiatingIdpLogsOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));
        waitForPage(driver, "sign in to provider", true);
    }

    @Test
    public void logoutWithActualIdpAsInitiatingIdpDoesNotLogOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName(), "kc-oidc-idp");
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));

        waitForAccountManagementTitle();
    }

    @Test
    public void logoutWithOtherIdpAsInitiatinIdpLogsOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName(), "something-else");
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));
        waitForPage(driver, "sign in to provider", true);
    }

    @Test
    public void logoutAfterBrowserRestart() {
        driver.navigate().to(getLoginUrl(getConsumerRoot(), bc.consumerRealmName(), "broker-app"));
        logInWithBroker(bc);
        updateAccountInformation();

        // Exchange code from "broker-app" client of "consumer" realm for the tokens
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.realm(bc.consumerRealmName())
                .clientId("broker-app")
                .redirectUri(getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/app")
                .doAccessTokenRequest(code, "broker-app-secret");
        assertEquals(200, response.getStatusCode());

        String idToken = response.getIdToken();

        // simulate browser restart by deleting an identity cookie
        log.debugf("Deleting %s and %s cookies", AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE,
                AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE + CookieHelper.LEGACY_COOKIE);
        driver.manage().deleteCookieNamed(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE);
        driver.manage().deleteCookieNamed(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE + CookieHelper.LEGACY_COOKIE);

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName(), null, idToken);
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));

        waitForPage(driver, "sign in to provider", true);
    }
}
