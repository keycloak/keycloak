package org.keycloak.adapters.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.keycloak.adapters.KeycloakConfigResolver;

/**
 * Keycloak authentication valve
 * 
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractKeycloakAuthenticatorValve extends FormAuthenticator implements LifecycleListener {

    public static final String TOKEN_STORE_NOTE = "TOKEN_STORE_NOTE";

	private final static Logger log = Logger.getLogger(""+AbstractKeycloakAuthenticatorValve.class);
	protected CatalinaUserSessionManagement userSessionManagement = new CatalinaUserSessionManagement();
    protected AdapterDeploymentContext deploymentContext;
    protected NodesRegistrationManagement nodesRegistrationManagement;

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.START_EVENT.equals(event.getType())) {
            cache = false;
        } else if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
        	keycloakInit();
        } else if (event.getType() == Lifecycle.BEFORE_STOP_EVENT) {
            beforeStop();
        }
    }

    protected void logoutInternal(Request request) {
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
        request.setUserPrincipal(null);
    }

    @SuppressWarnings("UseSpecificCatch")
    public void keycloakInit() {
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
                log.log(Level.INFO, "Using {0} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
            } catch (Exception ex) {
                log.log(Level.FINE, "The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", new Object[]{configResolverClass, ex.getMessage()});
                deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
            }
        } else {
            InputStream configInputStream = getConfigInputStream(context);
            KeycloakDeployment kd;
            if (configInputStream == null) {
                log.fine("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
                kd = new KeycloakDeployment();
            } else {
                kd = KeycloakDeploymentBuilder.build(configInputStream);
            }
            deploymentContext = new AdapterDeploymentContext(kd);
            log.fine("Keycloak is using a per-deployment configuration.");
        }

        context.getServletContext().setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        AuthenticatedActionsValve actions = new AuthenticatedActionsValve(deploymentContext, getNext(), getContainer());
        setNext(actions);

        nodesRegistrationManagement = new NodesRegistrationManagement();
    }

    protected void beforeStop() {
        nodesRegistrationManagement.stop();
    }

    private static InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        log.finest("**** using " + AdapterConstants.AUTH_DATA_PARAM_NAME);
        log.finest(json);
        return new ByteArrayInputStream(json.getBytes());
    }

    private static InputStream getConfigInputStream(Context context) {
        InputStream is = getJSONFromServletContext(context.getServletContext());
        if (is == null) {
            String path = context.getServletContext().getInitParameter("keycloak.config.file");
            if (path == null) {
                log.finest("**** using /WEB-INF/keycloak.json");
                is = context.getServletContext().getResourceAsStream("/WEB-INF/keycloak.json");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                	log.severe("NOT FOUND /WEB-INF/keycloak.json");
                    throw new RuntimeException(e);
                }
            }
        }
        return is;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
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

    protected abstract GenericPrincipalFactory createPrincipalFactory();

    protected boolean authenticateInternal(Request request, HttpServletResponse response) {
        CatalinaHttpFacade facade = new CatalinaHttpFacade(request, response);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            return false;
        }
        AdapterTokenStore tokenStore = getTokenStore(request, facade, deployment);

        nodesRegistrationManagement.tryRegister(deployment);

        CatalinaRequestAuthenticator authenticator = new CatalinaRequestAuthenticator(deployment, this, tokenStore, facade, request, createPrincipalFactory());
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
            store = new CatalinaSessionTokenStore(request, resolvedDeployment, userSessionManagement, createPrincipalFactory());
        } else {
            store = new CatalinaCookieTokenStore(request, facade, resolvedDeployment, createPrincipalFactory());
        }

        request.setNote(TOKEN_STORE_NOTE, store);
        return store;
    }

}
