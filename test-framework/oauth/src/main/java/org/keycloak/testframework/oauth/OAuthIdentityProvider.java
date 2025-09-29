package org.keycloak.testframework.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.Map;

public class OAuthIdentityProvider {

    private final HttpServer httpServer;

    private final OAuthIdentityProviderKeys keys;
    private final OAuthIdentityProviderConfigBuilder.OAuthIdentityProviderConfiguration config;

    public OAuthIdentityProvider(HttpServer httpServer, OAuthIdentityProviderConfigBuilder.OAuthIdentityProviderConfiguration config) {
        this.config = config;
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }

        this.httpServer = httpServer;
        httpServer.createContext("/idp/jwks", new JwksHttpHandler());

        keys = new OAuthIdentityProviderKeys(config);
    }

    public String encodeToken(JsonWebToken token) {
        return encodeToken(token, keys);
    }

    public String encodeToken(JsonWebToken token, OAuthIdentityProviderKeys keys) {
        return new JWSBuilder().type("JWT").jsonContent(token).sign(new ECDSASignatureSignerContext(keys.getKeyWrapper()));
    }

    public OAuthIdentityProviderKeys createKeys() {
        return new OAuthIdentityProviderKeys(config);
    }

    public void close() {
        httpServer.removeContext("/idp/jwks");
    }

    public class JwksHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, keys.getJwksString().length());
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(keys.getJwksString().getBytes(StandardCharsets.UTF_8));
            outputStream.close();
        }

    }

    public static class OAuthIdentityProviderKeys {

        private final KeyWrapper keyWrapper;

        private final String jwksString;

        public OAuthIdentityProviderKeys(OAuthIdentityProviderConfigBuilder.OAuthIdentityProviderConfiguration config) {
            try {
                KeyUse keyUse = config.spiffe() ? KeyUse.JWT_SVID : KeyUse.SIG;

                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
                keyPairGenerator.initialize(ecSpec);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();

                JWK jwk = JWKBuilder.create().ec(keyPair.getPublic());
                if (!config.spiffe()) {
                    jwk.setAlgorithm("ES256");
                }
                jwk.setPublicKeyUse(keyUse.getSpecName());

                Map<String, Object> jwks = new HashMap<>();
                jwks.put("keys", new JWK[] { jwk });

                if (config.spiffe()) {
                    jwks.put("spiffe_sequence", 1);
                    jwks.put("spiffe_refresh_hint", 300);
                }

                jwksString = JsonSerialization.writeValueAsString(jwks);

                keyWrapper = new KeyWrapper();
                keyWrapper.setKid(jwk.getKeyId());
                keyWrapper.setPublicKey(keyPair.getPublic());
                keyWrapper.setPrivateKey(keyPair.getPrivate());
                keyWrapper.setUse(KeyUse.SIG);
                keyWrapper.setAlgorithm(Algorithm.ES256);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public KeyWrapper getKeyWrapper() {
            return keyWrapper;
        }

        public String getJwksString() {
            return jwksString;
        }
    }

}
