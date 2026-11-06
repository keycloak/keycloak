package org.keycloak.tests.client.authentication.external;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.client.FederatedJWTClientAuthenticator;
import org.keycloak.broker.spiffe.SpiffeConstants;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderConfig;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testframework.util.HttpServerUtil;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = SpiffeClientAuthTest.SpiffeServerConfig.class)
public class SpiffeBundleMapClientAuthTest extends AbstractClientAuthTest {

    @InjectRealm(config = SpiffeClientAuthTest.ExernalClientAuthRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthIdentityProvider(config = SpiffeClientAuthTest.SpiffeIdpConfig.class)
    OAuthIdentityProvider identityProvider;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectHttpServer
    HttpServer httpServer;

    private static final String BUNDLE_MAP_PATH = "/spiffe-bundle-map-test";
    private boolean bundleMapContextInstalled;
    private String bundleMapEndpoint;

    public SpiffeBundleMapClientAuthTest() {
        super(null, SpiffeClientAuthTest.INTERNAL_CLIENT_ID, SpiffeClientAuthTest.EXTERNAL_CLIENT_ID);
    }

    @BeforeEach
    public void configureBundleMapEndpoint() {
        bundleMapEndpoint = "http://" + httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort() + BUNDLE_MAP_PATH;
        realm.updateIdentityProvider(SpiffeClientAuthTest.IDP_ALIAS,
                idp -> idp.getConfig().put(SpiffeIdentityProviderConfig.BUNDLE_ENDPOINT_KEY, bundleMapEndpoint));
    }

    @Test
    public void configuredTrustDomainBundleIsUsed() {
        // given
        var trustedKeys = identityProvider.createKeys();
        var otherDomainKeys = identityProvider.createKeys();
        String kid = "shared-kid";
        String trustedJwks = trustedKeys.getJwksString().replace(trustedKeys.getKeyWrapper().getKid(), kid);
        String otherJwks = otherDomainKeys.getJwksString().replace(otherDomainKeys.getKeyWrapper().getKid(), kid);
        trustedKeys.getKeyWrapper().setKid(kid);
        otherDomainKeys.getKeyWrapper().setKid(kid);
        publishBundleMap(bundleMap(trustedJwks, otherJwks));
        var token = identityProvider.encodeToken(createDefaultToken(), trustedKeys);

        // when
        var response = doClientGrant(token);

        // then
        Assertions.assertTrue(response.isSuccess());
    }

    @Test
    public void missingTrustDomainIsRejected() {
        // given
        publishBundleMap("{\"trust_domains\":{\"other-domain\":{\"keys\":[]}}}");
        var token = identityProvider.encodeToken(createDefaultToken());

        // when
        var response = doClientGrant(token);

        // then
        Assertions.assertFalse(response.isSuccess());
    }

    @Test
    public void malformedBundleMapIsRejected() {
        // given
        publishBundleMap("{\"trust_domains\":[]}");
        var token = identityProvider.encodeToken(createDefaultToken());

        // when
        var response = doClientGrant(token);

        // then
        Assertions.assertFalse(response.isSuccess());
    }

    @Test
    public void duplicateTrustDomainKeyIsRejected() {
        // given
        String jwks = identityProvider.getKeys().getJwksString();
        publishBundleMap("{\"trust_domains\":{\"mytrust-domain\":" + jwks + ",\"mytrust-domain\":" + jwks + "}}");
        var token = identityProvider.encodeToken(createDefaultToken());

        // when
        var response = doClientGrant(token);

        // then
        Assertions.assertFalse(response.isSuccess());
    }

    @Test
    public void signatureFromOtherDomainIsRejectedEvenWithSameKid() {
        // given
        var trustedKeys = identityProvider.createKeys();
        var otherDomainKeys = identityProvider.createKeys();
        String kid = "shared-kid";
        String trustedJwks = trustedKeys.getJwksString().replace(trustedKeys.getKeyWrapper().getKid(), kid);
        String otherJwks = otherDomainKeys.getJwksString().replace(otherDomainKeys.getKeyWrapper().getKid(), kid);
        trustedKeys.getKeyWrapper().setKid(kid);
        otherDomainKeys.getKeyWrapper().setKid(kid);
        publishBundleMap(bundleMap(trustedJwks, otherJwks));
        var token = new JWSBuilder().type("JWT").jsonContent(createDefaultToken())
                .sign(new ECDSASignatureSignerContext(otherDomainKeys.getKeyWrapper()));

        // when
        var response = doClientGrant(token);

        // then
        Assertions.assertFalse(response.isSuccess());
    }

    @Test
    public void withdrawnKeyIsRejectedAfterAutomaticRefresh() {
        // given
        publishBundleMap(bundleMap(identityProvider.getKeys().getJwksString(), "{\"keys\":[]}"));
        var originalResponse = doClientGrant(createDefaultToken());
        publishBundleMap(bundleMap("{\"keys\":[]}", "{\"keys\":[]}"));
        timeOffSet.set(350);
        var token = createDefaultToken();
        var encodedToken = identityProvider.encodeToken(token);

        // when
        var response = doClientGrant(encodedToken);

        // then
        Assertions.assertTrue(originalResponse.isSuccess());
        Assertions.assertFalse(response.isSuccess());
    }

    @Test
    public void rotatedKeyIsAcceptedAfterAutomaticRefresh() {
        // given
        publishBundleMap(bundleMap(identityProvider.getKeys().getJwksString(), "{\"keys\":[]}"));
        var originalResponse = doClientGrant(createDefaultToken());
        var rotatedKeys = identityProvider.createKeys();
        publishBundleMap(bundleMap(rotatedKeys.getJwksString(), "{\"keys\":[]}"));
        timeOffSet.set(350);
        var token = createDefaultToken();
        var encodedToken = identityProvider.encodeToken(token, rotatedKeys);

        // when
        var response = doClientGrant(encodedToken);

        // then
        Assertions.assertTrue(originalResponse.isSuccess());
        Assertions.assertTrue(response.isSuccess());
    }

    @Test
    public void twoIdentityProvidersUsingSameEndpointKeepDomainKeysSeparate() {
        // given
        var trustedKeys = identityProvider.createKeys();
        var otherDomainKeys = identityProvider.createKeys();
        String kid = "shared-domain-kid";
        String trustedJwks = trustedKeys.getJwksString().replace(trustedKeys.getKeyWrapper().getKid(), kid);
        String otherJwks = otherDomainKeys.getJwksString().replace(otherDomainKeys.getKeyWrapper().getKid(), kid);
        trustedKeys.getKeyWrapper().setKid(kid);
        otherDomainKeys.getKeyWrapper().setKid(kid);
        publishBundleMap(bundleMap(trustedJwks, otherJwks));
        var trustedResponse = doClientGrant(identityProvider.encodeToken(createDefaultToken(), trustedKeys));
        configureOtherDomainIdpAndClient();
        var token = createDefaultToken();
        token.subject("spiffe://other-domain/otherclient");
        var encodedToken = identityProvider.encodeToken(token, otherDomainKeys);

        // when
        var response = doClientGrant(encodedToken);

        // then
        Assertions.assertTrue(trustedResponse.isSuccess());
        Assertions.assertTrue(response.isSuccess());
    }

    @Test
    public void secondIdentityProviderRejectsPrimaryDomainKeyWithSameKid() {
        // given
        var trustedKeys = identityProvider.createKeys();
        var otherDomainKeys = identityProvider.createKeys();
        String kid = "shared-domain-kid";
        String trustedJwks = trustedKeys.getJwksString().replace(trustedKeys.getKeyWrapper().getKid(), kid);
        String otherJwks = otherDomainKeys.getJwksString().replace(otherDomainKeys.getKeyWrapper().getKid(), kid);
        trustedKeys.getKeyWrapper().setKid(kid);
        otherDomainKeys.getKeyWrapper().setKid(kid);
        publishBundleMap(bundleMap(trustedJwks, otherJwks));
        var trustedResponse = doClientGrant(identityProvider.encodeToken(createDefaultToken(), trustedKeys));
        configureOtherDomainIdpAndClient();
        var token = createDefaultToken();
        token.subject("spiffe://other-domain/otherclient");
        var encodedToken = identityProvider.encodeToken(token, trustedKeys);

        // when
        var response = doClientGrant(encodedToken);

        // then
        Assertions.assertTrue(trustedResponse.isSuccess());
        Assertions.assertFalse(response.isSuccess());
    }

    @AfterEach
    public void resetIdentityProvider() {
        if (bundleMapContextInstalled) {
            httpServer.removeContext(BUNDLE_MAP_PATH);
        }
    }

    private void publishBundleMap(String content) {
        if (bundleMapContextInstalled) {
            httpServer.removeContext(BUNDLE_MAP_PATH);
        }
        byte[] response = content.getBytes(StandardCharsets.UTF_8);
        httpServer.createContext(BUNDLE_MAP_PATH, exchange -> HttpServerUtil.sendResponse(exchange, 200,
                Map.of("Content-Type", List.of("application/json")), response));
        bundleMapContextInstalled = true;
    }

    private void configureOtherDomainIdpAndClient() {
        IdentityProviderRepresentation otherIdp = IdentityProviderBuilder.create()
                .providerId(SpiffeIdentityProviderFactory.PROVIDER_ID)
                .alias("spiffe-other")
                .attribute(SpiffeIdentityProviderConfig.TRUST_DOMAIN_KEY, "spiffe://other-domain")
                .attribute(SpiffeIdentityProviderConfig.BUNDLE_ENDPOINT_KEY, bundleMapEndpoint)
                .build();
        otherIdp.setInternalId(ApiUtil.getCreatedId(realm.admin().identityProviders().create(otherIdp)));
        realm.updateClientWithCleanup(SpiffeClientAuthTest.INTERNAL_CLIENT_ID, client -> client
                .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, "spiffe-other")
                .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, "spiffe://other-domain/otherclient"));
    }

    private String bundleMap(String trustedBundle, String otherBundle) {
        return "{\"trust_domains\":{\"mytrust-domain\":" + trustedBundle + ",\"other-domain\":" + otherBundle + "}}";
    }

    @Override
    protected JsonWebToken createDefaultToken() {
        JsonWebToken token = new JsonWebToken();
        token.audience(oAuthClient.getEndpoints().getIssuer());
        token.exp((long) Time.currentTime() + 300);
        token.subject(SpiffeClientAuthTest.EXTERNAL_CLIENT_ID);
        return token;
    }

    @Override
    protected String getClientAssertionType() {
        return SpiffeConstants.CLIENT_ASSERTION_TYPE;
    }

    @Override
    protected OAuthIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

}
