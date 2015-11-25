package org.keycloak.adapters.saml.servlet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import org.keycloak.adapters.saml.DefaultSamlDeployment;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;
import org.keycloak.adapters.servlet.ServletHttpFacade;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.saml.common.exceptions.ParsingException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlFilter implements Filter {
    protected SamlDeploymentContext deploymentContext;
    protected SessionIdMapper idMapper = new InMemorySessionIdMapper();
    private final static Logger log = Logger.getLogger("" + SamlFilter.class);

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        String configResolverClass = filterConfig.getInitParameter("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                throw new RuntimeException("Not implemented yet");
                // KeycloakConfigResolver configResolver = (KeycloakConfigResolver)
                // context.getLoader().getClassLoader().loadClass(configResolverClass).newInstance();
                // deploymentContext = new SamlDeploymentContext(configResolver);
                // log.log(Level.INFO, "Using {0} to resolve Keycloak configuration on a per-request basis.",
                // configResolverClass);
            } catch (Exception ex) {
                log.log(Level.FINE, "The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", new Object[] { configResolverClass, ex.getMessage() });
                // deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
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
        filterConfig.getServletContext().setAttribute(SamlDeploymentContext.class.getName(), deploymentContext);

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
        FilterSamlSessionStore tokenStore = new FilterSamlSessionStore(request, facade, 100000, idMapper);

        SamlAuthenticator authenticator = new SamlAuthenticator(facade, deployment, tokenStore) {
            @Override
            protected void completeAuthentication(SamlSession account) {

            }
        };
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
            if (deployment.getLogoutPage() != null) {
                RequestDispatcher disp = req.getRequestDispatcher(deployment.getLogoutPage());
                disp.forward(req, res);
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
