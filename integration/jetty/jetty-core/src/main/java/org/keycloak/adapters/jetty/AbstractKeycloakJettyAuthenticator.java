package org.keycloak.adapters.jetty;

import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
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
import org.keycloak.constants.AdapterConstants;
import org.keycloak.enums.TokenStore;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractKeycloakJettyAuthenticator extends LoginAuthenticator {
    public static final String TOKEN_STORE_NOTE = "TOKEN_STORE_NOTE";
    protected static final org.jboss.logging.Logger log = Logger.getLogger(AbstractKeycloakJettyAuthenticator.class);
    protected AdapterDeploymentContext deploymentContext;
    protected NodesRegistrationManagement nodesRegistrationManagement;
    protected AdapterConfig adapterConfig;
    protected KeycloakConfigResolver configResolver;

    public AbstractKeycloakJettyAuthenticator() {
        super();
    }

    private static InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        return new ByteArrayInputStream(json.getBytes());
    }

    public static AdapterTokenStore getTokenStore(Request request, HttpFacade facade, KeycloakDeployment resolvedDeployment) {
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

    public static void logoutCurrent(Request request) {
        AdapterDeploymentContext deploymentContext = (AdapterDeploymentContext)request.getAttribute(AdapterDeploymentContext.class.getName());
        KeycloakSecurityContext ksc = (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
        if (ksc != null) {
            JettyHttpFacade facade = new JettyHttpFacade(request, null);
            KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
            if (ksc instanceof RefreshableKeycloakSecurityContext) {
                ((RefreshableKeycloakSecurityContext) ksc).logout(deployment);
            }

            AdapterTokenStore tokenStore = getTokenStore(request, facade, deployment);
            tokenStore.logout();
            request.removeAttribute(KeycloakSecurityContext.class.getName());
        }
    }

    public static UserIdentity createIdentity(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        Set<String> roles = AdapterUtils.getRolesFromSecurityContext(principal.getKeycloakSecurityContext());
        if (roles == null) {
            roles = new HashSet<String>();
        }
        Subject theSubject = new Subject();
        String[] theRoles = new String[roles.size()];
        roles.toArray(theRoles);

        return new DefaultUserIdentity(theSubject, principal, theRoles);
    }

    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        //super.setConfiguration(configuration);
        initializeKeycloak();
    }

    @Override
    public boolean secureResponse(ServletRequest req, ServletResponse res, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException
    {
        return true;
    }

    public AdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public void setAdapterConfig(AdapterConfig adapterConfig) {
        this.adapterConfig = adapterConfig;
    }

    public KeycloakConfigResolver getConfigResolver() {
        return configResolver;
    }

    public void setConfigResolver(KeycloakConfigResolver configResolver) {
        this.configResolver = configResolver;
    }

    @SuppressWarnings("UseSpecificCatch")
    public void initializeKeycloak() {
        nodesRegistrationManagement = new NodesRegistrationManagement();
        String contextPath = ContextHandler.getCurrentContext().getContextPath();
        ServletContext theServletContext = ContextHandler.getCurrentContext().getContext(contextPath);

        // Jetty 9.1.x servlet context will be null :(
        if (configResolver == null && theServletContext != null) {
            String configResolverClass = theServletContext.getInitParameter("keycloak.config.resolver");
            if (configResolverClass != null) {
                try {
                    configResolver = (KeycloakConfigResolver) ContextHandler.getCurrentContext().getClassLoader().loadClass(configResolverClass).newInstance();
                    log.infov("Using {0} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
                } catch (Exception ex) {
                    log.infov("The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", new Object[]{configResolverClass, ex.getMessage()});
                }
            }
        }

        if (configResolver != null) {
            deploymentContext = new AdapterDeploymentContext(configResolver);
        } else if (adapterConfig != null) {
            KeycloakDeployment kd = KeycloakDeploymentBuilder.build(adapterConfig);
            deploymentContext = new AdapterDeploymentContext(kd);
        } else if (theServletContext != null) {
            InputStream configInputStream = getConfigInputStream(theServletContext);
            if (configInputStream != null) {
                deploymentContext = new AdapterDeploymentContext(KeycloakDeploymentBuilder.build(configInputStream));
             }
        }
        if (deploymentContext == null) {
            deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
        }
        if (theServletContext != null) theServletContext.setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
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
        Request request = resolveRequest(req);
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
        if (!mandatory)
            return new DeferredAuthentication(this);
        AdapterTokenStore tokenStore = getTokenStore(request, facade, deployment);
        nodesRegistrationManagement.tryRegister(deployment);

        tokenStore.checkCurrentToken();
        AbstractJettyRequestAuthenticator authenticator = createRequestAuthenticator(request, facade, deployment, tokenStore);
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



    protected abstract Request resolveRequest(ServletRequest req);

    protected abstract AbstractJettyRequestAuthenticator createRequestAuthenticator(Request request, JettyHttpFacade facade, KeycloakDeployment deployment, AdapterTokenStore tokenStore);

    @Override
    public String getAuthMethod() {
        return "KEYCLOAK";
    }

    protected Authentication register(Request request, KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        request.setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        Authentication authentication = request.getAuthentication();
        if (!(authentication instanceof KeycloakAuthentication)) {
            UserIdentity userIdentity = createIdentity(principal);
            authentication = createAuthentication(userIdentity);
            request.setAuthentication(authentication);
        }
        return authentication;
    }

    protected abstract Authentication createAuthentication(UserIdentity userIdentity);

    public static abstract class KeycloakAuthentication extends UserAuthentication
    {
        public KeycloakAuthentication(String method, UserIdentity userIdentity) {
            super(method, userIdentity);
        }

    }
}
