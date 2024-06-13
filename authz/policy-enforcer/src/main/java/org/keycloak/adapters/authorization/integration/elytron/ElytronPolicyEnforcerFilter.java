package org.keycloak.adapters.authorization.integration.elytron;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.authorization.integration.jakarta.ServletPolicyEnforcerFilter;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.wildfly.security.http.oidc.OidcClientConfiguration;
import org.wildfly.security.http.oidc.OidcPrincipal;
import org.wildfly.security.http.oidc.RefreshableOidcSecurityContext;

public class ElytronPolicyEnforcerFilter extends ServletPolicyEnforcerFilter {

    public ElytronPolicyEnforcerFilter(ConfigurationResolver configResolver) {
        super(configResolver);
    }

    @Override
    protected String extractBearerToken(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();

        if (principal == null) {
            return null;
        }

        OidcPrincipal oidcPrincipal = (OidcPrincipal) principal;
        RefreshableOidcSecurityContext securityContext = (RefreshableOidcSecurityContext) oidcPrincipal.getOidcSecurityContext();

        if (securityContext == null) {
            return null;
        }

        return securityContext.getTokenString();
    }

    @Override
    protected PolicyEnforcer createPolicyEnforcer(HttpServletRequest servletRequest, PolicyEnforcerConfig enforcerConfig) {
        RefreshableOidcSecurityContext securityContext = (RefreshableOidcSecurityContext) ((OidcPrincipal) servletRequest.getUserPrincipal()).getOidcSecurityContext();
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
}
