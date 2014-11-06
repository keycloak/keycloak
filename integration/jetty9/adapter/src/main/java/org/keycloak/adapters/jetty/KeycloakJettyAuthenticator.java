package org.keycloak.adapters.jetty;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.MultiMap;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.enums.TokenStore;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakJettyAuthenticator extends FormAuthenticator {
    private static final org.jboss.logging.Logger log = Logger.getLogger(KeycloakJettyAuthenticator.class);
    protected AdapterDeploymentContext deploymentContext;
    protected NodesRegistrationManagement nodesRegistrationManagement;

    public KeycloakJettyAuthenticator() {
        super();
    }

    public KeycloakJettyAuthenticator(String login, String error, boolean dispatch) {
        super(login, error, dispatch);
    }

    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        super.setConfiguration(configuration);
        initializeKeycloak();
    }

    @SuppressWarnings("UseSpecificCatch")
    public void initializeKeycloak() {
        String contextPath = ContextHandler.getCurrentContext().getContextPath();
        ServletContext theServletContext = ContextHandler.getCurrentContext().getContext(contextPath);
        // Possible scenarios:
        // 1) The deployment has a keycloak.config.resolver specified and it exists:
        //    Outcome: adapter uses the resolver
        // 2) The deployment has a keycloak.config.resolver and isn't valid (doesn't exists, isn't a resolver, ...) :
        //    Outcome: adapter is left unconfigured
        // 3) The deployment doesn't have a keycloak.config.resolver , but has a keycloak.json (or equivalent)
        //    Outcome: adapter uses it
        // 4) The deployment doesn't have a keycloak.config.resolver nor keycloak.json (or equivalent)
        //    Outcome: adapter is left unconfigured

        String configResolverClass = theServletContext.getInitParameter("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                KeycloakConfigResolver configResolver = (KeycloakConfigResolver) ContextHandler.getCurrentContext().getClassLoader().loadClass(configResolverClass).newInstance();
                deploymentContext = new AdapterDeploymentContext(configResolver);
                log.infov("Using {0} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
            } catch (Exception ex) {
                log.infov("The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", new Object[]{configResolverClass, ex.getMessage()});
                deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
            }
        } else {
            InputStream configInputStream = getConfigInputStream(theServletContext);
            KeycloakDeployment kd;
            if (configInputStream == null) {
                log.debug("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
                kd = new KeycloakDeployment();
            } else {
                kd = KeycloakDeploymentBuilder.build(configInputStream);
            }
            deploymentContext = new AdapterDeploymentContext(kd);
            log.debug("Keycloak is using a per-deployment configuration.");
        }

        theServletContext.setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        //AuthenticatedActionsValve actions = new AuthenticatedActionsValve(deploymentContext, getNext(), getContainer());
        //setNext(actions);

        nodesRegistrationManagement = new NodesRegistrationManagement();
    }

    private static InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        return new ByteArrayInputStream(json.getBytes());
    }


    private InputStream getConfigInputStream(ServletContext servletContext) {
        InputStream is = getJSONFromServletContext(servletContext);
        if (is == null) {
            String path = servletContext.getInitParameter("keycloak.config.file");
            if (path == null) {
                is = servletContext.getResourceAsStream("/WEB-INF/keycloak.json");
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

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException {
        if (log.isTraceEnabled()) {
            log.trace("*** authenticate");
        }
        Request request = HttpChannel.getCurrentHttpChannel().getRequest();
        JettyHttpFacade facade = new JettyHttpFacade(request, (HttpServletResponse)res);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            log.debug("*** deployment isn't configured return false");
            return Authentication.UNAUTHENTICATED;
        }
        PreAuthActionsHandler handler = new PreAuthActionsHandler(new JettyUserSessionManagement(request.getSessionManager()), deploymentContext, facade);
        if (handler.handleRequest()) {
            return Authentication.SEND_SUCCESS;
        }
        AdapterTokenStore tokenStore = getTokenStore(request, facade, deployment);

        nodesRegistrationManagement.tryRegister(deployment);

        JettyRequestAuthenticator authenticator = new JettyRequestAuthenticator(deployment, this, tokenStore, facade, request);
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            if (facade.isEnded()) {
                return Authentication.SEND_SUCCESS;
            }

            Authentication authentication = register(request, authenticator.principal);
            AuthenticatedActionsHandler authenticatedActionsHandler = new AuthenticatedActionsHandler(deployment, facade);
            if (authenticatedActionsHandler.handledRequest()) {
                return Authentication.SEND_SUCCESS;
            }
            return authentication;

        }
        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            challenge.challenge(facade);
        }
        return Authentication.SEND_CONTINUE;
    }

    @Override
    public String getAuthMethod() {
        return "KEYCLOAK";
    }

    public static final String TOKEN_STORE_NOTE = "TOKEN_STORE_NOTE";
    protected AdapterTokenStore getTokenStore(Request request, HttpFacade facade, KeycloakDeployment resolvedDeployment) {
        AdapterTokenStore store = (AdapterTokenStore)request.getAttribute(TOKEN_STORE_NOTE);
        if (store != null) {
            return store;
        }

        if (resolvedDeployment.getTokenStore() == TokenStore.SESSION) {
            store = new JettySessionTokenStore(request, resolvedDeployment);
        } else {
            store = new JettyCookieTokenStore(request, facade, resolvedDeployment);
        }

        request.setAttribute(TOKEN_STORE_NOTE, store);
        return store;
    }

    protected Authentication register(HttpServletRequest httpServletRequest, KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        Set<String> roles = AdapterUtils.getRolesFromSecurityContext(principal.getKeycloakSecurityContext());
        if (roles == null) {
            roles = new HashSet<String>();
        }
        Request request = (Request) httpServletRequest;
        Authentication authentication = request.getAuthentication();
        if (!(authentication instanceof UserAuthentication)) {
            Subject theSubject = new Subject();
            String[] theRoles = new String[roles.size()];
            roles.toArray(theRoles);

            UserIdentity userIdentity = new DefaultUserIdentity(theSubject, principal, theRoles);
            authentication = new UserAuthentication(getAuthMethod(), userIdentity);
            request.setAuthentication(authentication);
        }
        return authentication;
    }


}
