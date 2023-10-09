package org.keycloak.adapters.authorization.integration.jakarta;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.authorization.TokenPrincipal;
import org.keycloak.adapters.authorization.integration.elytron.ServletHttpRequest;
import org.keycloak.adapters.authorization.integration.elytron.ServletHttpResponse;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import java.io.IOException;
import java.util.Enumeration;

/**
 * A Jakarta Servlet {@link Filter} acting as a policy enforcer. This filter does not enforce access for anonymous subjects.</p>
 *
 * For authenticated subjects, this filter delegates the access decision to the {@link PolicyEnforcer} and decide if
 * the request should continue.</p>
 *
 * If access is not granted, this filter aborts the request and relies on the {@link PolicyEnforcer} to properly
 * respond to client.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ServletPolicyEnforcerFilter implements Filter, ServletContextAttributeListener {

    private final Logger logger = Logger.getLogger(getClass());
    private final PolicyEnforcer policyEnforcer;

    public ServletPolicyEnforcerFilter(ConfigurationResolver configResolver) {
        PolicyEnforcerConfig policyEnforcerConfig = configResolver.resolve(null);
        this.policyEnforcer = this.createPolicyEnforcer(null, policyEnforcerConfig);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // no-init
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        ServletHttpRequest httpRequest = new ServletHttpRequest(request, new TokenPrincipal() {
            @Override
            public String getRawToken() {
                return extractBearerToken(request);
            }
        });

        AuthorizationContext authzContext = policyEnforcer.enforce(httpRequest, new ServletHttpResponse(response));

        request.setAttribute(AuthorizationContext.class.getName(), authzContext);

        if (authzContext.isGranted()) {
            logger.debug("Request authorized, continuing the filter chain");
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            logger.debugf("Unauthorized request to path [%s], aborting the filter chain", request.getRequestURI());
        }
    }

    protected String extractBearerToken(HttpServletRequest request) {
        Enumeration<String> authorizationHeaderValues = request.getHeaders("Authorization");

        while (authorizationHeaderValues.hasMoreElements()) {
            String value = authorizationHeaderValues.nextElement();
            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                continue;
            }

            String bearer = parts[0];

            if (bearer.equalsIgnoreCase("Bearer")) {
                return parts[1];
            }
        }

        return null;
    }

    protected PolicyEnforcer createPolicyEnforcer(HttpServletRequest servletRequest, PolicyEnforcerConfig enforcerConfig) {
        String authServerUrl = enforcerConfig.getAuthServerUrl();

        return PolicyEnforcer.builder()
                .authServerUrl(authServerUrl)
                .realm(enforcerConfig.getRealm())
                .clientId(enforcerConfig.getResource())
                .credentials(enforcerConfig.getCredentials())
                .bearerOnly(false)
                .enforcerConfig(enforcerConfig).build();
    }
}
