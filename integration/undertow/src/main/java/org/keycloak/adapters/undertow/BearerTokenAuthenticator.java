package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import org.jboss.logging.Logger;
import org.keycloak.RSATokenVerifier;
import org.keycloak.ResourceMetadata;
import org.keycloak.VerificationException;
import org.keycloak.representations.SkeletonKeyToken;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.undertow.util.Headers.AUTHORIZATION;
import static io.undertow.util.Headers.WWW_AUTHENTICATE;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BearerTokenAuthenticator {
    protected ResourceMetadata resourceMetadata;
    protected Logger log = Logger.getLogger(BearerTokenAuthenticator.class);
    protected String tokenString;
    protected SkeletonKeyToken token;
    protected boolean useResourceRoleMappings;
    protected String surrogate;
    protected KeycloakChallenge challenge;

    public BearerTokenAuthenticator(ResourceMetadata resourceMetadata, boolean useResourceRoleMappings) {
        this.resourceMetadata = resourceMetadata;
        this.useResourceRoleMappings = useResourceRoleMappings;
    }

    public KeycloakChallenge getChallenge() {
        return challenge;
    }

    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    public String getTokenString() {
        return tokenString;
    }

    public SkeletonKeyToken getToken() {
        return token;
    }

    public String getSurrogate() {
        return surrogate;
    }

    public AuthenticationMechanism.AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange)  {
        List<String> authHeaders = exchange.getRequestHeaders().get(AUTHORIZATION);
        if (authHeaders == null || authHeaders.size() == 0) {
            challenge = challengeResponse(exchange, null, null);
            return AuthenticationMechanism.AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        tokenString = null;
        for (String authHeader : authHeaders) {
            String[] split = authHeader.trim().split("\\s+");
            if (split == null || split.length != 2) continue;
            if (!split[0].equalsIgnoreCase("Bearer")) continue;
            tokenString = split[1];
        }

        if (tokenString == null) {
            challenge = challengeResponse(exchange, null, null);
            return AuthenticationMechanism.AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        try {
            token = RSATokenVerifier.verifyToken(tokenString, resourceMetadata);
        } catch (VerificationException e) {
            log.error("Failed to verify token", e);
            challenge = challengeResponse(exchange, "invalid_token", e.getMessage());
            return AuthenticationMechanism.AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
        boolean verifyCaller = false;
        Set<String> roles = new HashSet<String>();
        if (useResourceRoleMappings) {
            verifyCaller = token.isVerifyCaller(resourceMetadata.getResourceName());
        } else {
            verifyCaller = token.isVerifyCaller();
        }
        surrogate = null;
        if (verifyCaller) {
            if (token.getTrustedCertificates() == null || token.getTrustedCertificates().size() == 0) {
                log.warn("No trusted certificates in token");
                challenge = clientCertChallenge();
                return AuthenticationMechanism.AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }

            // for now, we just make sure Undertow did two-way SSL
            // assume JBoss Web verifies the client cert
            X509Certificate[] chain = new X509Certificate[0];
            try {
                chain = exchange.getConnection().getSslSessionInfo().getPeerCertificateChain();
            } catch (SSLPeerUnverifiedException ignore) {

            }
            if (chain == null || chain.length == 0) {
                log.warn("No certificates provided by undertow to verify the caller");
                challenge = clientCertChallenge();
                return AuthenticationMechanism.AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
            surrogate = chain[0].getSubjectDN().getName();
        }
        return AuthenticationMechanism.AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    protected KeycloakChallenge clientCertChallenge() {
        return new KeycloakChallenge() {
            @Override
            public AuthenticationMechanism.ChallengeResult sendChallenge(HttpServerExchange httpServerExchange, SecurityContext securityContext) {
                // do the same thing as client cert auth
                return new AuthenticationMechanism.ChallengeResult(false);
            }
        };
    }


    protected KeycloakChallenge challengeResponse(HttpServerExchange exchange, String error, String description) {
        StringBuilder header = new StringBuilder("Bearer realm=\"");
        header.append(resourceMetadata.getRealm()).append("\"");
        if (error != null) {
            header.append(", error=\"").append(error).append("\"");
        }
        if (description != null) {
            header.append(", error_description=\"").append(description).append("\"");
        }
        String challenge = header.toString();
        exchange.getResponseHeaders().add(WWW_AUTHENTICATE, challenge);
        return new KeycloakChallenge() {
            @Override
            public AuthenticationMechanism.ChallengeResult sendChallenge(HttpServerExchange httpServerExchange, SecurityContext securityContext) {
                return new AuthenticationMechanism.ChallengeResult(true, UNAUTHORIZED);
            }
        };
    }
}
