package org.keycloak.testsuite.broker;

import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.broker.oidc.TestKeycloakOidcIdentityProviderFactory;

import org.junit.Ignore;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * Tests the propagation of the max_age parameter for brokered logins.
 *
 * see https://issues.redhat.com/browse/KEYCLOAK-18499
 */
public class KcOidcBrokerPassMaxAgeTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationWithPassMaxAge();
    }

    private static class KcOidcBrokerConfigurationWithPassMaxAge extends KcOidcBrokerConfiguration {
        
        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, TestKeycloakOidcIdentityProviderFactory.ID);

            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(config, syncMode);
            config.put(IdentityProviderModel.LOGIN_HINT, "false");
            config.put(IdentityProviderModel.PASS_MAX_AGE, "true");
            config.remove(OAuth2Constants.PROMPT);

            return idp;
        }
    }

    @Override
    @Test
    @Ignore
    public void testLogInAsUserInIDP() {
        // super.testLogInAsUserInIDP();
    }

    @Test
    @Override
    public void loginWithExistingUser() {
        // login as brokered user user, perform profile update on first broker login and logout user
        loginUser();
        testSingleLogout();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        setTimeOffset(2);

        // trigger re-auth with max_age while we are still authenticated
        String loginUrlWithMaxAge = getLoginUrl(getConsumerRoot(), bc.consumerRealmName(), "account") + "&max_age=1";
        driver.navigate().to(loginUrlWithMaxAge);

        // we should now see the login page of the consumer
        waitForPage(driver, "sign in to", true);
        loginPage.assertCurrent(bc.consumerRealmName());
        Assert.assertTrue("Driver should be on the consumer realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/protocol/openid-connect/auth"));

        loginPage.clickSocial(bc.getIDPAlias());
        // we should see the login page of the provider, since the max_age was propagated
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        loginPage.assertCurrent(bc.providerRealmName());

        // reauthenticate with password
        loginPage.login(bc.getUserPassword());
        waitForPage(driver, "account management", true);

        testSingleLogout();
    }

    @Test
    public void testEnforceReAuthenticationWhenMaxAgeIsSet() {
        // login as brokered user user, perform profile update on first broker login and logout user
        loginUser();
        testSingleLogout();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        IdentityProviderResource idpResource = realmsResouce().realm(bc.consumerRealmName()).identityProviders()
                .get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRep = idpResource.toRepresentation();

        TestKeycloakOidcIdentityProviderFactory.setIgnoreMaxAgeParam(idpRep);

        idpResource.update(idpRep);

        setTimeOffset(2);

        // trigger re-auth with max_age while we are still authenticated
        String loginUrlWithMaxAge = getLoginUrl(getConsumerRoot(), bc.consumerRealmName(), "account") + "&max_age=1";
        driver.navigate().to(loginUrlWithMaxAge);

        // we should now see the login page of the consumer
        waitForPage(driver, "sign in to", true);
        loginPage.assertCurrent(bc.consumerRealmName());
        Assert.assertTrue("Driver should be on the consumer realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/protocol/openid-connect/auth"));

        loginPage.clickSocial(bc.getIDPAlias());
        // we should see the login page of the provider, since the max_age was propagated
        waitForPage(driver, "sign in to", true);
        loginPage.getError();
        Assert.assertEquals("Unexpected error when authenticating with identity provider",
                loginPage.getInstruction());

        testSingleLogout();
    }
}
