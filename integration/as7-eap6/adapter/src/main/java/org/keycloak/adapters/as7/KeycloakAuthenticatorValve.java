package org.keycloak.adapters.as7;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.enums.TokenStore;

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

    public static final String TOKEN_STORE_NOTE = "TOKEN_STORE_NOTE";

    private static final Logger log = Logger.getLogger(KeycloakAuthenticatorValve.class);
    protected CatalinaUserSessionManagement userSessionManagement = new CatalinaUserSessionManagement();
    protected AdapterDeploymentContext deploymentContext;
    protected NodesRegistrationManagement nodesRegistrationManagement;


    @Override
    public void start() throws LifecycleException {
        super.start();
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
        cache = false;
    }

    @Override
    public void logout(Request request) throws ServletException {
        KeycloakSecurityContext ksc = (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
        if (ksc != null) {
            CatalinaHttpFacade facade = new CatalinaHttpFacade(request, null);
            KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
            if (ksc instanceof RefreshableKeycloakSecurityContext) {
                ((RefreshableKeycloakSecurityContext) ksc).logout(deployment);
            }

            AdapterTokenStore tokenStore = getTokenStore(request, facade, deployment);
            tokenStore.logout();
            request.removeAttribute(KeycloakSecurityContext.class.getName());
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
        log.debug("**** using " + AdapterConstants.AUTH_DATA_PARAM_NAME);
        log.debug(json);
        return new ByteArrayInputStream(json.getBytes());
    }

    private static InputStream getConfigInputStream(Context context) {
        InputStream is = getJSONFromServletContext(context.getServletContext());
        if (is == null) {
            String path = context.getServletContext().getInitParameter("keycloak.config.file");
            if (path == null) {
                log.debug("**** using /WEB-INF/keycloak.json");
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


    @SuppressWarnings("UseSpecificCatch")
    protected void init() {
        // Possible scenarios:
        // 1) The deployment has a keycloak.config.resolver specified and it exists:
        //    Outcome: adapter uses the resolver
        // 2) The deployment has a keycloak.config.resolver and isn't valid (doesn't exists, isn't a resolver, ...) :
        //    Outcome: adapter is left unconfigured
        // 3) The deployment doesn't have a keycloak.config.resolver , but has a keycloak.json (or equivalent)
        //    Outcome: adapter uses it
        // 4) The deployment doesn't have a keycloak.config.resolver nor keycloak.json (or equivalent)
        //    Outcome: adapter is left unconfigured

        String configResolverClass = context.getServletContext().getInitParameter("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                KeycloakConfigResolver configResolver = (KeycloakConfigResolver) context.getLoader().getClassLoader().loadClass(configResolverClass).newInstance();
                deploymentContext = new AdapterDeploymentContext(configResolver);
                log.info("Using " + configResolverClass + " to resolve Keycloak configuration on a per-request basis.");
            } catch (Exception ex) {
                log.warn("The specified resolver " + configResolverClass + " could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: " + ex.getMessage());
                deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
            }
        } else {
            InputStream configInputStream = getConfigInputStream(context);
            KeycloakDeployment kd;
            if (configInputStream == null) {
                log.warn("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
                kd = new KeycloakDeployment();
            } else {
                kd = KeycloakDeploymentBuilder.build(configInputStream);
            }
            deploymentContext = new AdapterDeploymentContext(kd);
            log.debug("Keycloak is using a per-deployment configuration.");
        }

        context.getServletContext().setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        AuthenticatedActionsValve actions = new AuthenticatedActionsValve(deploymentContext, getNext(), getContainer(), getController());
        setNext(actions);

        nodesRegistrationManagement = new NodesRegistrationManagement();
    }

    protected void beforeStop() {
        nodesRegistrationManagement.stop();
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
            log.debug("*** deployment isn't configured return false");
            return false;
        }
        AdapterTokenStore tokenStore = getTokenStore(request, facade, deployment);

        nodesRegistrationManagement.tryRegister(deployment);

        CatalinaRequestAuthenticator authenticator = new CatalinaRequestAuthenticator(deployment, this, tokenStore, facade, request);
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
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        AdapterTokenStore tokenStore = getTokenStore(request, facade, deployment);
        tokenStore.checkCurrentToken();
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

    protected AdapterTokenStore getTokenStore(Request request, HttpFacade facade, KeycloakDeployment resolvedDeployment) {
        AdapterTokenStore store = (AdapterTokenStore)request.getNote(TOKEN_STORE_NOTE);
        if (store != null) {
            return store;
        }

        if (resolvedDeployment.getTokenStore() == TokenStore.SESSION) {
            store = new CatalinaSessionTokenStore(request, resolvedDeployment, userSessionManagement);
        } else {
            store = new CatalinaCookieTokenStore(request, facade, resolvedDeployment);
        }

        request.setNote(TOKEN_STORE_NOTE, store);
        return store;
    }

}
