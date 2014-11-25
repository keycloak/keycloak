package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.representations.AccessToken;

import javax.security.cert.X509Certificate;
import java.util.List;
/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BearerTokenRequestAuthenticator {
    protected Logger log = Logger.getLogger(BearerTokenRequestAuthenticator.class);
    protected String tokenString;
    protected AccessToken token;
    protected String surrogate;
    protected AuthChallenge challenge;
    protected KeycloakDeployment deployment;

    public BearerTokenRequestAuthenticator(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    public AuthChallenge getChallenge() {
        return challenge;
    }

    public String getTokenString() {
        return tokenString;
    }

    public AccessToken getToken() {
        return token;
    }

    public String getSurrogate() {
        return surrogate;
    }

    public AuthOutcome authenticate(HttpFacade exchange)  {
        List<String> authHeaders = exchange.getRequest().getHeaders("Authorization");
        if (authHeaders == null || authHeaders.size() == 0) {
            challenge = challengeResponse(exchange, null, null);
            return AuthOutcome.NOT_ATTEMPTED;
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
            return AuthOutcome.NOT_ATTEMPTED;
        }

        return (authenticateToken(exchange, tokenString));
    }
    
    protected AuthOutcome authenticateToken(HttpFacade exchange, String tokenString) {
        try {
            token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealm());
        } catch (VerificationException e) {
            log.error("Failed to verify token", e);
            challenge = challengeResponse(exchange, "invalid_token", e.getMessage());
            return AuthOutcome.FAILED;
        }
        if (token.getIssuedAt() < deployment.getNotBefore()) {
            log.error("Stale token");
            challenge = challengeResponse(exchange, "invalid_token", "Stale token");
            return AuthOutcome.FAILED;
        }
        boolean verifyCaller = false;
        if (deployment.isUseResourceRoleMappings()) {
            verifyCaller = token.isVerifyCaller(deployment.getResourceName());
        } else {
            verifyCaller = token.isVerifyCaller();
        }
        surrogate = null;
        if (verifyCaller) {
            if (token.getTrustedCertificates() == null || token.getTrustedCertificates().size() == 0) {
                log.warn("No trusted certificates in token");
                challenge = clientCertChallenge();
                return AuthOutcome.FAILED;
            }

            // for now, we just make sure Undertow did two-way SSL
            // assume JBoss Web verifies the client cert
            X509Certificate[] chain = new X509Certificate[0];
            try {
                chain = exchange.getCertificateChain();
            } catch (Exception ignore) {

            }
            if (chain == null || chain.length == 0) {
                log.warn("No certificates provided by undertow to verify the caller");
                challenge = clientCertChallenge();
                return AuthOutcome.FAILED;
            }
            surrogate = chain[0].getSubjectDN().getName();
        }
        return AuthOutcome.AUTHENTICATED;
    }

    protected AuthChallenge clientCertChallenge() {
        return new AuthChallenge() {
            @Override
            public boolean challenge(HttpFacade exchange) {
                // do the same thing as client cert auth
                return false;
            }
        };
    }


    protected AuthChallenge challengeResponse(HttpFacade facade, String error, String description) {
        StringBuilder header = new StringBuilder("Bearer realm=\"");
        header.append(deployment.getRealm()).append("\"");
        if (error != null) {
            header.append(", error=\"").append(error).append("\"");
        }
        if (description != null) {
            header.append(", error_description=\"").append(description).append("\"");
        }
        final String challenge = header.toString();
        return new AuthChallenge() {
            @Override
            public boolean challenge(HttpFacade facade) {
                facade.getResponse().setStatus(401);
                facade.getResponse().addHeader("WWW-Authenticate", challenge);
                return true;
            }
        };
    }
}
