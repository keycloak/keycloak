package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.StatusCodes;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.StreamUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletAdminActionsHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(ServletAdminActionsHandler.class);
    protected HttpHandler next;
    protected UserSessionManagement userSessionManagement;
    protected RealmConfiguration realmConfig;
    protected ResourceMetadata resourceMetadata;

    public static class Wrapper implements HandlerWrapper {
        protected RealmConfiguration realmConfig;
        protected ResourceMetadata resourceMetadata;
        protected UserSessionManagement userSessionManagement;


        public Wrapper(RealmConfiguration realmConfig, ResourceMetadata resourceMetadata, UserSessionManagement userSessionManagement) {
            this.realmConfig = realmConfig;
            this.resourceMetadata = resourceMetadata;
            this.userSessionManagement = userSessionManagement;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new ServletAdminActionsHandler(realmConfig, resourceMetadata, userSessionManagement, handler);
        }
    }

    protected ServletAdminActionsHandler(RealmConfiguration realmConfig,
                                         ResourceMetadata resourceMetadata,
                                         UserSessionManagement userSessionManagement,
                                         HttpHandler next) {
        this.next = next;
        this.resourceMetadata = resourceMetadata;
        this.userSessionManagement = userSessionManagement;
        this.realmConfig = realmConfig;
    }

    protected JWSInput verifyAdminRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = StreamUtil.readString(request.getInputStream());
        if (token == null) {
            log.warn("admin request failed, no token");
            response.sendError(StatusCodes.FORBIDDEN, "no token");
            return null;
        }

        JWSInput input = new JWSInput(token);
        boolean verified = false;
        try {
            verified = RSAProvider.verify(input, realmConfig.getMetadata().getRealmKey());
        } catch (Exception ignore) {
        }
        if (!verified) {
            log.warn("admin request failed, unable to verify token");
            response.sendError(StatusCodes.FORBIDDEN, "verification failed");
            return null;
        }
        return input;
    }




    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        log.debugv("adminActions {0}", exchange.getRequestURI());
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest request = (HttpServletRequest) servletRequestContext.getServletRequest();
        HttpServletResponse response = (HttpServletResponse) servletRequestContext.getServletResponse();
        SessionManager manager = servletRequestContext.getDeployment().getSessionManager();
        String requestUri = exchange.getRequestURI();
        if (requestUri.endsWith(AdapterConstants.K_LOGOUT)) {
            log.info("K_LOGOUT sent");
            JWSInput token = verifyAdminRequest(request, response);
            if (token == null) return;
            userSessionManagement.remoteLogout(token, manager, response);
            return;
        } else if (requestUri.endsWith(AdapterConstants.K_PUSH_NOT_BEFORE)) {
            handlePushNotBefore(request, response);
            return;
        } else {
            next.handleRequest(exchange);
            return;
        }
    }

    protected void handlePushNotBefore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("K_PUSH_NOT_BEFORE sent");
        JWSInput token = verifyAdminRequest(request, response);
        if (token == null) return;
        PushNotBeforeAction action = JsonSerialization.readValue(token.getContent(), PushNotBeforeAction.class);
        if (action.isExpired()) {
            log.warn("admin request failed, expired token");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expired token");
            return;
        }
        if (!resourceMetadata.getResourceName().equals(action.getResource())) {
            log.warn("Resource name does not match");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Resource name does not match");
            return;

        }
        realmConfig.setNotBefore(action.getNotBefore());
        return;
    }
}
