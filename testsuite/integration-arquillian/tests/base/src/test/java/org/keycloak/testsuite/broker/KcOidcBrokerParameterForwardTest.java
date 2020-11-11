package org.keycloak.testsuite.broker;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;

public class KcOidcBrokerParameterForwardTest extends AbstractBrokerTest {

    private static final String FORWARDED_PARAMETER = "forwarded_parameter";
    private static final String FORWARDED_PARAMETER_VALUE = "forwarded_value";
    private static final String PARAMETER_NOT_SET = "parameter_not_set";
    private static final String PARAMETER_NOT_FORWARDED = "parameter_not_forwarded";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationWithParameterForward();
    }

    private class KcOidcBrokerConfigurationWithParameterForward extends KcOidcBrokerConfiguration {

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);
            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(config, syncMode);
            config.put("forwardParameters", FORWARDED_PARAMETER +", " + PARAMETER_NOT_SET);
            return idp;
        }
    }

    @Override
    protected void loginUser() {
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        String queryString = "&" + FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE + "&" + PARAMETER_NOT_FORWARDED + "=" + "value";
        driver.navigate().to(driver.getCurrentUrl() + queryString);

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sign in to", true);

        Assert.assertThat("Driver should be on the provider realm page right now",
                driver.getCurrentUrl(), containsString("/auth/realms/" + bc.providerRealmName() + "/"));

        Assert.assertThat(FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE + " should be part of the url",
                driver.getCurrentUrl(), containsString(FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE));

        Assert.assertThat("\"" + PARAMETER_NOT_SET + "\"" + " should NOT be part of the url",
                driver.getCurrentUrl(), not(containsString(PARAMETER_NOT_SET)));

        Assert.assertThat("\"" + PARAMETER_NOT_FORWARDED +"\"" + " should be NOT part of the url",
                driver.getCurrentUrl(), not(containsString(PARAMETER_NOT_FORWARDED)));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        waitForPage(driver, "update account information", false);

        updateAccountInformationPage.assertCurrent();

        Assert.assertThat("We must be on correct realm right now",
                driver.getCurrentUrl(), containsString("/auth/realms/" + bc.consumerRealmName() + "/"));

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
