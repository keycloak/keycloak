package org.keycloak.tests.broker.oidc;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class KcOidcBrokerPromptParameterTest extends AbstractKcOidcBrokerTest {

    private static final String PROMPT_CONSENT = "consent";
    private static final String PROMPT_LOGIN = "login";

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = PromptConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @Override
    protected void loginUser() {
        oauth.loginForm()
                .param(OIDCLoginProtocol.PROMPT_PARAM, PROMPT_CONSENT)
                .open();

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertThat(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_LOGIN + " should not be part of the url",
                webDriver.getCurrentUrl(), not(containsString(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_LOGIN)));
        assertThat(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT + " should be part of the url",
                webDriver.getCurrentUrl(), containsString(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT));

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    // The shared createOidcIdentityProvider() sets prompt=login, which the broker always prefers over the
    // client-forwarded prompt. This test needs the client's prompt=consent to pass through, so it clears the
    // IDP-configured prompt - mirroring the legacy KcOidcBrokerConfiguration2, which removed the attribute.
    static class PromptConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider().attribute("prompt", null));
        }
    }
}
