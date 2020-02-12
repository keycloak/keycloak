package org.keycloak.testsuite.broker;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.util.ReverseProxy;

public final class KcOidcBrokerFrontendUrlTest extends AbstractBrokerTest {

    @Rule
    public ReverseProxy proxy = new ReverseProxy();

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override 
            public RealmRepresentation createConsumerRealm() {
                RealmRepresentation realm = super.createConsumerRealm();

                Map<String, String> attributes = new HashMap<>();

                attributes.put("frontendUrl", proxy.getUrl());

                realm.setAttributes(attributes);
                
                return realm;
            }

            @Override 
            public List<ClientRepresentation> createProviderClients(SuiteContext suiteContext) {
                List<ClientRepresentation> clients = super.createProviderClients(suiteContext);

                List<String> redirectUris = new ArrayList<>();

                redirectUris.add(proxy.getUrl() + "/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*");

                clients.get(0).setRedirectUris(redirectUris);
                
                return clients;
            }
        };
    }

    @Test
    @Override 
    public void testLogInAsUserInIDP() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.consumerRealmName(), "consumer", "password", "FirstName", "LastName", "consumer@localhost.com");

        driver.navigate().to(proxy.getUrl() + "/realms/consumer/account");
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "log in to", true);
        log.debug("Logging in");

        // make sure the frontend url is used to build the redirect uri when redirecting to the broker
        try {
            assertTrue(driver.getCurrentUrl().contains("redirect_uri=" + URLEncoder.encode(proxy.getUrl(), "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        waitForPage(driver, "keycloak account management", true);
        accountUpdateProfilePage.assertCurrent();
    }

    @Ignore
    @Test
    @Override
    public void loginWithExistingUser() {
        // no-op
    }
}
