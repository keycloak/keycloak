package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.Version;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.adapters.action.AdminAction;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.StreamUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PreAuthActionsHandler {

    private static final Logger log = Logger.getLogger(PreAuthActionsHandler.class);

    protected UserSessionManagement userSessionManagement;
    protected AdapterDeploymentContext deploymentContext;
    protected KeycloakDeployment deployment;
    protected HttpFacade facade;

    public PreAuthActionsHandler(UserSessionManagement userSessionManagement, AdapterDeploymentContext deploymentContext, HttpFacade facade) {
        this.userSessionManagement = userSessionManagement;
        this.deploymentContext = deploymentContext;
        this.facade = facade;
    }

    protected boolean resolveDeployment() {
        deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isConfigured()) {
            log.warn("can't take request, adapter not configured");
            facade.getResponse().sendError(403, "adapter not configured");
            return false;
        }
        return true;
    }

    public boolean handleRequest() {
        String requestUri = facade.getRequest().getURI();
        log.debugv("adminRequest {0}", requestUri);
        if (preflightCors()) {
            return true;
        }
        if (requestUri.endsWith(AdapterConstants.K_LOGOUT)) {
            if (!resolveDeployment()) return true;
            handleLogout();
            return true;
        } else if (requestUri.endsWith(AdapterConstants.K_PUSH_NOT_BEFORE)) {
            if (!resolveDeployment()) return true;
            handlePushNotBefore();
            return true;
        } else if (requestUri.endsWith(AdapterConstants.K_VERSION)) {
            handleVersion();
            return true;
        } else if (requestUri.endsWith(AdapterConstants.K_TEST_AVAILABLE)) {
            if (!resolveDeployment()) return true;
            handleTestAvailable();
            return true;
        }
        return false;
    }

    public boolean preflightCors() {
        // don't need to resolve deployment on cors requests.  Just need to know local cors config.
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isCors()) return false;
        log.debugv("checkCorsPreflight {0}", facade.getRequest().getURI());
        if (!facade.getRequest().getMethod().equalsIgnoreCase("OPTIONS")) {
            return false;
        }
        if (facade.getRequest().getHeader(CorsHeaders.ORIGIN) == null) {
            log.debug("checkCorsPreflight: no origin header");
            return false;
        }
        log.debug("Preflight request returning");
        facade.getResponse().setStatus(200);
        String origin = facade.getRequest().getHeader(CorsHeaders.ORIGIN);
        facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        String requestMethods = facade.getRequest().getHeader(CorsHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        if (requestMethods != null) {
            if (deployment.getCorsAllowedMethods() != null) {
                requestMethods = deployment.getCorsAllowedMethods();
            }
            facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS, requestMethods);
        }
        String allowHeaders = facade.getRequest().getHeader(CorsHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (allowHeaders != null) {
            if (deployment.getCorsAllowedHeaders() != null) {
                allowHeaders = deployment.getCorsAllowedHeaders();
            }
            facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
        }
        if (deployment.getCorsMaxAge() > -1) {
            facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_MAX_AGE, Integer.toString(deployment.getCorsMaxAge()));
        }
        return true;
    }

    protected void handleLogout()  {
        if (log.isTraceEnabled()) {
            log.trace("K_LOGOUT sent");
        }
        try {
            JWSInput token = verifyAdminRequest();
            if (token == null) {
                return;
            }
            LogoutAction action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
            if (!validateAction(action)) return;
            if (action.getAdapterSessionIds() != null) {
                userSessionManagement.logoutHttpSessions(action.getAdapterSessionIds());
            } else {
                log.debugf("logout of all sessions for application '%s'", action.getResource());
                if (action.getNotBefore() > deployment.getNotBefore()) {
                    deployment.setNotBefore(action.getNotBefore());
                }
                userSessionManagement.logoutAll();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    protected void handlePushNotBefore()  {
        if (log.isTraceEnabled()) {
            log.trace("K_PUSH_NOT_BEFORE sent");
        }
        try {
            JWSInput token = verifyAdminRequest();
            if (token == null) {
                return;
            }
            PushNotBeforeAction action = JsonSerialization.readValue(token.getContent(), PushNotBeforeAction.class);
            if (!validateAction(action)) return;
            deployment.setNotBefore(action.getNotBefore());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void handleTestAvailable()  {
        if (log.isTraceEnabled()) {
            log.trace("K_TEST_AVAILABLE sent");
        }
        try {
            JWSInput token = verifyAdminRequest();
            if (token == null) {
                return;
            }
            TestAvailabilityAction action = JsonSerialization.readValue(token.getContent(), TestAvailabilityAction.class);
            validateAction(action);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected JWSInput verifyAdminRequest() throws Exception {
        if (!facade.getRequest().isSecure() && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            log.warn("SSL is required for adapter admin action");
            facade.getResponse().sendError(403, "ssl required");
            return null;
        }
        String token = StreamUtil.readString(facade.getRequest().getInputStream());
        if (token == null) {
            log.warn("admin request failed, no token");
            facade.getResponse().sendError(403, "no token");
            return null;
        }

        JWSInput input = new JWSInput(token);
        boolean verified = false;
        try {
            verified = RSAProvider.verify(input, deployment.getRealmKey());
        } catch (Exception ignore) {
        }
        if (!verified) {
            log.warn("admin request failed, unable to verify token");
            facade.getResponse().sendError(403, "no token");
            return null;
        }
        return input;
    }


    protected boolean validateAction(AdminAction action)  {
        if (!action.validate()) {
            log.warn("admin request failed, not validated" + action.getAction());
            facade.getResponse().sendError(400, "Not validated");
            return false;
        }
        if (action.isExpired()) {
            log.warn("admin request failed, expired token");
            facade.getResponse().sendError(400, "Expired token");
            return false;
        }
        if (!deployment.getResourceName().equals(action.getResource())) {
            log.warn("Resource name does not match");
            facade.getResponse().sendError(400, "Resource name does not match");
            return false;

        }
        return true;
    }

    protected void handleVersion()  {
        try {
            facade.getResponse().setStatus(200);
            facade.getResponse().setHeader("Content-Type", "application/json");
            JsonSerialization.writeValueToStream(facade.getResponse().getOutputStream(), Version.SINGLETON);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
