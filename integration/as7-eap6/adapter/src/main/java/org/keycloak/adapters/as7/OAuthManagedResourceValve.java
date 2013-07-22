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
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.RealmConfiguration;
import org.keycloak.ResourceMetadata;
import org.keycloak.SkeletonKeyPrincipal;
import org.keycloak.SkeletonKeySession;
import org.keycloak.adapters.as7.config.ManagedResourceConfig;
import org.keycloak.adapters.as7.config.ManagedResourceConfigLoader;
import org.keycloak.representations.SkeletonKeyToken;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
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
public class OAuthManagedResourceValve extends FormAuthenticator implements LifecycleListener {
    protected RealmConfiguration realmConfiguration;
    private static final Logger log = Logger.getLogger(OAuthManagedResourceValve.class);
    protected UserSessionManagement userSessionManagement = new UserSessionManagement();
    protected ManagedResourceConfig remoteSkeletonKeyConfig;
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
        ManagedResourceConfigLoader managedResourceConfigLoader = new ManagedResourceConfigLoader(context);
        managedResourceConfigLoader.init(true);
        resourceMetadata = managedResourceConfigLoader.getResourceMetadata();
        remoteSkeletonKeyConfig = managedResourceConfigLoader.getRemoteSkeletonKeyConfig();
        String client_id = remoteSkeletonKeyConfig.getClientId();
        if (client_id == null) {
            throw new IllegalArgumentException("Must set client-id to use with auth server");
        }
        realmConfiguration = new RealmConfiguration();
        String authUrl = remoteSkeletonKeyConfig.getAuthUrl();
        if (authUrl == null) {
            throw new RuntimeException("You must specify auth-url");
        }
        String tokenUrl = remoteSkeletonKeyConfig.getCodeUrl();
        if (tokenUrl == null) {
            throw new RuntimeException("You mut specify code-url");
        }
        realmConfiguration.setMetadata(resourceMetadata);
        realmConfiguration.setClientId(client_id);
        realmConfiguration.setSslRequired(!remoteSkeletonKeyConfig.isSslNotRequired());

        for (Map.Entry<String, String> entry : managedResourceConfigLoader.getRemoteSkeletonKeyConfig().getClientCredentials().entrySet()) {
            realmConfiguration.getCredentials().param(entry.getKey(), entry.getValue());
        }

        ResteasyClient client = managedResourceConfigLoader.getClient();

        realmConfiguration.setClient(client);
        realmConfiguration.setAuthUrl(UriBuilder.fromUri(authUrl).queryParam("client_id", client_id));
        realmConfiguration.setCodeUrl(client.target(tokenUrl));
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            String requestURI = request.getDecodedRequestURI();
            if (requestURI.endsWith("j_oauth_remote_logout")) {
                remoteLogout(request, response);
                return;
            }
            super.invoke(request, response);
        } finally {
            ResteasyProviderFactory.clearContextData(); // to clear push of SkeletonKeySession
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
            oauth(request, response);
        } catch (LoginException e) {
        }
        return false;
    }

    protected void remoteLogout(Request request, HttpServletResponse response) throws IOException {
        try {
            log.debug("->> remoteLogout: ");
            if (!bearer(true, request, response)) {
                log.debug("remoteLogout: bearer auth failed");
                return;
            }
            GenericPrincipal gp = (GenericPrincipal) request.getPrincipal();
            if (!gp.hasRole(remoteSkeletonKeyConfig.getAdminRole())) {
                log.debug("remoteLogout: role failure");
                response.sendError(403);
                return;
            }
            String user = request.getParameter("user");
            if (user != null) {
                userSessionManagement.logout(user);
            } else {
                userSessionManagement.logoutAll();
            }
        } catch (Exception e) {
            log.error("failed to logout", e);
        }
        response.setStatus(204);
    }

    protected boolean bearer(boolean challenge, Request request, HttpServletResponse response) throws LoginException, IOException {
        CatalinaBearerTokenAuthenticator bearer = new CatalinaBearerTokenAuthenticator(realmConfiguration.getMetadata(), !remoteSkeletonKeyConfig.isCancelPropagation(), challenge);
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
        if (session != null && !remoteSkeletonKeyConfig.isCancelPropagation()) {
            SkeletonKeySession skSession = (SkeletonKeySession) session.getNote(SkeletonKeySession.class.getName());
            if (skSession != null) {
                request.setAttribute(SkeletonKeySession.class.getName(), skSession);
                ResteasyProviderFactory.pushContext(SkeletonKeySession.class, skSession);

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
            Set<String> roles = null;
            if (resourceMetadata.getResourceName() != null) {
                SkeletonKeyToken.Access access = token.getResourceAccess(resourceMetadata.getResourceName());
                if (access != null) roles = access.getRoles();
            } else {
                SkeletonKeyToken.Access access = token.getRealmAccess();
                if (access != null) roles = access.getRoles();
            }
            SkeletonKeyPrincipal skp = new SkeletonKeyPrincipal(token.getPrincipal(), null);
            GenericPrincipal principal = new CatalinaSecurityContextHelper().createPrincipal(context.getRealm(), skp, roles);
            Session session = request.getSessionInternal(true);
            session.setPrincipal(principal);
            session.setAuthType("OAUTH");
            if (!remoteSkeletonKeyConfig.isCancelPropagation()) {
                SkeletonKeySession skSession = new SkeletonKeySession(oauth.getTokenString(), realmConfiguration.getMetadata());
                session.setNote(SkeletonKeySession.class.getName(), skSession);
            }

            String username = token.getPrincipal();
            log.debug("userSessionManage.login: " + username);
            userSessionManagement.login(session, username);
        }
    }

}
