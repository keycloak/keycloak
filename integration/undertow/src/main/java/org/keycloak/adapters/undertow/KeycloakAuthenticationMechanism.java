package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.jboss.logging.Logger;
import org.keycloak.RealmConfiguration;
import org.keycloak.ResourceMetadata;
import org.keycloak.SkeletonKeyPrincipal;
import org.keycloak.SkeletonKeySession;
import org.keycloak.adapters.config.ManagedResourceConfig;
import org.keycloak.representations.SkeletonKeyToken;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import static io.undertow.util.Headers.WWW_AUTHENTICATE;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticationMechanism implements AuthenticationMechanism {
    protected Logger log = Logger.getLogger(KeycloakAuthenticationMechanism.class);

    public static final AttachmentKey<KeycloakChallenge> KEYCLOAK_CHALLENGE_ATTACHMENT_KEY = AttachmentKey.create(KeycloakChallenge.class);
    public static final AttachmentKey<SkeletonKeySession> SKELETON_KEY_SESSION_ATTACHMENT_KEY = AttachmentKey.create(SkeletonKeySession.class);

    protected ResourceMetadata resourceMetadata;
    protected ManagedResourceConfig config;
    protected RealmConfiguration realmConfig;
    protected int sslRedirectPort;

    public KeycloakAuthenticationMechanism(ResourceMetadata resourceMetadata, ManagedResourceConfig config, RealmConfiguration realmConfig, int sslRedirectPort) {
        this.resourceMetadata = resourceMetadata;
        this.config = config;
        this.realmConfig = realmConfig;
        this.sslRedirectPort = sslRedirectPort;
    }

    public KeycloakAuthenticationMechanism(ResourceMetadata resourceMetadata, ManagedResourceConfig config, RealmConfiguration realmConfig) {
        this.resourceMetadata = resourceMetadata;
        this.config = config;
        this.realmConfig = realmConfig;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        BearerTokenAuthenticator bearer = createBearerTokenAuthenticator();
        AuthenticationMechanismOutcome outcome = bearer.authenticate(exchange);
        if (outcome == AuthenticationMechanismOutcome.NOT_AUTHENTICATED) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, bearer.getChallenge());
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
        else if (outcome == AuthenticationMechanismOutcome.AUTHENTICATED) {
            final SkeletonKeyToken token = bearer.getToken();
            String surrogate = bearer.getSurrogate();
            SkeletonKeySession session = new SkeletonKeySession(bearer.getTokenString(), token, resourceMetadata);
            propagateBearer(exchange, session);
            completeAuthentication(exchange, securityContext, token, surrogate);
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        }
        else if (config.isBearerOnly()) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, bearer.getChallenge());
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        OAuthAuthenticator oauth = createOAuthAuthenticator(exchange);
        outcome = oauth.authenticate();
        if (outcome == AuthenticationMechanismOutcome.NOT_AUTHENTICATED) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, oauth.getChallenge());
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
        else if (outcome == AuthenticationMechanismOutcome.NOT_ATTEMPTED) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, oauth.getChallenge());
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;

        }
        SkeletonKeySession session = new SkeletonKeySession(oauth.getTokenString(), oauth.getToken(), resourceMetadata);
        propagateOauth(exchange, session);
        completeAuthentication(exchange, securityContext, oauth.getToken(), null);
        log.info("AUTHENTICATED");
        return AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    protected OAuthAuthenticator createOAuthAuthenticator(HttpServerExchange exchange) {
        return new OAuthAuthenticator(exchange, realmConfig, sslRedirectPort);
    }

    protected BearerTokenAuthenticator createBearerTokenAuthenticator() {
        return new BearerTokenAuthenticator(resourceMetadata, config.isUseResourceRoleMappings());
    }

    protected void completeAuthentication(HttpServerExchange exchange, SecurityContext securityContext, SkeletonKeyToken token, String surrogate) {
        final SkeletonKeyPrincipal skeletonKeyPrincipal = new SkeletonKeyPrincipal(token.getPrincipal(), surrogate);
        Set<String> roles = null;
        if (config.isUseResourceRoleMappings()) {
            SkeletonKeyToken.Access access = token.getResourceAccess(resourceMetadata.getResourceName());
            if (access != null) roles = access.getRoles();
        } else {
            SkeletonKeyToken.Access access = token.getRealmAccess();
            if (access != null) roles = access.getRoles();
        }
        if (roles == null) roles = Collections.emptySet();
        final Set<String> accountRoles = roles;
        Account account = new Account() {
            @Override
            public Principal getPrincipal() {
                return skeletonKeyPrincipal;
            }

            @Override
            public Set<String> getRoles() {
                return accountRoles;
            }
        };
        securityContext.authenticationComplete(account, "FORM");
    }

    protected void propagateBearer(HttpServerExchange exchange, SkeletonKeySession session) {
        exchange.putAttachment(SKELETON_KEY_SESSION_ATTACHMENT_KEY, session);

    }

    protected void propagateOauth(HttpServerExchange exchange, SkeletonKeySession session) {
        exchange.putAttachment(SKELETON_KEY_SESSION_ATTACHMENT_KEY, session);
    }


    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        KeycloakChallenge challenge = exchange.getAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY);
        if (challenge != null) {
            return challenge.sendChallenge(exchange, securityContext);
        }
        return new ChallengeResult(false);
    }
}
