package org.keycloak.testframework.oauth;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP384R1;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP521R1;

/**
 * Mock JWKS provider exposing keys on the embedded test HTTP server.
 */
public class JwksProvider implements Closeable {

    public static final String CONTEXT = "/jwks";
    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";

    private final HttpServer httpServer;
    private final List<KeyData> keys = Collections.synchronizedList(new ArrayList<>());

    public JwksProvider(HttpServer httpServer) {
        this.httpServer = httpServer;
        this.httpServer.createContext(CONTEXT, new JwksHandler());
    }

    public Map<String, String> generateKeys(String jwaAlgorithm) {
        return generateKeys(jwaAlgorithm, null, true, false, null);
    }

    public Map<String, String> generateKeys(String jwaAlgorithm, String curve, Boolean advertiseJWKAlgorithm, Boolean keepExistingKeys, String kid) {
        try {
            KeyPair keyPair;
            KeyUse keyUse = KeyUse.SIG;
            if (jwaAlgorithm == null) {
                jwaAlgorithm = Algorithm.RS256;
            }
            String keyType;

            switch (jwaAlgorithm) {
                case Algorithm.RS256:
                case Algorithm.RS384:
                case Algorithm.RS512:
                case Algorithm.PS256:
                case Algorithm.PS384:
                case Algorithm.PS512:
                    keyType = KeyType.RSA;
                    keyPair = KeyUtils.generateRsaKeyPair(2048);
                    break;
                case Algorithm.ES256:
                    keyType = KeyType.EC;
                    keyPair = KeyUtils.generateEcKeyPair(EC_KEY_SECP256R1);
                    break;
                case Algorithm.ES384:
                    keyType = KeyType.EC;
                    keyPair = KeyUtils.generateEcKeyPair(EC_KEY_SECP384R1);
                    break;
                case Algorithm.ES512:
                    keyType = KeyType.EC;
                    keyPair = KeyUtils.generateEcKeyPair(EC_KEY_SECP521R1);
                    break;
                case Algorithm.EdDSA:
                    if (curve == null) {
                        curve = Algorithm.Ed25519;
                    }
                    keyType = KeyType.OKP;
                    keyPair = KeyUtils.generateEddsaKeyPair(curve);
                    break;
                case JWEConstants.RSA1_5:
                case JWEConstants.RSA_OAEP:
                case JWEConstants.RSA_OAEP_256:
                    // for JWE KEK Key Encryption
                    keyType = KeyType.RSA;
                    keyUse = KeyUse.ENC;
                    keyPair = KeyUtils.generateRsaKeyPair(2048);
                    break;
                default :
                    throw new RuntimeException("Unsupported signature algorithm");
            }

            KeyData keyData = new KeyData();
            keyData.setKid(kid);
            keyData.setKeyPair(keyPair);
            keyData.setKeyType(keyType);
            keyData.setCurve(curve);
            if (advertiseJWKAlgorithm == null || Boolean.TRUE.equals(advertiseJWKAlgorithm)) {
                keyData.setKeyAlgorithm(jwaAlgorithm);
            } else {
                keyData.setKeyAlgorithm(null);
            }
            keyData.setKeyUse(keyUse);

            if (keepExistingKeys != null && keepExistingKeys) {
                keys.add(keyData);
            } else {
                keys.clear();
                keys.add(keyData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating signing keypair", e);
        }
        return getKeysAsPem();
    }

    public Map<String, String> getKeysAsPem() {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        KeyData keyData = keys.get(0);
        String privateKeyPem = PemUtils.encodeKey(keyData.getKeyPair().getPrivate());
        String publicKeyPem = PemUtils.encodeKey(keyData.getKeyPair().getPublic());

        Map<String, String> res = new ConcurrentHashMap<>();
        res.put(PRIVATE_KEY, privateKeyPem);
        res.put(PUBLIC_KEY, publicKeyPem);
        return res;
    }

    public JSONWebKeySet getJwks() {
        JWK[] jwksArray = keys.stream()
                .map(keyData -> {
                    KeyPair keyPair = keyData.getKeyPair();
                    String keyAlgorithm = keyData.getKeyAlgorithm();
                    String keyType = keyData.getKeyType();
                    KeyUse keyUse = keyData.getKeyUse();
                    String kid = keyData.getKid();

                    JWKBuilder builder = JWKBuilder.create().algorithm(keyAlgorithm).kid(kid);

                    if (KeyType.RSA.equals(keyType)) {
                        return builder.rsa(keyPair.getPublic(), keyUse);
                    } else if (KeyType.EC.equals(keyType)) {
                        return builder.ec(keyPair.getPublic());
                    } else if (KeyType.OKP.equals(keyType)) {
                        return builder.okp(keyPair.getPublic());
                    } else {
                        throw new IllegalArgumentException("Unknown keyType: " + keyType);
                    }
                })
                .toArray(JWK[]::new);

        JSONWebKeySet keySet = new JSONWebKeySet();
        keySet.setKeys(jwksArray);
        return keySet;
    }

    public String getUri() {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort() + CONTEXT;
    }

    @Override
    public void close() {
        httpServer.removeContext(CONTEXT);
    }

    private class JwksHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JSONWebKeySet keySet = getJwks();
            String metadata = JsonSerialization.writeValueAsString(keySet);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(Response.Status.OK.getStatusCode(), metadata.length());
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(metadata.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public static class KeyData {

        private KeyPair keyPair;

        private String keyType = KeyType.RSA;
        private String keyAlgorithm;
        private KeyUse keyUse = KeyUse.SIG;
        private String curve;

        // Kid will be randomly generated (based on the key hash) if not provided here
        private String kid;

        public KeyPair getKeyPair() {
            return keyPair;
        }

        public void setKeyPair(KeyPair keyPair) {
            this.keyPair = keyPair;
        }

        public String getKeyType() {
            return keyType;
        }

        public void setKeyType(String keyType) {
            this.keyType = keyType;
        }

        public String getKeyAlgorithm() {
            return keyAlgorithm;
        }

        public void setKeyAlgorithm(String keyAlgorithm) {
            this.keyAlgorithm = keyAlgorithm;
        }

        public KeyUse getKeyUse() {
            return keyUse;
        }

        public void setKeyUse(KeyUse keyUse) {
            this.keyUse = keyUse;
        }

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getCurve() {
            return curve;
        }

        public void setCurve(String curve) {
            this.curve = curve;
        }
    }
}
