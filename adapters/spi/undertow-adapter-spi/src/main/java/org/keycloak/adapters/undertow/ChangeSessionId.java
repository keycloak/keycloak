package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.servlet.spec.ServletContextImpl;

import java.lang.reflect.Method;
import java.security.AccessController;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ChangeSessionId {
    /**
     * This is a hack to be backward compatible between Undertow 1.3+ and versions lower.  In Undertow 1.3, a new
     * switch was added setChangeSessionIdOnLogin, this screws up session management for keycloak as after the session id
     * is uploaded to Keycloak, undertow changes the session id and it can't be invalidated.
     *
     * @param deploymentInfo
     */
    public static void turnOffChangeSessionIdOnLogin(DeploymentInfo deploymentInfo) {
        try {
            Method method = DeploymentInfo.class.getMethod("setChangeSessionIdOnLogin", boolean.class);
            method.invoke(deploymentInfo, false);
        } catch (Exception ignore) {

        }
    }

    public static String changeSessionId(HttpServerExchange exchange, boolean create) {
        final ServletRequestContext sc = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        ServletContextImpl currentServletContext = sc.getCurrentServletContext();
        HttpSessionImpl session = currentServletContext.getSession(exchange, create);
        if (session == null) {
            return null;
        }
        Session underlyingSession;
        if(System.getSecurityManager() == null) {
            underlyingSession = session.getSession();
        } else {
            underlyingSession = AccessController.doPrivileged(new HttpSessionImpl.UnwrapSessionAction(session));
        }


        return underlyingSession.changeSessionId(exchange, currentServletContext.getSessionConfig());
    }
}
