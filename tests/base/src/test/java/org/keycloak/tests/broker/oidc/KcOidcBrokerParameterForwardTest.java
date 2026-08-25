package org.keycloak.tests.broker.oidc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.broker.BrokerLoginTest;
import org.keycloak.tests.broker.KcOidcBrokerConfigSupport;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerParameterForwardTest implements BrokerLoginTest, KcOidcBrokerConfigSupport {

    private static final String FORWARDED_PARAMETER = "forwarded_parameter";
    private static final String FORWARDED_PARAMETER_VALUE = "forwarded_value";
    private static final String PARAMETER_NOT_SET = "parameter_not_set";
    private static final String PARAMETER_NOT_FORWARDED = "parameter_not_forwarded";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = ParameterForwardConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

    @Override
    public ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    public ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public ManagedWebDriver getWebDriver() {
        return webDriver;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public IdpReviewUserProfilePage getUpdateProfilePage() {
        return updateProfilePage;
    }

    @Override
    public void loginUser() {
        String claimsValue = "{\"userinfo\":{\"http://itsme.services/v2/claim/BENationalNumber\":null,\"spaced_value\":\"with space\"}}";
        String urlEncodedClaims = URLEncoder.encode(claimsValue, StandardCharsets.UTF_8);
        String forwardedEncodedParam = "forwarded_encoded";
        String forwardedEncodedParamValue = "encoded value";
        String forwardedEncodedParamValueEncoded = URLEncoder.encode(forwardedEncodedParamValue, StandardCharsets.UTF_8);

        getOAuthClient().openLoginForm();
        String queryString = "&" + FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE
                + "&" + PARAMETER_NOT_FORWARDED + "=" + "value"
                + "&" + OAuth2Constants.ACR_VALUES + "=" + "phr"
                + "&" + OIDCLoginProtocol.CLAIMS_PARAM + "=" + urlEncodedClaims
                + "&" + forwardedEncodedParam + "=" + forwardedEncodedParamValue;
        webDriver.open(webDriver.getCurrentUrl() + queryString);

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));

        String currentUrl = webDriver.getCurrentUrl();
        assertThat(FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE + " should be part of the url",
                currentUrl, containsString(FORWARDED_PARAMETER + "=" + FORWARDED_PARAMETER_VALUE));
        assertThat(OAuth2Constants.ACR_VALUES + "=phr should be part of the url",
                currentUrl, containsString(OAuth2Constants.ACR_VALUES + "=" + "phr"));
        assertThat(OIDCLoginProtocol.CLAIMS_PARAM + " should be part of the url",
                currentUrl, containsString(OIDCLoginProtocol.CLAIMS_PARAM + "=" + urlEncodedClaims));
        assertThat(forwardedEncodedParam + " should be part of the url",
                currentUrl, containsString(forwardedEncodedParam + "=" + forwardedEncodedParamValueEncoded));
        assertThat(PARAMETER_NOT_SET + " should NOT be part of the url",
                currentUrl, not(containsString(PARAMETER_NOT_SET)));
        assertThat(PARAMETER_NOT_FORWARDED + " should NOT be part of the url",
                currentUrl, not(containsString(PARAMETER_NOT_FORWARDED)));

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    static class ParameterForwardConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return KcOidcBrokerConfigSupport.configureConsumerRealm(realm,
                    KcOidcBrokerConfigSupport.createOidcIdentityProvider()
                            .attribute("forwardParameters",
                                    FORWARDED_PARAMETER + ", " + PARAMETER_NOT_SET + ", "
                                            + OAuth2Constants.ACR_VALUES + ", " + OIDCLoginProtocol.CLAIMS_PARAM
                                            + ",forwarded_encoded"));
        }
    }
}
