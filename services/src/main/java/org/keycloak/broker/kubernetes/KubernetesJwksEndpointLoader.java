package org.keycloak.broker.kubernetes;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JWKSUtils;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.jboss.logging.Logger;

import static org.keycloak.broker.kubernetes.KubernetesConstants.SERVICE_ACCOUNT_TOKEN_PATH;
import static org.keycloak.connections.httpclient.DefaultHttpClientFactory.METRICS_URI_TEMPLATE_HEADER;

public class KubernetesJwksEndpointLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(KubernetesJwksEndpointLoader.class);

    private final KeycloakSession session;
    private final String issuer;

    public KubernetesJwksEndpointLoader(KeycloakSession session, String issuer) {
        this.session = session;
        this.issuer = issuer;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        SimpleHttp simpleHttp = SimpleHttp.create(session);

        String token = getToken(issuer);

        String path = "/.well-known/openid-configuration";
        String wellKnownEndpoint = issuer + path;

        SimpleHttpRequest wellKnownReqest = simpleHttp.doGet(wellKnownEndpoint).header(METRICS_URI_TEMPLATE_HEADER, path).acceptJson();

        if (token != null) {
            wellKnownReqest.auth(token);
        }
        URI uri = URI.create(wellKnownReqest.asJson(OIDCConfigurationRepresentation.class).getJwksUri());
        SimpleHttpRequest jwksRequest = simpleHttp.doGet(uri.toString())
              .header(HttpHeaders.ACCEPT, "application/jwk-set+json")
              .header(METRICS_URI_TEMPLATE_HEADER, uri.getPath());
        if (token != null) {
            jwksRequest.auth(token);
        }

        JSONWebKeySet jwks = jwksRequest.asJson(JSONWebKeySet.class);
        return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG);
    }

    private String getToken(String issuer) {
        try {
            File file = new File(SERVICE_ACCOUNT_TOKEN_PATH);
            if (!file.exists()) {
                return null;
            }

            String token = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            JsonWebToken jwt = new JWSInput(token).readJsonContent(JsonWebToken.class);
            if (jwt.getIssuer().equals(issuer)) {
                logger.trace("Including service account token in request");
                return token;
            } else {
                logger.debug("Not including service account token due to issuer missmatch");
            }
        } catch (Exception e) {
            logger.warn("Failed to read service account token file", e);
        }
        return null;
    }
}
