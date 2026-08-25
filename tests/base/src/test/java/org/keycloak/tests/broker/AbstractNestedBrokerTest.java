package org.keycloak.tests.broker;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.tests.broker.BrokerTestTools.waitForPage;

@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public abstract class AbstractNestedBrokerTest extends AbstractBaseBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    protected NestedBrokerConfiguration nbc = getNestedBrokerConfiguration();

    protected abstract NestedBrokerConfiguration getNestedBrokerConfiguration();

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return getNestedBrokerConfiguration();
    }

    @BeforeEach
    public void createSubConsumerRealm() {
        importRealm(nbc.createSubConsumerRealm());
    }

    @AfterEach
    public void removeSubConsumerRealm() {
        adminClient.realm(nbc.subConsumerRealmName()).remove();
    }

    /** Logs in subconsumer realm via consumer IDP via provider IDP and updates account information */
    protected void logInAsUserInNestedIDPForFirstTime() {
        String redirectUri = getAuthServerRoot() + "realms/" + nbc.subConsumerRealmName() + "/account";
        oauth.client("account").redirectUri(redirectUri);
        oauth.realm(nbc.subConsumerRealmName());
        oauth.openLoginForm();

        waitForPage(driver, "sign in to", true);
        log.debug("Clicking social " + nbc.getSubConsumerIDPDisplayName());
        loginPage.clickSocial(nbc.getSubConsumerIDPDisplayName());
        waitForPage(driver, "sign in to", true);
        log.debug("Clicking social " + nbc.getIDPAlias());
        loginPage.clickSocial(nbc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        log.debug("Logging in");
        loginPage.login(nbc.getUserLogin(), nbc.getUserPassword());
    }
}
