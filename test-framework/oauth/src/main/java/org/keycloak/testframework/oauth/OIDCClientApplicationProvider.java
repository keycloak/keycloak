package org.keycloak.testframework.oauth;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.MacSignatureSignerContext;
import org.keycloak.crypto.ServerECDSASignatureSignerContext;
import org.keycloak.crypto.ServerEdDSASignatureSignerContext;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.testframework.util.HttpServerUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Creates request objects and serves the registered JWT on the embedded test HTTP server.
 */
public class OIDCClientApplicationProvider implements Closeable {

    public static final String CONTEXT = "/get-oidc-request";

    private final HttpServer httpServer;
    private final JwksProvider jwksProvider;
    private final String signingAlgorithm;
    private final Map<String, Object> defaultClaims;
    private volatile String oidcRequest;

    public OIDCClientApplicationProvider(HttpServer httpServer, JwksProvider jwksProvider) {
        this(httpServer, jwksProvider, new DefaultOIDCClientApplicationProviderConfig().build());
    }

    public OIDCClientApplicationProvider(HttpServer httpServer, JwksProvider jwksProvider,
                                         OIDCClientApplicationProviderConfiguration configuration) {
        this.httpServer = httpServer;
        this.jwksProvider = jwksProvider;
        this.signingAlgorithm = configuration.getSigningAlgorithm();
        this.defaultClaims = new HashMap<>(configuration.getClaims());
        if (configuration.isGenerateInitialSigningKey() && !"none".equals(signingAlgorithm)) {
            jwksProvider.generateKeys(signingAlgorithm, configuration.getCurve(), true, false, null);
        }
        httpServer.createContext(CONTEXT, new OIDCRequestHandler());
    }

    /**
     * Creates a request from the configured defaults and per-request overrides without registering it.
     */
    public String createRequest(Map<String, Object> overrides) {
        Map<String, Object> request = new HashMap<>(defaultClaims);
        request.putAll(overrides);
        return signRequestObject(request, signingAlgorithm);
    }

    public void setRequest(Map<String, Object> overrides) {
        oidcRequest = createRequest(overrides);
    }

    /**
     * Signs exactly the supplied object, without adding configured claims or generating a new key.
     * Uses the first key currently published by the JWKS provider.
     */
    public String signRequestObject(Object requestObject, String algorithm) {
        if (algorithm == null) {
            throw new BadRequestException("Unknown argument: null");
        }
        switch (algorithm) {
            case "none":
                return new JWSBuilder().jsonContent(requestObject).none();
            case Algorithm.RS256, Algorithm.RS384, Algorithm.RS512,
                    Algorithm.PS256, Algorithm.PS384, Algorithm.PS512,
                    Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.EdDSA:
                break;
            default:
                throw new BadRequestException("Unknown argument: " + algorithm);
        }

        JwksProvider.KeyData keyData = jwksProvider.getFirstKey();
        if (keyData == null) {
            throw new BadRequestException("signing key not set");
        }

        String kid = keyData.getKid() != null ? keyData.getKid() : KeyUtils.createKeyId(keyData.getKeyPair().getPublic());
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setAlgorithm(algorithm);
        keyWrapper.setKid(kid);
        keyWrapper.setPrivateKey(keyData.getKeyPair().getPrivate());

        SignatureSignerContext signer = switch (algorithm) {
            case Algorithm.ES256, Algorithm.ES384, Algorithm.ES512 -> new ServerECDSASignatureSignerContext(keyWrapper);
            case Algorithm.EdDSA -> {
                keyWrapper.setCurve(keyData.getCurve());
                yield new ServerEdDSASignatureSignerContext(keyWrapper);
            }
            default -> new AsymmetricSignatureSignerContext(keyWrapper);
        };
        return new JWSBuilder().kid(kid).jsonContent(requestObject).sign(signer);
    }

    /**
     * Signs using the supplied client secret, or creates an unsigned request when the algorithm is none.
     */
    public String signRequestObject(Object requestObject, String algorithm, String clientSecret) {
        if (algorithm == null) {
            throw new BadRequestException("Unknown argument: null");
        }
        switch (algorithm) {
            case "none":
                return new JWSBuilder().jsonContent(requestObject).none();
            case Algorithm.HS256, Algorithm.HS384, Algorithm.HS512:
                break;
            default:
                throw new BadRequestException("Unknown jwaAlgorithm: " + algorithm);
        }
        if (clientSecret == null) {
            throw new BadRequestException("client secret not set");
        }

        SecretKey secretKey = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), JavaAlgorithm.getJavaAlgorithm(algorithm));
        String kid = KeyUtils.createKeyId(secretKey);
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setSecretKey(secretKey);
        keyWrapper.setKid(kid);
        keyWrapper.setAlgorithm(algorithm);
        keyWrapper.setUse(KeyUse.SIG);
        keyWrapper.setType(KeyType.OCT);
        return new JWSBuilder().kid(kid).jsonContent(requestObject).sign(new MacSignatureSignerContext(keyWrapper));
    }

    public void setOidcRequest(Object requestObject, String algorithm) {
        oidcRequest = signRequestObject(requestObject, algorithm);
    }

    public void setOidcRequest(Object requestObject, String algorithm, String clientSecret) {
        oidcRequest = signRequestObject(requestObject, algorithm, clientSecret);
    }

    public String getOidcRequest() {
        return oidcRequest;
    }

    public void reset() {
        oidcRequest = null;
    }

    public String getUri() {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort() + CONTEXT;
    }

    @Override
    public void close() {
        httpServer.removeContext(CONTEXT);
    }

    private class OIDCRequestHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String request = oidcRequest;
            if (request == null) {
                HttpServerUtil.sendResponse(exchange, 404, null);
            } else {
                HttpServerUtil.sendResponse(exchange, 200,
                        Map.of("Content-Type", List.of("application/oauth-authz-req+jwt")), request);
            }
        }
    }
}
