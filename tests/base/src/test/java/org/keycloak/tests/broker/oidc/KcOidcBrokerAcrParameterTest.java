package org.keycloak.tests.broker.oidc;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerAcrParameterTest extends AbstractKcOidcBrokerTest {

    private static final String ACR_VALUES = "acr_values";
    private static final String ACR_3 = "3";

    @Override
    protected void loginUser() {
        oauth.loginForm().param(ACR_VALUES, ACR_3).open();

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertThat(ACR_VALUES + "=" + ACR_3 + " should be part of the url",
                webDriver.getCurrentUrl(), containsString(ACR_VALUES + "=" + ACR_3));

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }
}
