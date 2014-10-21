package org.keycloak.adapters.as7;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationLifecycle;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.keycloak.adapters.KeycloakConfigResolver;

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
    protected CatalinaUserSessionManagement userSessionManagement = new CatalinaUserSessionManagement();
    protected AdapterDeploymentContext deploymentContext;
    protected NodesRegistrationLifecycle nodesRegistrationLifecycle;


    @Override
    public void start() throws LifecycleException {
        super.start();
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
        cache = false;
    }

    @Override
    public void logout(Request request) throws ServletException {
        CatalinaHttpFacade facade = new CatalinaHttpFacade(request, null);
        KeycloakSecurityContext ksc = (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
        if (ksc != null) {
            request.removeAttribute(KeycloakSecurityContext.class.getName());
            Session session = request.getSessionInternal(false);
            if (session != null) {
                session.removeNote(KeycloakSecurityContext.class.getName());
                if (ksc instanceof RefreshableKeycloakSecurityContext) {
                    ((RefreshableKeycloakSecurityContext)ksc).logout(deploymentContext.getDeployment(facade.getRequest()));
                }
            }
        }
        super.logout(request);
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getType() == Lifecycle.AFTER_START_EVENT) {
            init();
        } else if (event.getType() == Lifecycle.BEFORE_STOP_EVENT) {
            beforeStop();
        }
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
        KeycloakConfigResolver configResolver = null;
        String configResolverClass = (String) context.getServletContext().getAttribute("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                configResolver = (KeycloakConfigResolver) context.getLoader().getClassLoader().loadClass(configResolverClass).newInstance();
                log.info("Using " + configResolverClass + " to resolve Keycloak configuration on a per-request basis.");
            } catch (Exception ex) {
                // TODO: should it re-throw the exception?
                //
                // Reasoning: if the explicitly defined `resolver` wasn't found, it *will* behave differently
                // than the user expects!
                //
                // Counter-reasoning: the original code doesn't throws an exception if keycloak is unconfigured, ie,
                // it's already not doing what the user would generally expect, but might be OK for development environments
                // so, we probably shouldn't block the deployment of the user's application because of keycloak's configuration
                // problem
                log.warn("The specified resolver " + configResolverClass + " could NOT be loaded. Falling back to standard behavior. Reason: " + ex.getMessage());
            }
        }

        if (configResolver != null) {
            deploymentContext = new AdapterDeploymentContext(configResolver);
            log.debug("Keycloak is using a per-request configuration resolver.");
        } else {
            InputStream configInputStream = getConfigInputStream(context);
            KeycloakDeployment kd;
            if (configInputStream == null) {
                log.warn("No adapter configuration.  Keycloak is unconfigured and will deny all requests.");
                kd = new KeycloakDeployment();
            } else {
                kd = KeycloakDeploymentBuilder.build(configInputStream);
            }
            deploymentContext = new AdapterDeploymentContext(kd);
            log.debug("Keycloak is using a per-deployment configuration.");

            // TODO: got this during a rebase, so, need to figure out what to do with it
            // one option is to require a generic keycloak.json, with no realm information
            // and use it for the node registration, but needs some testing
            nodesRegistrationLifecycle = new NodesRegistrationLifecycle(kd);
            nodesRegistrationLifecycle.start();
        }

        context.getServletContext().setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        AuthenticatedActionsValve actions = new AuthenticatedActionsValve(deploymentContext, getNext(), getContainer(), getController());
        setNext(actions);
    }

    protected void beforeStop() {
        nodesRegistrationLifecycle.stop();
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            if (log.isTraceEnabled()) {
                log.trace("invoke");
            }
            CatalinaHttpFacade facade = new CatalinaHttpFacade(request, response);
            Manager sessionManager = request.getContext().getManager();
            CatalinaUserSessionManagementWrapper sessionManagementWrapper = new CatalinaUserSessionManagementWrapper(userSessionManagement, sessionManager);
            PreAuthActionsHandler handler = new PreAuthActionsHandler(sessionManagementWrapper, deploymentContext, facade);
            if (handler.handleRequest()) {
                return;
            }
            checkKeycloakSession(request, facade);
            super.invoke(request, response);
        } finally {
        }
    }

    @Override
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("*** authenticate");
        }
        CatalinaHttpFacade facade = new CatalinaHttpFacade(request, response);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            log.info("*** deployment isn't configured return false");
            return false;
        }

        CatalinaRequestAuthenticator authenticator = new CatalinaRequestAuthenticator(deployment, this, userSessionManagement, facade, request);
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            if (facade.isEnded()) {
                return false;
            }
            return true;
        }
        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            challenge.challenge(facade);
        }
        return false;
    }

    /**
     * Checks that access token is still valid.  Will attempt refresh of token if it is not.
     *
     * @param request
     */
    protected void checkKeycloakSession(Request request, HttpFacade facade) {
        if (request.getSessionInternal(false) == null || request.getSessionInternal().getPrincipal() == null) return;
        RefreshableKeycloakSecurityContext session = (RefreshableKeycloakSecurityContext) request.getSessionInternal().getNote(KeycloakSecurityContext.class.getName());
        if (session == null) return;
        // just in case session got serialized
        if (session.getDeployment() == null) session.setDeployment(deploymentContext.resolveDeployment(facade));
        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        boolean success = session.refreshExpiredToken(false);
        if (success && session.isActive()) return;

        // Refresh failed, so user is already logged out from keycloak. Cleanup and expire our session
        Session catalinaSession = request.getSessionInternal();
        log.debugf("Cleanup and expire session %s after failed refresh", catalinaSession.getId());
        catalinaSession.removeNote(KeycloakSecurityContext.class.getName());
        request.setUserPrincipal(null);
        request.setAuthType(null);
        catalinaSession.setPrincipal(null);
        catalinaSession.setAuthType(null);
        catalinaSession.expire();
    }

    public void keycloakSaveRequest(Request request) throws IOException {
        saveRequest(request, request.getSessionInternal(true));
    }

    public boolean keycloakRestoreRequest(Request request) {
        try {
            return restoreRequest(request, request.getSessionInternal());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
