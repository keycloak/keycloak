package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.representations.AccessToken;

import java.io.IOException;
import java.util.Set;

/**
 * Pre-installed actions that must be authenticated
 *
 * Actions include:
 *
 * CORS Origin Check and Response headers
 * k_query_bearer_token: Get bearer token from server for Javascripts CORS requests
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticatedActionsHandler {
    private static final Logger log = Logger.getLogger(AuthenticatedActionsHandler.class);
    protected KeycloakDeployment deployment;
    protected HttpFacade facade;

    public AuthenticatedActionsHandler(KeycloakDeployment deployment, HttpFacade facade) {
        this.deployment = deployment;
        this.facade = facade;
    }

    public boolean handledRequest() {
        log.debugv("AuthenticatedActionsValve.invoke {0}", facade.getRequest().getURI());
        if (corsRequest()) return true;
        String requestUri = facade.getRequest().getURI();
        if (requestUri.endsWith(AdapterConstants.K_QUERY_BEARER_TOKEN)) {
            queryBearerToken();
            return true;
        }
        return false;
    }

    protected void queryBearerToken()  {
        log.debugv("queryBearerToken {0}",facade.getRequest().getURI());
        if (abortTokenResponse()) return;
        facade.getResponse().setStatus(200);
        facade.getResponse().setHeader("Content-Type", "text/plain");
        try {
            facade.getResponse().getOutputStream().write(facade.getSecurityContext().getTokenString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        facade.getResponse().end();
    }

    protected boolean abortTokenResponse() {
        if (facade.getSecurityContext() == null) {
            log.debugv("Not logged in, sending back 401: {0}",facade.getRequest().getURI());
            facade.getResponse().setStatus(401);
            facade.getResponse().end();
            return true;
        }
        if (!deployment.isExposeToken()) {
            facade.getResponse().setStatus(200);
            facade.getResponse().end();
             return true;
        }
        // Don't allow a CORS request if we're not validating CORS requests.
        if (!deployment.isCors() && facade.getRequest().getHeader(CorsHeaders.ORIGIN) != null) {
            facade.getResponse().setStatus(200);
            facade.getResponse().end();
            return true;
        }
        return false;
    }

    protected boolean corsRequest()  {
        if (!deployment.isCors()) return false;
        KeycloakSecurityContext securityContext = facade.getSecurityContext();
        String origin = facade.getRequest().getHeader(CorsHeaders.ORIGIN);
        log.debugv("Origin: {0} uri: {1}", origin, facade.getRequest().getURI());
        if (securityContext != null && origin != null) {
            AccessToken token = securityContext.getToken();
            Set<String> allowedOrigins = token.getAllowedOrigins();
            if (log.isDebugEnabled()) {
                for (String a : allowedOrigins) log.debug("   " + a);
            }
            if (allowedOrigins == null || (!allowedOrigins.contains("*") && !allowedOrigins.contains(origin))) {
                if (allowedOrigins == null) {
                    log.debugv("allowedOrigins was null in token");
                }
                if (!allowedOrigins.contains("*") && !allowedOrigins.contains(origin)) {
                    log.debugv("allowedOrigins did not contain origin");

                }
                facade.getResponse().setStatus(403);
                facade.getResponse().end();
                return true;
            }
            log.debugv("returning origin: {0}", origin);
            facade.getResponse().setStatus(200);
            facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            log.debugv("cors validation not needed as we're not a secure session or origin header was null: {0}", facade.getRequest().getURI());
        }
        return false;
    }
}
