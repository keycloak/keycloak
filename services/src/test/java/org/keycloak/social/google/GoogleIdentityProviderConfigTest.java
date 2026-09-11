package org.keycloak.social.google;

import java.lang.reflect.Proxy;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Regression tests for GoogleIdentityProviderConfig validation.
 *
 * <p>Google has fixed, well-known endpoints and intentionally <em>replaces</em> the generic OIDC
 * validation chain instead of extending it. When the validation entry point moved from
 * {@code validate(RealmModel)} to {@code validate(KeycloakSession, RealmModel)}, Google's override has to
 * move with it; otherwise the generic OIDC checks (e.g. "JWKS URL required when Supports client assertions
 * enabled") run for Google and break creation of Google IdPs that enable federated client authentication
 * without a JWKS URL. See IdentityProviderIssuerTest#testCreateUpdateDuplicateIdentityProvider.
 */
public class GoogleIdentityProviderConfigTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    private static GoogleIdentityProviderConfig config() {
        GoogleIdentityProviderConfig c = new GoogleIdentityProviderConfig(new IdentityProviderModel());
        // Google's factory always sets the fixed issuer; mirror that here.
        c.getConfig().put(IdentityProviderModel.ISSUER, GoogleIdentityProvider.ISSUER_URL);
        return c;
    }

    private static RealmModel realmWithSsl(SslRequired sslRequired) {
        return (RealmModel) Proxy.newProxyInstance(
                GoogleIdentityProviderConfigTest.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> {
                    if ("getSslRequired".equals(method.getName())) {
                        return sslRequired;
                    }
                    throw new UnsupportedOperationException("stub: " + method.getName());
                });
    }

    private static KeycloakSession session() {
        return (KeycloakSession) Proxy.newProxyInstance(
                GoogleIdentityProviderConfigTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException("stub: " + method.getName());
                });
    }

    /**
     * With federated client authentication enabled and no JWKS URL, the generic OIDC config would reject
     * creation. Google must not: it replaces the chain and validates only its own (fixed) issuer.
     */
    @Test
    public void federatedClientAuthWithoutJwksUrlIsAccepted() {
        GoogleIdentityProviderConfig c = config();
        c.setSupportsClientAssertions(true);
        c.setUseJwksUrl(true);
        // deliberately no JWKS URL set

        RealmModel realm = realmWithSsl(SslRequired.NONE);

        // must not throw
        c.validate(session(), realm);
    }

    /**
     * Sanity check that the generic OIDC config (which Google deliberately does not inherit the behaviour of)
     * would in fact reject the same configuration, proving the test above exercises the real difference.
     */
    @Test(expected = IllegalArgumentException.class)
    public void genericOidcRejectsFederatedClientAuthWithoutJwksUrl() {
        OIDCIdentityProviderConfig c = new OIDCIdentityProviderConfig(new IdentityProviderModel());
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setSupportsClientAssertions(true);
        c.setUseJwksUrl(true);
        // no JWKS URL

        RealmModel realm = realmWithSsl(SslRequired.NONE);
        c.validate(session(), realm);
    }
}
