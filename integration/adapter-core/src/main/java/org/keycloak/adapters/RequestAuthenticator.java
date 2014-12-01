package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class RequestAuthenticator {
    protected static Logger log = Logger.getLogger(RequestAuthenticator.class);

    protected HttpFacade facade;
    protected KeycloakDeployment deployment;
    protected AdapterTokenStore tokenStore;
    protected AuthChallenge challenge;
    protected int sslRedirectPort;

    public RequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, AdapterTokenStore tokenStore, int sslRedirectPort) {
        this.facade = facade;
        this.deployment = deployment;
        this.tokenStore = tokenStore;
        this.sslRedirectPort = sslRedirectPort;
    }

    public RequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment) {
        this.facade = facade;
        this.deployment = deployment;
    }

    public AuthChallenge getChallenge() {
        return challenge;
    }

    public AuthOutcome authenticate() {
        if (log.isTraceEnabled()) {
            log.trace("--> authenticate()");
        }

        BearerTokenRequestAuthenticator bearer = createBearerTokenAuthenticator();
        if (log.isTraceEnabled()) {
            log.trace("try bearer");
        }
        
        AuthOutcome outcome = bearer.authenticate(facade);
        if (outcome == AuthOutcome.FAILED) {
            challenge = bearer.getChallenge();
            log.debug("Bearer FAILED");
            return AuthOutcome.FAILED;
        } else if (outcome == AuthOutcome.AUTHENTICATED) {
            if (verifySSL()) return AuthOutcome.FAILED;
            completeAuthentication(bearer, "KEYCLOAK");
            log.debug("Bearer AUTHENTICATED");
            return AuthOutcome.AUTHENTICATED;
        } else if (deployment.isBearerOnly()) {
            challenge = bearer.getChallenge();
            log.debug("NOT_ATTEMPTED: bearer only");
            return AuthOutcome.NOT_ATTEMPTED;
        }

        if (deployment.isEnableBasicAuth()) {
            BasicAuthRequestAuthenticator basicAuth = createBasicAuthAuthenticator();
            if (log.isTraceEnabled()) {
                log.trace("try basic auth");
            }
    
            outcome = basicAuth.authenticate(facade);
            if (outcome == AuthOutcome.FAILED) {
                challenge = basicAuth.getChallenge();
                log.debug("BasicAuth FAILED");
                return AuthOutcome.FAILED;
            } else if (outcome == AuthOutcome.AUTHENTICATED) {
                log.debug("BasicAuth AUTHENTICATED");
                completeAuthentication(basicAuth, "BASIC");
                return AuthOutcome.AUTHENTICATED;
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("try oauth");
        }

        if (tokenStore.isCached(this)) {
            if (verifySSL()) return AuthOutcome.FAILED;
            log.debug("AUTHENTICATED: was cached");
            return AuthOutcome.AUTHENTICATED;
        }

        OAuthRequestAuthenticator oauth = createOAuthAuthenticator();
        outcome = oauth.authenticate();
        if (outcome == AuthOutcome.FAILED) {
            challenge = oauth.getChallenge();
            return AuthOutcome.FAILED;
        } else if (outcome == AuthOutcome.NOT_ATTEMPTED) {
            challenge = oauth.getChallenge();
            return AuthOutcome.NOT_ATTEMPTED;

        }

        if (verifySSL()) return AuthOutcome.FAILED;

        completeAuthentication(oauth);

        // redirect to strip out access code and state query parameters
        facade.getResponse().setHeader("Location", oauth.getStrippedOauthParametersRequestUri());
        facade.getResponse().setStatus(302);
        facade.getResponse().end();

        log.debug("AUTHENTICATED");
        return AuthOutcome.AUTHENTICATED;
    }

    protected boolean verifySSL() {
        if (!facade.getRequest().isSecure() && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            log.warn("SSL is required to authenticate");
            return true;
        }
        return false;
    }

    protected abstract OAuthRequestAuthenticator createOAuthAuthenticator();

    protected BearerTokenRequestAuthenticator createBearerTokenAuthenticator() {
        return new BearerTokenRequestAuthenticator(deployment);
    }

    protected BasicAuthRequestAuthenticator createBasicAuthAuthenticator() {
        return new BasicAuthRequestAuthenticator(deployment);
    }

    protected void completeAuthentication(OAuthRequestAuthenticator oauth) {
        RefreshableKeycloakSecurityContext session = new RefreshableKeycloakSecurityContext(deployment, tokenStore, oauth.getTokenString(), oauth.getToken(), oauth.getIdTokenString(), oauth.getIdToken(), oauth.getRefreshToken());
        final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(AdapterUtils.getPrincipalName(deployment, oauth.getToken()), session);
        completeOAuthAuthentication(principal);
    }

    protected abstract void completeOAuthAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal);
    protected abstract void completeBearerAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, String method);
    protected abstract String getHttpSessionId(boolean create);

    protected void completeAuthentication(BearerTokenRequestAuthenticator bearer, String method) {
        RefreshableKeycloakSecurityContext session = new RefreshableKeycloakSecurityContext(deployment, null, bearer.getTokenString(), bearer.getToken(), null, null, null);
        final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(AdapterUtils.getPrincipalName(deployment, bearer.getToken()), session);
        completeBearerAuthentication(principal, method);
    }

}
