package org.keycloak.tests.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.OAuth2Constants.REDIRECT_URI;
import static org.keycloak.OAuth2Constants.RESPONSE_TYPE;
import static org.keycloak.OAuth2Constants.SCOPE;
import static org.keycloak.models.Constants.CLIENT_ID;
import static org.keycloak.models.Constants.KC_ACTION;
import static org.keycloak.models.Constants.KC_ACTION_STATUS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public abstract class AbstractAppInitiatedActionTest {

    @InjectRealm(config = AbstractAppInitiatedActionTest.ForgetResetEnabledRealm.class)
    protected ManagedRealm managedRealm;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectKeycloakUrls
    protected KeycloakUrls keycloakUrls;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    protected static final String SUCCESS = "success";
    protected static final String CANCELLED = "cancelled";

    protected abstract String getAiaAction();

    protected void doAIA() {
        UriBuilder builder = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(keycloakUrls.getBase()));
        String uri = builder.queryParam(KC_ACTION, getAiaAction()) // TODO
                .queryParam(RESPONSE_TYPE, "code")
                .queryParam(CLIENT_ID, "test-app")
                .queryParam(SCOPE, "openid")
                .queryParam(REDIRECT_URI, oauth.getRedirectUri()) // TODO
                .build(managedRealm.getName()).toString();
        driver.open(uri);
    }

    protected void assertKcActionStatus(String expectedStatus) {
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        String kcActionStatus = getCurrentUrlParam(KC_ACTION_STATUS);
        assertThat(kcActionStatus, is(expectedStatus));
    }

    protected String getCurrentUrlParam(String paramName) {
        final URI url;
        try {
            url = new URI(this.driver.getCurrentUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        List<NameValuePair> pairs = URLEncodedUtils.parse(url, StandardCharsets.UTF_8);
        for (NameValuePair p : pairs) {
            if (p.getName().equals(paramName)) {
                return p.getValue();
            }
        }
        return null;
    }

    private static class ForgetResetEnabledRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .resetPasswordAllowed(true)
                    .registrationAllowed(true);
        }
    }
}
