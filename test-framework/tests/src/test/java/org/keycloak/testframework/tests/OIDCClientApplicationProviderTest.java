package org.keycloak.testframework.tests;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.HMACProvider;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.JwksProvider;
import org.keycloak.testframework.oauth.OIDCClientApplicationProvider;
import org.keycloak.testframework.oauth.OIDCClientApplicationProviderConfig;
import org.keycloak.testframework.oauth.OIDCClientApplicationProviderConfiguration;
import org.keycloak.testframework.oauth.annotations.InjectJwksProvider;
import org.keycloak.testframework.oauth.annotations.InjectOIDCClientApplicationProvider;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class OIDCClientApplicationProviderTest {

    @InjectOIDCClientApplicationProvider(config = OidcClientConfig.class)
    OIDCClientApplicationProvider provider;

    @InjectJwksProvider
    JwksProvider jwksProvider;

    @InjectHttpClient
    HttpClient httpClient;

    @Test
    public void testConfig() throws Exception {
        JWSInput request = new JWSInput(provider.createRequest(Map.of("redirect_uri", "http://localhost/callback")));
        Assertions.assertEquals(Algorithm.RS256, request.getHeader().getAlgorithm().name());
        Assertions.assertEquals(Map.of(
                "client_id", "configured-client",
                "response_type", "code",
                "scope", "openid",
                "redirect_uri", "http://localhost/callback"), request.readJsonContent(Map.class));
        Assertions.assertNull(provider.getOidcRequest());
    }

    @Test
    public void testInitialSigningKey() throws Exception {
        JWK[] keys = jwksProvider.getJwks().getKeys();
        Assertions.assertEquals(1, keys.length);
        JWK key = keys[0];
        Assertions.assertEquals(Algorithm.RS256, key.getAlgorithm());

        JWSInput request = new JWSInput(provider.createRequest(Map.of("state", "signed")));
        Assertions.assertEquals(Algorithm.RS256, request.getHeader().getAlgorithm().name());
        Assertions.assertEquals(key.getKeyId(), request.getHeader().getKeyId());
        Assertions.assertTrue(RSAProvider.verify(request, JWKParser.create(key).toPublicKey()));
        Assertions.assertEquals("signed", request.readJsonContent(Map.class).get("state"));
        Assertions.assertNull(provider.getOidcRequest());
    }

    @Test
    public void testConfigClaimsCopy() {
        Map<String, Object> source = new HashMap<>(Map.of("client_id", "original"));
        OIDCClientApplicationProviderConfiguration config = new OIDCClientApplicationProviderConfiguration();
        config.setClaims(source);
        source.put("client_id", "changed");
        Assertions.assertEquals(Map.of("client_id", "original"), config.getClaims());
        config.getClaims().put("max_age", 10);
        Assertions.assertEquals(10, config.getClaims().get("max_age"));
    }

    @Test
    public void testRequestOverrides() throws Exception {
        Map<String, Object> overrides = new HashMap<>(Map.of("client_id", "override", "state", "one-request"));
        Map<?, ?> first = new JWSInput(provider.createRequest(overrides)).readJsonContent(Map.class);
        Assertions.assertEquals("override", first.get("client_id"));
        Assertions.assertEquals("one-request", first.get("state"));
        Assertions.assertEquals(2, overrides.size());
        overrides.put("state", "changed");

        Map<?, ?> second = new JWSInput(provider.createRequest(Map.of())).readJsonContent(Map.class);
        Assertions.assertEquals(new OidcClientConfig().build().getClaims(), second);
        Assertions.assertFalse(second.containsKey("state"));
    }

    @Test
    public void testUnsignedRequest() throws Exception {
        Map<String, Object> claims = Map.of("state", "deliberately-incomplete");
        JWSInput request = new JWSInput(provider.signRequestObject(claims, "none"));
        Assertions.assertEquals("none", request.getHeader().getAlgorithm().name());
        Assertions.assertEquals(claims, request.readJsonContent(Map.class));
    }

    @Test
    public void testUnregisteredRequest() throws Exception {
        Assertions.assertNull(provider.getOidcRequest());
        httpClient.execute(new HttpGet(provider.getUri()), response -> {
            Assertions.assertEquals(404, response.getStatusLine().getStatusCode());
            return null;
        });
    }

    @Test
    public void testRegisteredRequest() throws Exception {
        Assertions.assertNull(provider.getOidcRequest());
        provider.setRequest(Map.of("state", "hosted"));
        assertRequestServed(provider.getOidcRequest());
        Assertions.assertEquals("hosted", new JWSInput(provider.getOidcRequest()).readJsonContent(Map.class).get("state"));

        provider.setRequest(Map.of("state", "replacement"));
        assertRequestServed(provider.getOidcRequest());
        Assertions.assertEquals("replacement", new JWSInput(provider.getOidcRequest()).readJsonContent(Map.class).get("state"));
    }

    @Test
    public void testReset() throws Exception {
        provider.setRequest(Map.of("state", "before-reset"));
        assertRequestServed(provider.getOidcRequest());

        provider.reset();
        Assertions.assertNull(provider.getOidcRequest());
        httpClient.execute(new HttpGet(provider.getUri()), response -> {
            Assertions.assertEquals(404, response.getStatusLine().getStatusCode());
            return null;
        });

        provider.setRequest(Map.of("state", "after-reset"));
        assertRequestServed(provider.getOidcRequest());
    }

    @Test
    public void testHs256Signing() throws Exception {
        assertHmacSigning(Algorithm.HS256);
    }

    @Test
    public void testHs384Signing() throws Exception {
        assertHmacSigning(Algorithm.HS384);
    }

    @Test
    public void testHs512Signing() throws Exception {
        assertHmacSigning(Algorithm.HS512);
    }

    private void assertHmacSigning(String algorithm) throws Exception {
        String secret = "client-secret-\u00e9";
        Map<String, Object> claims = Map.of("state", "symmetric");
        String encodedRequest = provider.signRequestObject(claims, algorithm, secret);
        JWSInput request = new JWSInput(encodedRequest);
        Assertions.assertEquals(algorithm, request.getHeader().getAlgorithm().name());
        Assertions.assertEquals(claims, request.readJsonContent(Map.class));
        Assertions.assertTrue(HMACProvider.verify(request, secret.getBytes(StandardCharsets.UTF_8)));
        Assertions.assertFalse(HMACProvider.verify(request, "wrong-secret".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertNull(provider.getOidcRequest());

        provider.setOidcRequest(claims, algorithm, secret);
        Assertions.assertEquals(encodedRequest, provider.getOidcRequest());
        assertRequestServed(encodedRequest);
    }

    private void assertRequestServed(String expectedRequest) throws Exception {
        httpClient.execute(new HttpGet(provider.getUri()), response -> {
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            Assertions.assertEquals("application/oauth-authz-req+jwt", response.getFirstHeader("Content-Type").getValue());
            Assertions.assertEquals(expectedRequest, EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            return null;
        });
    }

    public static class OidcClientConfig implements OIDCClientApplicationProviderConfig {

        @Override
        public OIDCClientApplicationProviderConfiguration build() {
            OIDCClientApplicationProviderConfiguration config = new OIDCClientApplicationProviderConfiguration();
            config.setClientId("configured-client");
            config.setResponseType("code");
            config.setScope("openid");
            config.setSigningAlgorithm(Algorithm.RS256);
            config.setGenerateInitialSigningKey(true);
            return config;
        }
    }
}
