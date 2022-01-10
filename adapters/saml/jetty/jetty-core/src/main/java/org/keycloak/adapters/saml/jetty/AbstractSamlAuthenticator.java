/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.saml.jetty;

import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.jboss.logging.Logger;
import org.keycloak.adapters.jetty.spi.JettyHttpFacade;
import org.keycloak.adapters.jetty.spi.JettyUserSessionManagement;
import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlConfigResolver;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;
import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.webbrowsersso.BrowserHandler;
import org.keycloak.adapters.saml.profile.webbrowsersso.SamlEndpoint;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
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
import java.util.regex.Pattern;

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
        store = createJettySamlSessionStore(request, facade, resolvedDeployment);

        request.setAttribute(TOKEN_STORE_NOTE, store);
        return store;
    }

    protected JettySamlSessionStore createJettySamlSessionStore(Request request, HttpFacade facade, SamlDeployment resolvedDeployment) {
        JettySamlSessionStore store;
        store = new JettySamlSessionStore(request, createSessionTokenStore(request, resolvedDeployment), facade, idMapper, createSessionManagement(request), resolvedDeployment);
        return store;
    }

    public abstract AdapterSessionStore createSessionTokenStore(Request request, SamlDeployment resolvedDeployment);

    public abstract JettyUserSessionManagement createSessionManagement(Request request);

    public void logoutCurrent(Request request) {
        JettyHttpFacade facade = new JettyHttpFacade(request, null);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        JettySamlSessionStore tokenStore = getTokenStore(request, facade, deployment);
        tokenStore.logoutAccount();
    }

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:");

    protected void forwardToLogoutPage(Request request, HttpServletResponse response, SamlDeployment deployment) {
        final String location = deployment.getLogoutPage();

        try {
            //make sure the login page is never cached
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            if (location == null) {
                log.warn("Logout page not set.");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else if (PROTOCOL_PATTERN.matcher(location).find()) {
                response.sendRedirect(response.encodeRedirectURL(location));
            } else {
                RequestDispatcher disp = request.getRequestDispatcher(location);

                disp.forward(request, response);
            }
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static class DummyLoginService implements LoginService {
        @Override
        public String getName() {
            return null;
        }

        @Override
        public UserIdentity login(String username, Object credentials) {
            return null;
        }

        @Override
        public boolean validate(UserIdentity user) {
            return false;
        }

        @Override
        public IdentityService getIdentityService() {
            return null;
        }

        @Override
        public void setIdentityService(IdentityService service) {

        }

        @Override
        public void logout(UserIdentity user) {

        }
    }



    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        //super.setConfiguration(configuration);
        initializeKeycloak();
        // need this so that getUserPrincipal does not throw NPE
        _loginService = new DummyLoginService();
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
        boolean isEndpoint = request.getRequestURI().substring(request.getContextPath().length()).endsWith("/saml");
        if (!mandatory && !isEndpoint)
            return new DeferredAuthentication(this);
        JettySamlSessionStore tokenStore = getTokenStore(request, facade, deployment);

        SamlAuthenticator authenticator = null;
        if (isEndpoint) {
            authenticator = new SamlAuthenticator(facade, deployment, tokenStore) {
                @Override
                protected void completeAuthentication(SamlSession account) {

                }

                @Override
                protected SamlAuthenticationHandler createBrowserHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
                    return new SamlEndpoint(facade, deployment, sessionStore);
                }
            };

        } else {
            authenticator = new SamlAuthenticator(facade, deployment, tokenStore) {
                @Override
                protected void completeAuthentication(SamlSession account) {

                }

                @Override
                protected SamlAuthenticationHandler createBrowserHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
                    return new BrowserHandler(facade, deployment, sessionStore);
                }
            };
        }
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
            authentication = createAuthentication(userIdentity, request);
            request.setAuthentication(authentication);
        }
        return authentication;
    }

    public abstract Authentication createAuthentication(UserIdentity userIdentity, Request request);

    public static abstract class KeycloakAuthentication extends UserAuthentication {
        public KeycloakAuthentication(String method, UserIdentity userIdentity) {
            super(method, userIdentity);
        }

    }
}
