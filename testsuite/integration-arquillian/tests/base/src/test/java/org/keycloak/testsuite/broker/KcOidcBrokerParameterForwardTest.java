package org.keycloak.testsuite.broker;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
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
            config.put("forwardParameters", FORWARDED_PARAMETER +", " + PARAMETER_NOT_SET + ", " + OAuth2Constants.ACR_VALUES + ", " + OIDCLoginProtocol.CLAIMS_PARAM + ",forwarded_encoded");
            return idp;
        }
    }

    @Override
    protected void loginUser() {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        String claimsValue = "{\"userinfo\":{\"http://itsme.services/v2/claim/BENationalNumber\":null}}";
        String urlEncodedClaims = URLEncoder.encode(claimsValue, StandardCharsets.UTF_8);
        String forwardedEncodedParam = "forwarded_encoded";
        String forwardedEncodedParamValue = "encoded value";
        String forwardedEncodedParamvalueEncoded = URLEncoder.encode(forwardedEncodedParamValue, StandardCharsets.UTF_8);
        String queryString = "&" + FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE + "&" + PARAMETER_NOT_FORWARDED + "=" + "value"
                + "&" + OAuth2Constants.ACR_VALUES + "=" + "phr"
                + "&" + OIDCLoginProtocol.CLAIMS_PARAM + "=" + urlEncodedClaims
                + "&" + forwardedEncodedParam + "=" + forwardedEncodedParamValue;
        driver.navigate().to(driver.getCurrentUrl() + queryString);

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sign in to", true);

        assertThat("Driver should be on the provider realm page right now",
                driver.getCurrentUrl(), containsString("/auth/realms/" + bc.providerRealmName() + "/"));

        assertThat(FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE + " should be part of the url",
                driver.getCurrentUrl(), containsString(FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE));
        assertThat(OAuth2Constants.ACR_VALUES + "=" + "phr" + " should be part of the url",
                driver.getCurrentUrl(), containsString(OAuth2Constants.ACR_VALUES + "=" + "phr"));
        assertThat(OIDCLoginProtocol.CLAIMS_PARAM + "=" + urlEncodedClaims + " should be part of the url",
                driver.getCurrentUrl(), containsString(OIDCLoginProtocol.CLAIMS_PARAM + "=" + urlEncodedClaims));
        assertThat(forwardedEncodedParam + "=" + forwardedEncodedParamValue +  "should be part of the url",
                driver.getCurrentUrl(), containsString(forwardedEncodedParam + "=" + URLEncoder.encode(forwardedEncodedParamvalueEncoded, StandardCharsets.UTF_8)));
        assertThat("\"" + PARAMETER_NOT_SET + "\"" + " should NOT be part of the url",
                driver.getCurrentUrl(), not(containsString(PARAMETER_NOT_SET)));

        assertThat("\"" + PARAMETER_NOT_FORWARDED +"\"" + " should be NOT part of the url",
                driver.getCurrentUrl(), not(containsString(PARAMETER_NOT_FORWARDED)));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        waitForPage(driver, "update account information", false);

        updateAccountInformationPage.assertCurrent();

        assertThat("We must be on correct realm right now",
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
