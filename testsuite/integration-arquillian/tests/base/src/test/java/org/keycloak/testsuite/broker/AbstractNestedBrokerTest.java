package org.keycloak.testsuite.broker;

import org.junit.After;
import org.junit.Before;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public abstract class AbstractNestedBrokerTest extends AbstractBaseBrokerTest {

    protected NestedBrokerConfiguration nbc = getNestedBrokerConfiguration();

    protected abstract NestedBrokerConfiguration getNestedBrokerConfiguration();

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return getNestedBrokerConfiguration();
    }

    @Before
    public void createSubConsumerRealm() {
        importRealm(nbc.createSubConsumerRealm());
    }

    @After
    public void removeSubConsumerRealm() {
        adminClient.realm(nbc.subConsumerRealmName()).remove();
    }

    /** Logs in subconsumer realm via consumer IDP via provider IDP and updates account information */
    protected void logInAsUserInNestedIDPForFirstTime() {
        String redirectUri = getAuthServerRoot() + "realms/" + nbc.subConsumerRealmName() + "/account";
        oauth.clientId("account").redirectUri(redirectUri);
        loginPage.open(nbc.subConsumerRealmName());

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
