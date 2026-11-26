package org.keycloak.broker.kubernetes;

import java.io.File;
import java.io.IOException;

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.Strings;

import org.apache.http.HttpHeaders;
import org.jboss.logging.Logger;

public class KubernetesJwksEndpointLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(KubernetesJwksEndpointLoader.class);

    private final KeycloakSession session;
    private final String issuer;
    private final String jwksUrl;
    private final boolean forwardTokenToAdditionalEndpoints;
    private final File tokenFile;

    public KubernetesJwksEndpointLoader(KeycloakSession session, KubernetesIdentityProviderConfig config) {
        this(session, config.getIssuer(), config.getJwksUrl(), config.isForwardTokenToAdditionalEndpoints());
    }

    public KubernetesJwksEndpointLoader(KeycloakSession session, String issuer, String jwksUrl) {
        this(session, issuer, jwksUrl, false);
    }

    public KubernetesJwksEndpointLoader(KeycloakSession session, String issuer, String jwksUrl, boolean forwardTokenToAdditionalEndpoints) {
        this(session, issuer, jwksUrl, forwardTokenToAdditionalEndpoints, new File(KubernetesConstants.SERVICE_ACCOUNT_TOKEN_PATH));
    }

    KubernetesJwksEndpointLoader(KeycloakSession session, String issuer, String jwksUrl, boolean forwardTokenToAdditionalEndpoints, File tokenFile) {
        this.session = session;
        this.issuer = issuer;
        this.jwksUrl = jwksUrl;
        this.forwardTokenToAdditionalEndpoints = forwardTokenToAdditionalEndpoints;
        this.tokenFile = tokenFile != null ? tokenFile : new File(KubernetesConstants.SERVICE_ACCOUNT_TOKEN_PATH);
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        SimpleHttp simpleHttp = SimpleHttp.create(session);

        String token = KubernetesUtils.getServiceAccountToken(issuer, tokenFile);
        String jwksUri = jwksUrl;
        if (Strings.isEmpty(jwksUri)) {
            String wellKnownEndpoint = KubernetesUtils.discoveryUrl(issuer);
            SimpleHttpRequest wellKnownRequest = simpleHttp.doGet(wellKnownEndpoint).acceptJson();
            if (token != null) {
                if (isTokenForwardingAllowed(wellKnownEndpoint)) {
                    wellKnownRequest.auth(token);
                } else {
                    logger.debugf("Not forwarding service account token to '%s' because token forwarding to additional endpoints is disabled", wellKnownEndpoint);
                }
            }

            jwksUri = wellKnownRequest.asJson(OIDCConfigurationRepresentation.class).getJwksUri();
            if (jwksUri == null) {
                throw new IOException("OIDC discovery document from " + wellKnownEndpoint + " did not include a jwks_uri");
            }
        }

        SimpleHttpRequest jwksRequest = simpleHttp.doGet(jwksUri).header(HttpHeaders.ACCEPT, "application/jwk-set+json");
        if (token != null) {
            if (isTokenForwardingAllowed(jwksUri)) {
                jwksRequest.auth(token);
            } else {
                logger.debugf("Not forwarding service account token to '%s' because token forwarding to additional endpoints is disabled", jwksUri);
            }
        }

        JSONWebKeySet jwks = jwksRequest.asJson(JSONWebKeySet.class);
        return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG);
    }

    boolean isTokenForwardingAllowed(String uri) {
        if (forwardTokenToAdditionalEndpoints) {
            return true;
        }
        return KubernetesUtils.isTrustedKubernetesEndpoint(uri);
    }

}
