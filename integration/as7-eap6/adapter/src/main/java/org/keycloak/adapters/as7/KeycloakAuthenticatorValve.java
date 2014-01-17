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
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.SkeletonKeyPrincipal;
import org.keycloak.SkeletonKeySession;
import org.keycloak.adapters.as7.config.CatalinaAdapterConfigLoader;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.adapters.config.RealmConfigurationLoader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.SkeletonKeyToken;
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
            }
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
            response.sendError(403, "no token");
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
            response.sendError(403, "verification failed");
            return null;
        }
        return input;
    }

    protected void remoteLogout(JWSInput token, HttpServletResponse response) throws IOException {
        try {
            log.debug("->> remoteLogout: ");
            LogoutAction action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
            if (action.isExpired()) {
                log.warn("admin request failed, expired token");
                response.sendError(400, "Expired token");
                return;
            }
            if (!resourceMetadata.getResourceName().equals(action.getResource())) {
                log.warn("Resource name does not match");
                response.sendError(400, "Resource name does not match");
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
            response.sendError(500, "Failed to logout");
        }
        response.setStatus(204);
    }

    protected boolean bearer(boolean challenge, Request request, HttpServletResponse response) throws LoginException, IOException {
        CatalinaBearerTokenAuthenticator bearer = new CatalinaBearerTokenAuthenticator(realmConfiguration.getMetadata(), challenge, adapterConfig.isUseResourceRoleMappings());
        if (bearer.login(request, response)) {
            return true;
        }
        return false;
    }

    protected boolean checkLoggedIn(Request request, HttpServletResponse response) {
        if (request.getSessionInternal() == null || request.getSessionInternal().getPrincipal() == null)
            return false;
        log.debug("remote logged in already");
        GenericPrincipal principal = (GenericPrincipal) request.getSessionInternal().getPrincipal();
        request.setUserPrincipal(principal);
        request.setAuthType("OAUTH");
        Session session = request.getSessionInternal();
        if (session != null) {
            SkeletonKeySession skSession = (SkeletonKeySession) session.getNote(SkeletonKeySession.class.getName());
            if (skSession != null) {
                request.setAttribute(SkeletonKeySession.class.getName(), skSession);
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
                response.sendError(400, "OAuth " + error);
                return;
            } else {
                saveRequest(request, request.getSessionInternal(true));
                oauth.loginRedirect();
            }
            return;
        } else {
            if (!oauth.resolveCode(code)) return;

            SkeletonKeyToken token = oauth.getToken();
            Set<String> roles = new HashSet<String>();
            if (adapterConfig.isUseResourceRoleMappings()) {
                SkeletonKeyToken.Access access = token.getResourceAccess(resourceMetadata.getResourceName());
                if (access != null) roles.addAll(access.getRoles());
            } else {
                SkeletonKeyToken.Access access = token.getRealmAccess();
                if (access != null) roles.addAll(access.getRoles());
            }
            SkeletonKeyPrincipal skp = new SkeletonKeyPrincipal(token.getSubject(), null);
            GenericPrincipal principal = new CatalinaSecurityContextHelper().createPrincipal(context.getRealm(), skp, roles);
            Session session = request.getSessionInternal(true);
            session.setPrincipal(principal);
            session.setAuthType("OAUTH");
            SkeletonKeySession skSession = new SkeletonKeySession(oauth.getTokenString(), token, realmConfiguration.getMetadata());
            session.setNote(SkeletonKeySession.class.getName(), skSession);

            String username = token.getSubject();
            log.debug("userSessionManage.login: " + username);
            userSessionManagement.login(session, username);
        }
    }

}
