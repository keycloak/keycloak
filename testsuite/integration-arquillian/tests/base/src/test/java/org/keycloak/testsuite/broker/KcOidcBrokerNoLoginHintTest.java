package org.keycloak.testsuite.broker;

import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.SuiteContext;

public class KcOidcBrokerNoLoginHintTest extends KcOidcBrokerLoginHintTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationWithNoLoginHint();
    }

    private class KcOidcBrokerConfigurationWithNoLoginHint extends KcOidcBrokerConfiguration {
        
        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(suiteContext, config);
            config.put("loginHint", "false");
            return idp;
        }
    }

    @Override
    protected void loginUser() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        
        driver.navigate().to(driver.getCurrentUrl() + "&login_hint=" + USER_EMAIL);

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "log in to", true);

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        Assert.assertTrue("User identifiant should not be fullfilled",
                StringUtils.isBlank(loginPage.getUsername()));
        
        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "update account information", false);

        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

        int userCount = consumerUsers.count();
        Assert.assertTrue("There must be at least one user", userCount > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                isUserFound = true;
                break;
            }
        }

        Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
                isUserFound);
    }
}
