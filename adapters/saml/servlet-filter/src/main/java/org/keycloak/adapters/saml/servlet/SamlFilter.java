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

package org.keycloak.adapters.saml.servlet;

import org.keycloak.adapters.saml.DefaultSamlDeployment;
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
import org.keycloak.adapters.servlet.ServletHttpFacade;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.saml.common.exceptions.ParsingException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlFilter implements Filter {
    protected SamlDeploymentContext deploymentContext;
    protected SessionIdMapper idMapper;
    private final static Logger log = Logger.getLogger("" + SamlFilter.class);
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:");

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        deploymentContext = (SamlDeploymentContext)filterConfig.getServletContext().getAttribute(SamlDeploymentContext.class.getName());
        if (deploymentContext != null) {
            idMapper = (SessionIdMapper)filterConfig.getServletContext().getAttribute(SessionIdMapper.class.getName());
            return;
        }
        String configResolverClass = filterConfig.getInitParameter("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                SamlConfigResolver configResolver = (SamlConfigResolver) getClass().getClassLoader().loadClass(configResolverClass).newInstance();
                deploymentContext = new SamlDeploymentContext(configResolver);
                log.log(Level.INFO, "Using {0} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
            } catch (Exception ex) {
                log.log(Level.WARNING, "The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", new Object[] { configResolverClass, ex.getMessage() });
                deploymentContext = new SamlDeploymentContext(new DefaultSamlDeployment());
            }
        } else {
            String fp = filterConfig.getInitParameter("keycloak.config.file");
            InputStream is = null;
            if (fp != null) {
                try {
                    is = new FileInputStream(fp);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                String path = "/WEB-INF/keycloak-saml.xml";
                String pathParam = filterConfig.getInitParameter("keycloak.config.path");
                if (pathParam != null)
                    path = pathParam;
                is = filterConfig.getServletContext().getResourceAsStream(path);
            }
            final SamlDeployment deployment;
            if (is == null) {
                log.info("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
                deployment = new DefaultSamlDeployment();
            } else {
                try {
                    ResourceLoader loader = new ResourceLoader() {
                        @Override
                        public InputStream getResourceAsStream(String resource) {
                            return filterConfig.getServletContext().getResourceAsStream(resource);
                        }
                    };
                    deployment = new DeploymentBuilder().build(is, loader);
                } catch (ParsingException e) {
                    throw new RuntimeException(e);
                }
            }
            deploymentContext = new SamlDeploymentContext(deployment);
            log.fine("Keycloak is using a per-deployment configuration.");
        }
        idMapper = new InMemorySessionIdMapper();
        filterConfig.getServletContext().setAttribute(SamlDeploymentContext.class.getName(), deploymentContext);
        filterConfig.getServletContext().setAttribute(SessionIdMapper.class.getName(), idMapper);

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        ServletHttpFacade facade = new ServletHttpFacade(request, response);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            response.sendError(403);
            log.fine("deployment not configured");
            return;
        }
        FilterSamlSessionStore tokenStore = new FilterSamlSessionStore(request, facade, 100000, idMapper, deployment);
        boolean isEndpoint = request.getRequestURI().substring(request.getContextPath().length()).endsWith("/saml");
        SamlAuthenticator authenticator;
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
            log.fine("AUTHENTICATED");
            if (facade.isEnded()) {
                return;
            }
            HttpServletRequestWrapper wrapper = tokenStore.getWrap();
            chain.doFilter(wrapper, res);
            return;
        }
        if (outcome == AuthOutcome.LOGGED_OUT) {
            tokenStore.logoutAccount();
            String logoutPage = deployment.getLogoutPage();
            if (logoutPage != null) {
                if (PROTOCOL_PATTERN.matcher(logoutPage).find()) {
                    response.sendRedirect(logoutPage);
                    log.log(Level.FINE, "Redirected to logout page {0}", logoutPage);
                } else {
                    RequestDispatcher disp = req.getRequestDispatcher(logoutPage);
                    disp.forward(req, res);
                }
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            log.fine("challenge");
            challenge.challenge(facade);
            return;
        }

        if (deployment.isIsPassive() && outcome == AuthOutcome.NOT_AUTHENTICATED) {
            log.fine("PASSIVE_NOT_AUTHENTICATED");
            if (facade.isEnded()) {
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        if (!facade.isEnded()) {
            response.sendError(403);
        }

    }

    @Override
    public void destroy() {

    }
}
