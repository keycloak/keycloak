package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterAdminResourceConstants;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
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

    public static class Wrapper implements HandlerWrapper {
        protected RealmConfiguration realmConfig;
        protected UserSessionManagement userSessionManagement;

        public Wrapper(RealmConfiguration realmConfig, UserSessionManagement userSessionManagement) {
            this.realmConfig = realmConfig;
            this.userSessionManagement = userSessionManagement;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new ServletAdminActionsHandler(realmConfig, userSessionManagement, handler);
        }
    }

    protected ServletAdminActionsHandler(RealmConfiguration realmConfig,
                                         UserSessionManagement userSessionManagement,
                                         HttpHandler next) {
        this.next = next;
        this.userSessionManagement = userSessionManagement;
        this.realmConfig = realmConfig;
    }

    protected JWSInput verifyAdminRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = StreamUtil.readString(request.getInputStream());
        if (token == null) {
            log.warn("admin request failed, no token");
            response.sendError(403, "no token");
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
            response.sendError(403, "verification failed");
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
        if (requestUri.endsWith(AdapterAdminResourceConstants.LOGOUT)) {
            JWSInput token = verifyAdminRequest(request, response);
            if (token == null) return;
            userSessionManagement.remoteLogout(token, manager, response);
            return;
        } else {
            next.handleRequest(exchange);
            return;
        }
    }
}
