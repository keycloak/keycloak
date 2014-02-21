package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakAuthenticatedSession;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticationMechanism implements AuthenticationMechanism {
    protected Logger log = Logger.getLogger(KeycloakAuthenticationMechanism.class);

    public static final AttachmentKey<KeycloakChallenge> KEYCLOAK_CHALLENGE_ATTACHMENT_KEY = AttachmentKey.create(KeycloakChallenge.class);
    public static final AttachmentKey<KeycloakAuthenticatedSession> SKELETON_KEY_SESSION_ATTACHMENT_KEY = AttachmentKey.create(KeycloakAuthenticatedSession.class);

    protected ResourceMetadata resourceMetadata;
    protected AdapterConfig adapterConfig;
    protected RealmConfiguration realmConfig;
    protected int sslRedirectPort;

    public KeycloakAuthenticationMechanism(AdapterConfig config, RealmConfiguration realmConfig, int sslRedirectPort) {
        this.resourceMetadata = realmConfig.getMetadata();
        this.adapterConfig = config;
        this.realmConfig = realmConfig;
        this.sslRedirectPort = sslRedirectPort;
    }

    public KeycloakAuthenticationMechanism(AdapterConfig adapterConfig, ResourceMetadata resourceMetadata) {
        this.resourceMetadata = resourceMetadata;
        this.adapterConfig = adapterConfig;
    }

    public KeycloakAuthenticationMechanism(AdapterConfig adapterConfig, RealmConfiguration realmConfig) {
        this.resourceMetadata = realmConfig.getMetadata();
        this.adapterConfig = adapterConfig;
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
            final AccessToken token = bearer.getToken();
            String surrogate = bearer.getSurrogate();
            KeycloakAuthenticatedSession session = new KeycloakAuthenticatedSession(bearer.getTokenString(), token, resourceMetadata);
            KeycloakPrincipal principal = completeAuthentication(securityContext, token, surrogate);
            propagateBearer(exchange, session, principal);
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        }
        else if (adapterConfig.isBearerOnly()) {
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
        KeycloakAuthenticatedSession session = new KeycloakAuthenticatedSession(oauth.getTokenString(), oauth.getToken(), resourceMetadata);
        KeycloakPrincipal principal = completeAuthentication(securityContext, oauth.getToken(), null);
        propagateOauth(exchange, session, principal);
        log.info("AUTHENTICATED");
        return AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    protected OAuthAuthenticator createOAuthAuthenticator(HttpServerExchange exchange) {
        return new OAuthAuthenticator(exchange, realmConfig, sslRedirectPort);
    }

    protected BearerTokenAuthenticator createBearerTokenAuthenticator() {
        return new BearerTokenAuthenticator(resourceMetadata, adapterConfig.isUseResourceRoleMappings());
    }

    protected KeycloakPrincipal completeAuthentication(SecurityContext securityContext, AccessToken token, String surrogate) {
        final KeycloakPrincipal skeletonKeyPrincipal = new KeycloakPrincipal(token.getSubject(), surrogate);
        Set<String> roles = null;
        if (adapterConfig.isUseResourceRoleMappings()) {
            AccessToken.Access access = token.getResourceAccess(resourceMetadata.getResourceName());
            if (access != null) roles = access.getRoles();
        } else {
            AccessToken.Access access = token.getRealmAccess();
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
        securityContext.authenticationComplete(account, "KEYCLOAK", true);
        return skeletonKeyPrincipal;
    }

    protected void propagateBearer(HttpServerExchange exchange, KeycloakAuthenticatedSession session, KeycloakPrincipal principal) {
        exchange.putAttachment(SKELETON_KEY_SESSION_ATTACHMENT_KEY, session);

    }

    protected void propagateOauth(HttpServerExchange exchange, KeycloakAuthenticatedSession session, KeycloakPrincipal principal) {
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
