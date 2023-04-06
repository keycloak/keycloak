package org.keycloak.adapters.authorization.integration.elytron;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jboss.logging.Logger;
import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.authorization.TokenPrincipal;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.wildfly.security.http.oidc.OidcClientConfiguration;
import org.wildfly.security.http.oidc.OidcPrincipal;
import org.wildfly.security.http.oidc.RefreshableOidcSecurityContext;

/**
 * A {@link Filter} acting as a policy enforcer. This filter does not enforce access for anonymous subjects.</p>
 *
 * For authenticated subjects, this filter delegates the access decision to the {@link PolicyEnforcer} and decide if
 * the request should continue.</p>
 *
 * If access is not granted, this filter aborts the request and relies on the {@link PolicyEnforcer} to properly
 * respond to client.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcerFilter implements Filter, ServletContextAttributeListener {

    private final Logger logger = Logger.getLogger(getClass());
    private final Map<PolicyEnforcerConfig, PolicyEnforcer> policyEnforcer;
    private final ConfigurationResolver configResolver;

    public PolicyEnforcerFilter(ConfigurationResolver configResolver) {
        this.configResolver = configResolver;
        this.policyEnforcer = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // no-init
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpSession session = request.getSession(false);

        if (session == null) {
            logger.debug("Anonymous request, continuing the filter chain");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        RefreshableOidcSecurityContext securityContext = (RefreshableOidcSecurityContext) ((OidcPrincipal) request.getUserPrincipal()).getOidcSecurityContext();
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String accessToken = securityContext.getTokenString();
        ServletHttpRequest httpRequest = new ServletHttpRequest(request, new TokenPrincipal() {
            @Override
            public String getRawToken() {
                return accessToken;
            }
        });

        PolicyEnforcer policyEnforcer = getOrCreatePolicyEnforcer(httpRequest, securityContext);
        AuthorizationContext authzContext = policyEnforcer.enforce(httpRequest, new ServletHttpResponse(response));

        request.setAttribute(AuthorizationContext.class.getName(), authzContext);

        if (authzContext.isGranted()) {
            logger.debug("Request authorized, continuing the filter chain");
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            logger.debugf("Unauthorized request to path [%s], aborting the filter chain", request.getRequestURI());
        }
    }

    private PolicyEnforcer getOrCreatePolicyEnforcer(HttpRequest request, RefreshableOidcSecurityContext securityContext) {
        return policyEnforcer.computeIfAbsent(configResolver.resolve(request), new Function<PolicyEnforcerConfig, PolicyEnforcer>() {
            @Override
            public PolicyEnforcer apply(PolicyEnforcerConfig enforcerConfig) {
                OidcClientConfiguration configuration = securityContext.getOidcClientConfiguration();
                String authServerUrl = configuration.getAuthServerBaseUrl();

                return PolicyEnforcer.builder()
                        .authServerUrl(authServerUrl)
                        .realm(configuration.getRealm())
                        .clientId(configuration.getClientId())
                        .credentials(configuration.getResourceCredentials())
                        .bearerOnly(false)
                        .enforcerConfig(enforcerConfig)
                        .httpClient(configuration.getClient()).build();
            }
        });
    }
}
