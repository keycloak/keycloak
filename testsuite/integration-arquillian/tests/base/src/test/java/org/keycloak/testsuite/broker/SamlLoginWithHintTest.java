package org.keycloak.testsuite.broker;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPagePrefix;


public class SamlLoginWithHintTest extends AbstractBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE_PASS_LOGIN_HINT;
    }

    @Before
    public void addIdentityProviderToConsumerRealm() {
        log.debug("adding identity provider to realm " + bc.consumerRealmName());
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        IdentityProviderRepresentation idp = bc.setUpIdentityProvider(suiteContext);
        realm.identityProviders().create(idp).close();
    }

    @Test
    public void testLoginWithHint() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        addLoginHintOnSocialButton("test@acme.com");
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPagePrefix(driver, getProviderUrlPrefix());

        assertTrue("Url should contain a 'login_hint' query parameter",
                driver.getCurrentUrl().contains("login_hint=test%40acme.com"));
        assertTrue("Url should contain a 'username' query parameter",
                driver.getCurrentUrl().contains("username=test%40acme.com"));
    }

    @Test
    public void testLoginWithoutHint() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPagePrefix(driver, getProviderUrlPrefix());

        assertFalse("Url should not contain a 'login_hint' query parameter",
                driver.getCurrentUrl().contains("login_hint"));
        assertFalse("Url should not contain a 'username' query parameter",
                driver.getCurrentUrl().contains("username"));
    }

    private String getProviderUrlPrefix() {
        return getAuthServerRoot() + "realms/provider/protocol/saml";
    }

    private void addLoginHintOnSocialButton(String hint) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        WebElement button = loginPage.findSocialButton(bc.getIDPAlias());
        String url = button.getAttribute("href") + "&login_hint="+hint;
        executor.executeScript("document.getElementById('"+button.getAttribute("id")+"').setAttribute('href', '"+url+"')");
    }
}
