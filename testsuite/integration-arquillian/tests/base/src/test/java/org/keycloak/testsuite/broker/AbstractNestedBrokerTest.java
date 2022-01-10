package org.keycloak.testsuite.broker;

import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import org.junit.After;
import org.junit.Before;

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
        driver.navigate().to(getAccountUrl(getConsumerRoot(), nbc.subConsumerRealmName()));
        waitForPage(driver, "sign in to", true);
        log.debug("Clicking social " + nbc.getSubConsumerIDPDisplayName());
        loginPage.clickSocial(nbc.getSubConsumerIDPDisplayName());
        waitForPage(driver, "sign in to", true);
        log.debug("Clicking social " + nbc.getIDPAlias());
        loginPage.clickSocial(nbc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        log.debug("Logging in");
        loginPage.login(nbc.getUserLogin(), nbc.getUserPassword());

        waitForPage(driver, "update account information", false);
        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname",
                "Lastname");
    }
}
