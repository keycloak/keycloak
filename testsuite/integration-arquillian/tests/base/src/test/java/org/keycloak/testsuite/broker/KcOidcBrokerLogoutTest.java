package org.keycloak.testsuite.broker;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.openqa.selenium.Cookie;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public class KcOidcBrokerLogoutTest extends AbstractBaseBrokerTest {
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
        realm.identityProviders().create(bc.setUpIdentityProvider(suiteContext)).close();
    }

    @Before
    public void addClients() {
        final List<ClientRepresentation> clients = bc.createProviderClients(suiteContext);
        final RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        for (final ClientRepresentation client : clients) {
            log.debug("adding client " + client.getClientId() + " to realm " + bc.providerRealmName());

            final Response resp = providerRealm.clients().create(client);
            resp.close();
        }
    }

    @Test
    public void logoutWithoutInitiatingIdpLogsOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(bc.consumerRealmName());
        driver.navigate().to(getAccountUrl(REALM_PROV_NAME));
        waitForPage(driver, "log in to provider", true);
    }

    @Test
    public void logoutWithActualIdpAsInitiatingIdpDoesNotLogOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(bc.consumerRealmName(), "kc-oidc-idp");
        driver.navigate().to(getAccountUrl(REALM_PROV_NAME));

        waitForAccountManagementTitle();
    }

    @Test
    public void logoutWithOtherIdpAsInitiatinIdpLogsOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(bc.consumerRealmName(), "something-else");
        driver.navigate().to(getAccountUrl(REALM_PROV_NAME));
        waitForPage(driver, "log in to provider", true);
    }

    @Test
    public void logoutAfterBrowserRestart() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        Cookie identityCookie = driver.manage().getCookieNamed(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE);
        String idToken = identityCookie.getValue();

        // simulate browser restart by deleting an identity cookie
        log.debugf("Deleting %s cookie", AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE);
        driver.manage().deleteCookieNamed(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE);

        logoutFromRealm(bc.consumerRealmName(), null, idToken);
        driver.navigate().to(getAccountUrl(REALM_PROV_NAME));

        waitForPage(driver, "log in to provider", true);
    }
}
