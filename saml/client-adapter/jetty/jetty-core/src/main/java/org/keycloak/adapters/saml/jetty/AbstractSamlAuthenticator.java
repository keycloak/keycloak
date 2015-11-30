package org.keycloak.adapters.saml.jetty;

import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.URIUtil;
import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.jetty.spi.JettyHttpFacade;
import org.keycloak.adapters.jetty.spi.JettyUserSessionManagement;
import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlConfigResolver;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;
import org.keycloak.saml.common.exceptions.ParsingException;

import javax.security.auth.Subject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractSamlAuthenticator extends LoginAuthenticator {
    public static final String TOKEN_STORE_NOTE = "TOKEN_STORE_NOTE";
    protected static final Logger log = Logger.getLogger(AbstractSamlAuthenticator.class);
    protected SamlDeploymentContext deploymentContext;
    protected SamlConfigResolver configResolver;
    protected String errorPage;
    protected SessionIdMapper idMapper = new InMemorySessionIdMapper();

    public AbstractSamlAuthenticator() {
        super();
    }

    private static InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        return new ByteArrayInputStream(json.getBytes());
    }

    public JettySamlSessionStore getTokenStore(Request request, HttpFacade facade, SamlDeployment resolvedDeployment) {
        JettySamlSessionStore store = (JettySamlSessionStore) request.getAttribute(TOKEN_STORE_NOTE);
        if (store != null) {
            return store;
        }
        store = new JettySamlSessionStore(request, createSessionTokenStore(request, resolvedDeployment), facade, idMapper, new JettyUserSessionManagement(request.getSessionManager()));

        request.setAttribute(TOKEN_STORE_NOTE, store);
        return store;
    }

    public abstract AdapterSessionStore createSessionTokenStore(Request request, SamlDeployment resolvedDeployment);

    public void logoutCurrent(Request request) {
        JettyHttpFacade facade = new JettyHttpFacade(request, null);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        JettySamlSessionStore tokenStore = getTokenStore(request, facade, deployment);
        tokenStore.logoutAccount();
    }

    protected void forwardToLogoutPage(Request request, HttpServletResponse response, SamlDeployment deployment) {
        RequestDispatcher disp = request.getRequestDispatcher(deployment.getLogoutPage());
        //make sure the login page is never cached
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");


        try {
            disp.forward(request, response);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }



    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        //super.setConfiguration(configuration);
        initializeKeycloak();
        String error = configuration.getInitParameter(FormAuthenticator.__FORM_ERROR_PAGE);
        setErrorPage(error);
    }

    private void setErrorPage(String path) {
        if (path == null || path.trim().length() == 0) {
        } else {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            errorPage = path;

            if (errorPage.indexOf('?') > 0)
                errorPage = errorPage.substring(0, errorPage.indexOf('?'));
        }
    }

    @Override
    public boolean secureResponse(ServletRequest req, ServletResponse res, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        return true;
    }



    public SamlConfigResolver getConfigResolver() {
        return configResolver;
    }

    public void setConfigResolver(SamlConfigResolver configResolver) {
        this.configResolver = configResolver;
    }

    @SuppressWarnings("UseSpecificCatch")
    public void initializeKeycloak() {

        ServletContext theServletContext = null;
        ContextHandler.Context currentContext = ContextHandler.getCurrentContext();
        if (currentContext != null) {
            String contextPath = currentContext.getContextPath();

            if ("".equals(contextPath)) {
                // This could be the case in osgi environment when deploying apps through pax whiteboard extension.
                theServletContext = currentContext;
            } else {
                theServletContext = currentContext.getContext(contextPath);
            }
        }

        // Jetty 9.1.x servlet context will be null :(
        if (configResolver == null && theServletContext != null) {
            String configResolverClass = theServletContext.getInitParameter("keycloak.config.resolver");
            if (configResolverClass != null) {
                try {
                    configResolver = (SamlConfigResolver) ContextHandler.getCurrentContext().getClassLoader().loadClass(configResolverClass).newInstance();
                    log.infov("Using {0} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
                } catch (Exception ex) {
                    log.infov("The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", new Object[]{configResolverClass, ex.getMessage()});
                }
            }
        }

        if (configResolver != null) {
            //deploymentContext = new AdapterDeploymentContext(configResolver);
        } else if (theServletContext != null) {
            InputStream configInputStream = getConfigInputStream(theServletContext);
            if (configInputStream != null) {
                final ServletContext servletContext = theServletContext;
                SamlDeployment deployment = null;
                try {
                    deployment = new DeploymentBuilder().build(configInputStream, new ResourceLoader() {
                        @Override
                        public InputStream getResourceAsStream(String resource) {
                            return servletContext.getResourceAsStream(resource);
                        }
                    });
                } catch (ParsingException e) {
                    throw new RuntimeException(e);
                }
                deploymentContext = new SamlDeploymentContext(deployment);
            }
        }
        if (theServletContext != null)
            theServletContext.setAttribute(SamlDeploymentContext.class.getName(), deploymentContext);
    }

    private InputStream getConfigInputStream(ServletContext servletContext) {
        InputStream is = getJSONFromServletContext(servletContext);
        if (is == null) {
            String path = servletContext.getInitParameter("keycloak.config.file");
            if (path == null) {
                is = servletContext.getResourceAsStream("/WEB-INF/keycloak-saml.xml");
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
        JettyHttpFacade facade = new JettyHttpFacade(request, (HttpServletResponse) res);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            log.debug("*** deployment isn't configured return false");
            return Authentication.UNAUTHENTICATED;
        }
        if (!mandatory)
            return new DeferredAuthentication(this);
        JettySamlSessionStore tokenStore = getTokenStore(request, facade, deployment);

        SamlAuthenticator authenticator = new SamlAuthenticator(facade, deployment, tokenStore ) {
            @Override
            protected void completeAuthentication(SamlSession account) {

            }
        };
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            if (facade.isEnded()) {
                return Authentication.SEND_SUCCESS;
            }
            SamlSession samlSession = tokenStore.getAccount();
            Authentication authentication = register(request, samlSession);
            return authentication;

        }
        if (outcome == AuthOutcome.LOGGED_OUT) {
            logoutCurrent(request);
            if (deployment.getLogoutPage() != null) {
                forwardToLogoutPage(request, (HttpServletResponse)res, deployment);

            }
            return Authentication.SEND_CONTINUE;
        }

        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            challenge.challenge(facade);
        }
        return Authentication.SEND_CONTINUE;
    }


    protected abstract Request resolveRequest(ServletRequest req);

    @Override
    public String getAuthMethod() {
        return "KEYCLOAK-SAML";
    }

    public static UserIdentity createIdentity(SamlSession samlSession) {
        Set<String> roles = samlSession.getRoles();
        if (roles == null) {
            roles = new HashSet<String>();
        }
        Subject theSubject = new Subject();
        String[] theRoles = new String[roles.size()];
        roles.toArray(theRoles);

        return new DefaultUserIdentity(theSubject, samlSession.getPrincipal(), theRoles);
    }
    public Authentication register(Request request, SamlSession samlSession) {
        Authentication authentication = request.getAuthentication();
        if (!(authentication instanceof KeycloakAuthentication)) {
            UserIdentity userIdentity = createIdentity(samlSession);
            authentication = createAuthentication(userIdentity);
            request.setAuthentication(authentication);
        }
        return authentication;
    }

    public abstract Authentication createAuthentication(UserIdentity userIdentity);

    public static abstract class KeycloakAuthentication extends UserAuthentication {
        public KeycloakAuthentication(String method, UserIdentity userIdentity) {
            super(method, userIdentity);
        }

    }
}
