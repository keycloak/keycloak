package org.keycloak.adapters.as7;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.RefreshableKeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.action.AdminAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.adapters.action.SessionStatsAction;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.adapters.action.UserStatsAction;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.StreamUtil;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Web deployment whose security is managed by a remote OAuth Skeleton Key authentication server
 * <p/>
 * Redirects browser to remote authentication server if not logged in.  Also allows OAuth Bearer Token requests
 * that contain a Skeleton Key bearer tokens.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticatorValve extends FormAuthenticator implements LifecycleListener {
    private static final Logger log = Logger.getLogger(KeycloakAuthenticatorValve.class);
    protected UserSessionManagement userSessionManagement = new UserSessionManagement();
    protected KeycloakDeployment deployment;


    @Override
    public void start() throws LifecycleException {
        super.start();
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
        cache = false;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getType() == Lifecycle.AFTER_START_EVENT) init();
    }

    private static InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        log.info("**** using " + AdapterConstants.AUTH_DATA_PARAM_NAME);
        log.info(json);
        return new ByteArrayInputStream(json.getBytes());
    }
    private static InputStream getConfigInputStream(Context context) {
        InputStream is = getJSONFromServletContext(context.getServletContext());
        if (is == null) {
            String path = context.getServletContext().getInitParameter("keycloak.config.file");
            if (path == null) {
                log.info("**** using /WEB-INF/keycloak.json");
                is = context.getServletContext().getResourceAsStream("/WEB-INF/keycloak.json");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return is;
    }


    protected void init() {
        this.deployment = KeycloakDeploymentBuilder.build(getConfigInputStream(context));
        AuthenticatedActionsValve actions = new AuthenticatedActionsValve(deployment, getNext(), getContainer(), getController());
        setNext(actions);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            if (deployment.isCors() && new CorsPreflightChecker(deployment).checkCorsPreflight(request, response)) {
                return;
            }
            String requestURI = request.getDecodedRequestURI();
            if (requestURI.endsWith(AdapterConstants.K_LOGOUT)) {
                JWSInput input = verifyAdminRequest(request, response);
                if (input == null) {
                    return; // we failed to verify the request
                }
                remoteLogout(input, response);
                return;
            } else if (requestURI.endsWith(AdapterConstants.K_PUSH_NOT_BEFORE)) {
                JWSInput input = verifyAdminRequest(request, response);
                if (input == null) {
                    return; // we failed to verify the request
                }
                pushNotBefore(input, response);
                return;
            } else if (requestURI.endsWith(AdapterConstants.K_GET_SESSION_STATS)) {
                JWSInput input = verifyAdminRequest(request, response);
                if (input == null) {
                    return; // we failed to verify the request
                }
                getSessionStats(input, response);
                return;
            } else if (requestURI.endsWith(AdapterConstants.K_GET_USER_STATS)) {
                JWSInput input = verifyAdminRequest(request, response);
                if (input == null) {
                    return; // we failed to verify the request
                }
                getUserStats(input, response);
                return;
            }
            checkKeycloakSession(request);
            super.invoke(request, response);
        } finally {
        }
    }

    @Override
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        try {
            if (bearer(false, request, response)) return true;
            else if (checkLoggedIn(request, response)) {
                if (request.getSessionInternal().getNote(Constants.FORM_REQUEST_NOTE) != null) {
                    if (restoreRequest(request, request.getSessionInternal())) {
                        log.debug("restoreRequest");
                        return (true);
                    } else {
                        log.debug("Restore of original request failed");
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return (false);
                    }
                } else {
                    return true;
                }
            }

            // initiate or continue oauth2 protocol
            if (!deployment.isBearerOnly()) oauth(request, response);
        } catch (LoginException e) {
        }
        return false;
    }

    protected JWSInput verifyAdminRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = StreamUtil.readString(request.getInputStream());
        if (token == null) {
            log.warn("admin request failed, no token");
            response.sendError(403, "no token");
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
            response.sendError(403, "verification failed");
            return null;
        }
        return input;
    }


    protected boolean validateAction(HttpServletResponse response, AdminAction action) throws IOException {
        if (!action.validate()) {
            log.warn("admin request failed, not validated" + action.getAction());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not validated");
            return false;
        }
        if (action.isExpired()) {
            log.warn("admin request failed, expired token");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expired token");
            return false;
        }
        if (!deployment.getResourceName().equals(action.getResource())) {
            log.warn("Resource name does not match");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Resource name does not match");
            return false;

        }
        return true;
    }

    protected void pushNotBefore(JWSInput token, HttpServletResponse response) throws IOException {
        log.info("->> pushNotBefore: ");
        PushNotBeforeAction action = JsonSerialization.readValue(token.getContent(), PushNotBeforeAction.class);
        if (!validateAction(response, action)) {
            return;
        }
        deployment.setNotBefore(action.getNotBefore());
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);

    }

    protected UserStats getUserStats(String user) {
        UserStats stats = new UserStats();
        Long loginTime = userSessionManagement.getUserLoginTime(user);
        if (loginTime != null) {
            stats.setLoggedIn(true);
            stats.setWhenLoggedIn(loginTime);
        } else {
            stats.setLoggedIn(false);
        }
        return stats;
    }


    protected void getSessionStats(JWSInput token, HttpServletResponse response) throws IOException {
        log.info("->> getSessionStats: ");
        SessionStatsAction action = JsonSerialization.readValue(token.getContent(), SessionStatsAction.class);
        if (!validateAction(response, action)) {
            return;
        }
        SessionStats stats = new SessionStats();
        stats.setActiveSessions(userSessionManagement.getActiveSessions());
        stats.setActiveUsers(userSessionManagement.getActiveUsers().size());
        if (action.isListUsers() && userSessionManagement.getActiveSessions() > 0) {
            Map<String, UserStats> list = new HashMap<String, UserStats>();
            for (String user : userSessionManagement.getActiveUsers()) {
                list.put(user, getUserStats(user));
            }
            stats.setUsers(list);
        }
        response.setStatus(200);
        response.setContentType("application/json");
        JsonSerialization.writeValueToStream(response.getOutputStream(), stats);

    }

    protected void getUserStats(JWSInput token, HttpServletResponse response) throws IOException {
        log.info("->> getUserStats: ");
        UserStatsAction action = JsonSerialization.readValue(token.getContent(), UserStatsAction.class);
        if (!validateAction(response, action)) {
            return;
        }
        String user = action.getUser();
        UserStats stats = getUserStats(user);
        response.setStatus(200);
        response.setContentType("application/json");
        JsonSerialization.writeValueToStream(response.getOutputStream(), stats);
    }


    protected void remoteLogout(JWSInput token, HttpServletResponse response) throws IOException {
        try {
            log.debug("->> remoteLogout: ");
            LogoutAction action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
            if (!validateAction(response, action)) {
                return;
            }
            String user = action.getUser();
            if (user != null) {
                log.debug("logout of session for: " + user);
                userSessionManagement.logout(user);
            } else {
                log.debug("logout of all sessions");
                userSessionManagement.logoutAll();
            }
        } catch (Exception e) {
            log.warn("failed to logout", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to logout");
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    protected boolean bearer(boolean challenge, Request request, HttpServletResponse response) throws LoginException, IOException {
        boolean useResourceRoleMappings = deployment.isUseResourceRoleMappings();
        CatalinaBearerTokenAuthenticator bearer = new CatalinaBearerTokenAuthenticator(deployment, challenge);
        if (bearer.login(request, response)) {
            return true;
        }
        return false;
    }

    /**
     * Checks that access token is still valid.  Will attempt refresh of token if it is not.
     *
     * @param request
     */
    protected void checkKeycloakSession(Request request) {
        if (request.getSessionInternal(false) == null || request.getSessionInternal().getPrincipal() == null) return;
        RefreshableKeycloakSession session = (RefreshableKeycloakSession)request.getSessionInternal().getNote(KeycloakSecurityContext.class.getName());
        if (session == null) return;
        // just in case session got serialized
        session.setDeployment(deployment);
        if (session.isActive()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        session.refreshExpiredToken();
        if (session.isActive()) return;

        request.getSessionInternal().removeNote(KeycloakSecurityContext.class.getName());
        request.setUserPrincipal(null);
        request.setAuthType(null);
        request.getSessionInternal().setPrincipal(null);
        request.getSessionInternal().setAuthType(null);
    }

    protected boolean checkLoggedIn(Request request, HttpServletResponse response) {
        if (request.getSessionInternal(false) == null || request.getSessionInternal().getPrincipal() == null)
            return false;
        log.debug("remote logged in already");
        GenericPrincipal principal = (GenericPrincipal) request.getSessionInternal().getPrincipal();
        request.setUserPrincipal(principal);
        request.setAuthType("KEYCLOAK");
        Session session = request.getSessionInternal();
        if (session != null) {
            KeycloakSecurityContext skSession = (KeycloakSecurityContext) session.getNote(KeycloakSecurityContext.class.getName());
            if (skSession != null) {
                request.setAttribute(KeycloakSecurityContext.class.getName(), skSession);
            }
        }
        return true;
    }

    /**
     * This method always set the HTTP response, so do not continue after invoking
     */
    protected void oauth(Request request, HttpServletResponse response) throws IOException {
        ServletOAuthLogin oauth = new ServletOAuthLogin(deployment, request, response, request.getConnector().getRedirectPort());
        String code = oauth.getCode();
        if (code == null) {
            String error = oauth.getError();
            if (error != null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth " + error);
                return;
            } else {
                saveRequest(request, request.getSessionInternal(true));
                oauth.loginRedirect();
            }
            return;
        } else {
            if (!oauth.resolveCode(code)) return;

            AccessToken token = oauth.getToken();
            Set<String> roles = new HashSet<String>();
            if (deployment.isUseResourceRoleMappings()) {
                AccessToken.Access access = token.getResourceAccess(deployment.getResourceName());
                if (access != null) roles.addAll(access.getRoles());
            } else {
                AccessToken.Access access = token.getRealmAccess();
                if (access != null) roles.addAll(access.getRoles());
            }
            KeycloakPrincipal skp = new KeycloakPrincipal(token.getSubject(), null);
            GenericPrincipal principal = new CatalinaSecurityContextHelper().createPrincipal(context.getRealm(), skp, roles);
            Session session = request.getSessionInternal(true);
            session.setPrincipal(principal);
            session.setAuthType("OAUTH");
            KeycloakSecurityContext skSession = new RefreshableKeycloakSession(deployment, oauth.getTokenString(), oauth.getToken(), oauth.getIdTokenString(), oauth.getIdToken(), oauth.getRefreshToken());
            session.setNote(KeycloakSecurityContext.class.getName(), skSession);

            String username = token.getSubject();
            log.debug("userSessionManage.login: " + username);
            userSessionManagement.login(session, username);
        }
    }

}
