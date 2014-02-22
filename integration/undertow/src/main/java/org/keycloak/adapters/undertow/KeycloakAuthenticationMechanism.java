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
            completeAuthentication(securityContext, bearer);
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
        completeAuthentication(securityContext, oauth);
        log.info("AUTHENTICATED");
        return AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    protected OAuthAuthenticator createOAuthAuthenticator(HttpServerExchange exchange) {
        return new OAuthAuthenticator(exchange, realmConfig, sslRedirectPort);
    }

    protected BearerTokenAuthenticator createBearerTokenAuthenticator() {
        return new BearerTokenAuthenticator(resourceMetadata, adapterConfig.isUseResourceRoleMappings());
    }

    protected void completeAuthentication(SecurityContext securityContext, OAuthAuthenticator oauth) {
        final KeycloakPrincipal principal = new KeycloakPrincipal(oauth.getToken().getSubject(), null);
        KeycloakUndertowAccount account = new KeycloakUndertowAccount(principal, oauth.getToken(), oauth.getTokenString(), oauth.getRefreshToken(), realmConfig, resourceMetadata, adapterConfig);
        securityContext.authenticationComplete(account, "KEYCLOAK", true);
    }


    protected void completeAuthentication(SecurityContext securityContext, BearerTokenAuthenticator bearer) {
        final KeycloakPrincipal principal = new KeycloakPrincipal(bearer.getToken().getSubject(), bearer.getSurrogate());
        KeycloakUndertowAccount account = new KeycloakUndertowAccount(principal, bearer.getToken(), bearer.getTokenString(), null, realmConfig, resourceMetadata, adapterConfig);
        securityContext.authenticationComplete(account, "KEYCLOAK", false);
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
