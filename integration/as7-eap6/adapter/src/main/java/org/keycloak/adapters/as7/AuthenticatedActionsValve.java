package org.keycloak.adapters.as7;

import org.apache.catalina.Container;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.keycloak.SkeletonKeySession;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.SkeletonKeyToken;

import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Pre-installed actions that must be authenticated
 * <p/>
 * Actions include:
 * <p/>
 * CORS Origin Check and Response headers
 * k_query_bearer_token: Get bearer token from server for Javascripts CORS requests
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticatedActionsValve extends ValveBase {
    private static final Logger log = Logger.getLogger(AuthenticatedActionsValve.class);
    protected AdapterConfig config;

    public AuthenticatedActionsValve(AdapterConfig config, Valve next, Container container, ObjectName controller) {
        this.config = config;
        if (next == null) throw new RuntimeException("WTF is next null?!");
        setNext(next);
        setContainer(container);
        setController(controller);
    }


    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        log.debugv("AuthenticatedActionsValve.invoke {0}", request.getRequestURI());
        SkeletonKeySession session = getSkeletonKeySession(request);
        if (corsRequest(request, response, session)) return;
        String requestUri = request.getRequestURI();
        if (requestUri.endsWith(AdapterConstants.K_QUERY_BEARER_TOKEN)) {
            queryBearerToken(request, response, session);
            return;
        }
        getNext().invoke(request, response);
    }

    public SkeletonKeySession getSkeletonKeySession(Request request) {
        SkeletonKeySession skSession = (SkeletonKeySession) request.getAttribute(SkeletonKeySession.class.getName());
        if (skSession != null) return skSession;
        Session session = request.getSessionInternal();
        if (session != null) {
            return (SkeletonKeySession) session.getNote(SkeletonKeySession.class.getName());
        }
        return null;
    }

    protected void queryBearerToken(Request request, Response response, SkeletonKeySession session) throws IOException, ServletException {
        log.debugv("queryBearerToken {0}", request.getRequestURI());
        if (abortTokenResponse(request, response, session)) return;
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.getOutputStream().write(session.getTokenString().getBytes());
        response.getOutputStream().flush();

    }

    protected boolean abortTokenResponse(Request request, Response response, SkeletonKeySession session) throws IOException {
        if (session == null) {
            log.debugv("session was null, sending back 401: {0}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return true;
        }
        if (!config.isExposeToken()) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        if (!config.isCors() && request.getHeader("Origin") != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        return false;
    }

    protected boolean corsRequest(Request request, Response response, SkeletonKeySession session) throws IOException {
        if (!config.isCors()) return false;
        log.debugv("CORS enabled + request.getRequestURI()");
        String origin = request.getHeader("Origin");
        log.debugv("Origin: {0} uri: {1}", origin, request.getRequestURI());
        if (session != null && origin != null) {
            SkeletonKeyToken token = session.getToken();
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
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return true;
            }
            log.debugv("returning origin: {0}", origin);
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            log.debugv("session or origin was null: {0}", request.getRequestURI());
        }
        return false;
    }
}
