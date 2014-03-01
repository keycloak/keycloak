package org.keycloak.adapters.as7;

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
import org.keycloak.KeycloakAuthenticatedSession;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.RefreshableKeycloakSession;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.adapters.as7.config.CatalinaAdapterConfigLoader;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.adapters.config.RealmConfigurationLoader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.StreamUtil;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
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
    protected RealmConfiguration realmConfiguration;
    private static final Logger log = Logger.getLogger(KeycloakAuthenticatorValve.class);
    protected UserSessionManagement userSessionManagement = new UserSessionManagement();
    protected AdapterConfig adapterConfig;
    protected ResourceMetadata resourceMetadata;


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

    protected void init() {
        RealmConfigurationLoader configLoader = new CatalinaAdapterConfigLoader(context);
        configLoader.init(true);
        resourceMetadata = configLoader.getResourceMetadata();
        adapterConfig = configLoader.getAdapterConfig();

        realmConfiguration = configLoader.getRealmConfiguration();
        AuthenticatedActionsValve actions = new AuthenticatedActionsValve(adapterConfig, getNext(), getContainer(), getController());
        setNext(actions);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            if (adapterConfig.isCors() && new CorsPreflightChecker(adapterConfig).checkCorsPreflight(request, response)) {
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
            if (!adapterConfig.isBearerOnly()) oauth(request, response);
        } catch (LoginException e) {
        }
        return false;
    }

    protected JWSInput verifyAdminRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = StreamUtil.readString(request.getInputStream());
        if (token == null) {
            log.warn("admin request failed, no token");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "no token");
            return null;
        }

        JWSInput input = new JWSInput(token);
        boolean verified = false;
        try {
            verified = RSAProvider.verify(input, resourceMetadata.getRealmKey());
        } catch (Exception ignore) {
        }
        if (!verified) {
            log.warn("admin request failed, unable to verify token");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "verification failed");
            return null;
        }
        return input;
    }

    protected void pushNotBefore(JWSInput token, HttpServletResponse response) throws IOException {
        try {
            log.debug("->> pushNotBefore: ");
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
            realmConfiguration.setNotBefore(action.getNotBefore());
        } catch (Exception e) {
            log.warn("failed to logout", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to logout");
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);

    }

    protected void remoteLogout(JWSInput token, HttpServletResponse response) throws IOException {
        try {
            log.debug("->> remoteLogout: ");
            LogoutAction action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
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
        boolean useResourceRoleMappings = adapterConfig.isUseResourceRoleMappings();
        CatalinaBearerTokenAuthenticator bearer = new CatalinaBearerTokenAuthenticator(resourceMetadata, realmConfiguration.getNotBefore(), challenge, useResourceRoleMappings);
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
        RefreshableKeycloakSession session = (RefreshableKeycloakSession)request.getSessionInternal().getNote(KeycloakAuthenticatedSession.class.getName());
        if (session == null) return;
        // just in case session got serialized
        session.setRealmConfiguration(realmConfiguration);
        session.setMetadata(resourceMetadata);
        if (session.isActive()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        session.refreshExpiredToken();
        if (session.isActive()) return;

        request.getSessionInternal().removeNote(KeycloakAuthenticatedSession.class.getName());
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
            KeycloakAuthenticatedSession skSession = (KeycloakAuthenticatedSession) session.getNote(KeycloakAuthenticatedSession.class.getName());
            if (skSession != null) {
                request.setAttribute(KeycloakAuthenticatedSession.class.getName(), skSession);
            }
        }
        return true;
    }

    /**
     * This method always set the HTTP response, so do not continue after invoking
     */
    protected void oauth(Request request, HttpServletResponse response) throws IOException {
        ServletOAuthLogin oauth = new ServletOAuthLogin(realmConfiguration, request, response, request.getConnector().getRedirectPort());
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
            if (adapterConfig.isUseResourceRoleMappings()) {
                AccessToken.Access access = token.getResourceAccess(resourceMetadata.getResourceName());
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
            KeycloakAuthenticatedSession skSession = new RefreshableKeycloakSession(oauth.getTokenString(), oauth.getToken(), oauth.getIdTokenString(), oauth.getIdToken(), resourceMetadata, realmConfiguration, oauth.getRefreshToken());
            session.setNote(KeycloakAuthenticatedSession.class.getName(), skSession);

            String username = token.getSubject();
            log.debug("userSessionManage.login: " + username);
            userSessionManagement.login(session, username);
        }
    }

}
