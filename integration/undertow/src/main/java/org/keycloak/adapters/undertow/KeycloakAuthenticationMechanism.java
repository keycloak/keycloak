package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticationMechanism implements AuthenticationMechanism {
    protected Logger log = Logger.getLogger(KeycloakAuthenticationMechanism.class);

    public static final AttachmentKey<KeycloakChallenge> KEYCLOAK_CHALLENGE_ATTACHMENT_KEY = AttachmentKey.create(KeycloakChallenge.class);

    protected KeycloakDeployment deployment;
    protected int sslRedirectPort;

    public KeycloakAuthenticationMechanism(KeycloakDeployment deployment, int sslRedirectPort) {
        this.deployment = deployment;
        this.sslRedirectPort = sslRedirectPort;
    }

    public KeycloakAuthenticationMechanism(KeycloakDeployment deployment) {
        this.deployment = deployment;
   }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        log.info("--> authenticate()");
        BearerTokenAuthenticator bearer = createBearerTokenAuthenticator();
        AuthenticationMechanismOutcome outcome = bearer.authenticate(exchange);
        if (outcome == AuthenticationMechanismOutcome.NOT_AUTHENTICATED) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, bearer.getChallenge());
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        } else if (outcome == AuthenticationMechanismOutcome.AUTHENTICATED) {
            completeAuthentication(exchange, securityContext, bearer);
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        } else if (deployment.isBearerOnly()) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, bearer.getChallenge());
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }
        // We cache account ourselves instead of using the Cache session handler of Undertow because
        // Undertow will return a 403 from an invalid account when calling IdentityManager.verify(Account) and
        // we want to just return NOT_ATTEMPTED so we can be redirected to relogin
        KeycloakUndertowAccount account = checkCachedAccount(exchange);
        if (account != null) {
            log.info("Cached account found");
            securityContext.authenticationComplete(account, "KEYCLOAK", false);
            propagateKeycloakContext(exchange, account);
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        }


        OAuthAuthenticator oauth = createOAuthAuthenticator(exchange);
        outcome = oauth.authenticate();
        if (outcome == AuthenticationMechanismOutcome.NOT_AUTHENTICATED) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, oauth.getChallenge());
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        } else if (outcome == AuthenticationMechanismOutcome.NOT_ATTEMPTED) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, oauth.getChallenge());
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;

        }
        completeAuthentication(exchange, securityContext, oauth);

        // redirect to strip out access code and state query parameters
        exchange.getResponseHeaders().put(Headers.LOCATION, oauth.getStrippedOauthParametersRequestUri());
        exchange.setResponseCode(302);
        exchange.endExchange();

        log.info("AUTHENTICATED");
        return AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    protected OAuthAuthenticator createOAuthAuthenticator(HttpServerExchange exchange) {
        return new OAuthAuthenticator(exchange, deployment, sslRedirectPort);
    }

    protected BearerTokenAuthenticator createBearerTokenAuthenticator() {
        return new BearerTokenAuthenticator(deployment);
    }

    protected void completeAuthentication(HttpServerExchange exchange, SecurityContext securityContext, OAuthAuthenticator oauth) {
        final KeycloakPrincipal principal = new KeycloakPrincipal(oauth.getToken().getSubject(), null);
        RefreshableKeycloakSession session = new RefreshableKeycloakSession(deployment, oauth.getTokenString(), oauth.getToken(), oauth.getIdTokenString(), oauth.getIdToken(), oauth.getRefreshToken());
        KeycloakUndertowAccount account = new KeycloakUndertowAccount(principal, session, deployment);

        // We cache account ourselves instead of using the Cache session handler of Undertow because
        // Undertow will return a 403 from an invalid account when calling IdentityManager.verify(Account) and
        // we want to just return NOT_ATTEMPTED so we can be redirected to relogin
        securityContext.authenticationComplete(account, "KEYCLOAK", false);
        login(exchange, account);
    }

    protected void login(HttpServerExchange exchange, KeycloakUndertowAccount account) {
        // complete
    }

    protected void propagateKeycloakContext(HttpServerExchange exchange, KeycloakUndertowAccount account) {
        // complete
    }


    protected void completeAuthentication(HttpServerExchange exchange, SecurityContext securityContext, BearerTokenAuthenticator bearer) {
        final KeycloakPrincipal principal = new KeycloakPrincipal(bearer.getToken().getSubject(), bearer.getSurrogate());
        RefreshableKeycloakSession session = new RefreshableKeycloakSession(deployment, bearer.getTokenString(), bearer.getToken(), null, null, null);
        KeycloakUndertowAccount account = new KeycloakUndertowAccount(principal, session, deployment);
        securityContext.authenticationComplete(account, "KEYCLOAK", false);
        propagateKeycloakContext(exchange, account);
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        KeycloakChallenge challenge = exchange.getAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY);
        if (challenge != null) {
            return challenge.sendChallenge(exchange, securityContext);
        }
        return new ChallengeResult(false);
    }

    protected KeycloakUndertowAccount checkCachedAccount(HttpServerExchange exchange) {
        return null;
    }


}
