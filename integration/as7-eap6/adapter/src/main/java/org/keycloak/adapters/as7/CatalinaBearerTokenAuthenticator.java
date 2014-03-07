package org.keycloak.adapters.as7;

import org.apache.catalina.connector.Request;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.RSATokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.VerificationException;
import org.keycloak.representations.AccessToken;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaBearerTokenAuthenticator {
    protected boolean challenge;
    protected Logger log = Logger.getLogger(CatalinaBearerTokenAuthenticator.class);
    protected String tokenString;
    protected AccessToken token;
    protected Principal principal;
    protected KeycloakDeployment deployment;

    public CatalinaBearerTokenAuthenticator(KeycloakDeployment deployment, boolean challenge) {
        this.deployment = deployment;
        this.challenge = challenge;
    }

    public String getTokenString() {
        return tokenString;
    }

    public AccessToken getToken() {
        return token;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public boolean login(Request request, HttpServletResponse response) throws LoginException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            if (challenge) {
                challengeResponse(response, null, null);
                return false;
            } else {
                return false;
            }
        }

        String[] split = authHeader.trim().split("\\s+");
        if (split == null || split.length != 2) challengeResponse(response, null, null);
        if (!split[0].equalsIgnoreCase("Bearer")) challengeResponse(response, null, null);


        tokenString = split[1];

        try {
            token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealm());
        } catch (VerificationException e) {
            log.error("Failed to verify token", e);
            challengeResponse(response, "invalid_token", e.getMessage());
        }

        if (token.getIssuedAt() < deployment.getNotBefore()) {
            log.error("Stale token");
            challengeResponse(response, "invalid_token", "Stale token");
        }

        boolean verifyCaller = false;
        Set<String> roles = new HashSet<String>();
        if (deployment.isUseResourceRoleMappings()) {
            AccessToken.Access access = token.getResourceAccess(deployment.getResourceName());
            if (access != null) roles = access.getRoles();
            verifyCaller = token.isVerifyCaller(deployment.getResourceName());
        } else {
            verifyCaller = token.isVerifyCaller();
            AccessToken.Access access = token.getRealmAccess();
            if (access != null) roles = access.getRoles();
        }
        String surrogate = null;
        if (verifyCaller) {
            if (token.getTrustedCertificates() == null || token.getTrustedCertificates().size() == 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                throw new LoginException("No trusted certificates in token");
            }
            // for now, we just make sure JBoss Web did two-way SSL
            // assume JBoss Web verifies the client cert
            X509Certificate[] chain = request.getCertificateChain();
            if (chain == null || chain.length == 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                throw new LoginException("No certificates provided by jboss web to verify the caller");
            }
            surrogate = chain[0].getSubjectX500Principal().getName();
        }
        KeycloakPrincipal skeletonKeyPrincipal = new KeycloakPrincipal(token.getSubject(), surrogate);
        principal = new CatalinaSecurityContextHelper().createPrincipal(request.getContext().getRealm(), skeletonKeyPrincipal, roles);
        request.setUserPrincipal(principal);
        request.setAuthType("KEYCLOAK");
        KeycloakSecurityContext skSession = new KeycloakSecurityContext(tokenString, token, null, null);
        request.setAttribute(KeycloakSecurityContext.class.getName(), skSession);

        return true;
    }


    protected void challengeResponse(HttpServletResponse response, String error, String description) throws LoginException {
        StringBuilder header = new StringBuilder("Bearer realm=\"");
        header.append(deployment.getRealm()).append("\"");
        if (error != null) {
            header.append(", error=\"").append(error).append("\"");
        }
        if (description != null) {
            header.append(", error_description=\"").append(description).append("\"");
        }
        response.setHeader("WWW-Authenticate", header.toString());
        try {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new LoginException("Challenged");
    }
}
