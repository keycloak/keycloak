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

package org.keycloak.adapters;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.keycloak.adapters.authentication.ClientCredentialsProvider;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.rotation.PublicKeyLocator;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.common.enums.RelativeUrlsUsed;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.enums.TokenStore;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdapterDeploymentContext {
    private static final Logger log = Logger.getLogger(AdapterDeploymentContext.class);
    protected KeycloakDeployment deployment;
    protected KeycloakConfigResolver configResolver;

    public AdapterDeploymentContext() {
    }

    /**
     * For single-tenant deployments, this constructor is to be used, as a
     * full KeycloakDeployment is known at deployment time and won't change
     * during the application deployment's life cycle.
     *
     * @param deployment A KeycloakConfigResolver, possibly missing the Auth
     *                   Server URL
     */
    public AdapterDeploymentContext(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    /**
     * For multi-tenant deployments, this constructor is to be used, as a
     * KeycloakDeployment is not known at deployment time. It defers the
     * resolution of a KeycloakDeployment to a KeycloakConfigResolver,
     * to be implemented by the target application.
     *
     * @param configResolver A KeycloakConfigResolver that will be used
     *                       to resolve a KeycloakDeployment
     */
    public AdapterDeploymentContext(KeycloakConfigResolver configResolver) {
        this.configResolver = configResolver;
    }

    /**
     * For single-tenant deployments, it complements KeycloakDeployment
     * by resolving a relative Auth Server's URL based on the current request
     *
     * For multi-tenant deployments, defers the resolution of KeycloakDeployment
     * to the KeycloakConfigResolver .
     *
     * @param facade the Request/Response Façade , used to either determine
     *               the Auth Server URL (single tenant) or pass thru to the
     *               KeycloakConfigResolver.
     * @return
     */
    public KeycloakDeployment resolveDeployment(HttpFacade facade) {
        if (null != configResolver) {
            return configResolver.resolve(facade.getRequest());
        }

        if (deployment == null) return null;
        if (deployment.getAuthServerBaseUrl() == null) return deployment;

        KeycloakDeployment resolvedDeployment = resolveUrls(deployment, facade);
        if (resolvedDeployment.getPublicKeyLocator() == null) {
            throw new RuntimeException("KeycloakDeployment was never initialized through appropriate SPIs");
        }
        return resolvedDeployment;
    }

    protected KeycloakDeployment resolveUrls(KeycloakDeployment deployment, HttpFacade facade) {
        if (deployment.relativeUrls == RelativeUrlsUsed.NEVER) {
            // Absolute URI are already set to everything
            return deployment;
        } else {
            DeploymentDelegate delegate = new DeploymentDelegate(this.deployment);
            delegate.setAuthServerBaseUrl(getBaseBuilder(facade, this.deployment.getAuthServerBaseUrl()).build().toString());
            return delegate;
        }
    }

    /**
     * This delegate is used to store temporary, per-request metadata like request resolved URLs.
     * Ever method is delegated except URL get methods and isConfigured()
     *
     */
    protected static class DeploymentDelegate extends KeycloakDeployment {
        protected KeycloakDeployment delegate;

        public DeploymentDelegate(KeycloakDeployment delegate) {
            this.delegate = delegate;
        }

        public void setAuthServerBaseUrl(String authServerBaseUrl) {
            this.authServerBaseUrl = authServerBaseUrl;
            KeycloakUriBuilder serverBuilder = KeycloakUriBuilder.fromUri(authServerBaseUrl);
            resolveUrls(serverBuilder);
        }

        @Override
        public RelativeUrlsUsed getRelativeUrls() {
            return delegate.getRelativeUrls();
        }

        @Override
        public String getRealmInfoUrl() {
            return (this.realmInfoUrl != null) ? this.realmInfoUrl : delegate.getRealmInfoUrl();
        }

        @Override
        public String getTokenUrl() {
            return (this.tokenUrl != null) ? this.tokenUrl : delegate.getTokenUrl();
        }

        @Override
        public KeycloakUriBuilder getLogoutUrl() {
            return (this.logoutUrl != null) ? this.logoutUrl : delegate.getLogoutUrl();
        }

        @Override
        public String getAccountUrl() {
            return (this.accountUrl != null) ? this.accountUrl : delegate.getAccountUrl();
        }

        @Override
        public String getRegisterNodeUrl() {
            return (this.registerNodeUrl != null) ? this.registerNodeUrl : delegate.getRegisterNodeUrl();
        }

        @Override
        public String getUnregisterNodeUrl() {
            return (this.unregisterNodeUrl != null) ? this.unregisterNodeUrl : delegate.getUnregisterNodeUrl();
        }

        @Override
        public String getJwksUrl() {
            return (this.jwksUrl != null) ? this.jwksUrl : delegate.getJwksUrl();
        }

        @Override
        public String getResourceName() {
            return delegate.getResourceName();
        }

        @Override
        public String getRealm() {
            return delegate.getRealm();
        }

        @Override
        public void setRealm(String realm) {
            delegate.setRealm(realm);
        }

        @Override
        public void setPublicKeyLocator(PublicKeyLocator publicKeyLocator) {
            delegate.setPublicKeyLocator(publicKeyLocator);
        }

        @Override
        public PublicKeyLocator getPublicKeyLocator() {
            return delegate.getPublicKeyLocator();
        }

        @Override
        public void setResourceName(String resourceName) {
            delegate.setResourceName(resourceName);
        }

        @Override
        public boolean isBearerOnly() {
            return delegate.isBearerOnly();
        }

        @Override
        public void setBearerOnly(boolean bearerOnly) {
            delegate.setBearerOnly(bearerOnly);
        }
        
        @Override
        public boolean isAutodetectBearerOnly() {
            return delegate.isAutodetectBearerOnly();
        }
        
        @Override
        public void setAutodetectBearerOnly(boolean autodetectBearerOnly) {
            delegate.setAutodetectBearerOnly(autodetectBearerOnly);
        }

        @Override
        public boolean isEnableBasicAuth() {
            return delegate.isEnableBasicAuth();
        }

        @Override
        public void setEnableBasicAuth(boolean enableBasicAuth) {
            delegate.setEnableBasicAuth(enableBasicAuth);
        }

        @Override
        public boolean isPublicClient() {
            return delegate.isPublicClient();
        }

        @Override
        public void setPublicClient(boolean publicClient) {
            delegate.setPublicClient(publicClient);
        }

        @Override
        public Map<String, Object> getResourceCredentials() {
            return delegate.getResourceCredentials();
        }

        @Override
        public void setResourceCredentials(Map<String, Object> resourceCredentials) {
            delegate.setResourceCredentials(resourceCredentials);
        }

        @Override
        public void setClientAuthenticator(ClientCredentialsProvider clientAuthenticator) {
            delegate.setClientAuthenticator(clientAuthenticator);
        }

        @Override
        public ClientCredentialsProvider getClientAuthenticator() {
            return delegate.getClientAuthenticator();
        }

        @Override
        public HttpClient getClient() {
            return delegate.getClient();
        }

        @Override
        public void setClient(HttpClient client) {
            delegate.setClient(client);
        }

        @Override
        public String getScope() {
            return delegate.getScope();
        }

        @Override
        public void setScope(String scope) {
            delegate.setScope(scope);
        }

        @Override
        public SslRequired getSslRequired() {
            return delegate.getSslRequired();
        }

        @Override
        public void setSslRequired(SslRequired sslRequired) {
            delegate.setSslRequired(sslRequired);
        }

        @Override
        public int getConfidentialPort() {
            return delegate.getConfidentialPort();
        }

        @Override
        public void setConfidentialPort(int confidentialPort) {
            delegate.setConfidentialPort(confidentialPort);
        }

        @Override
        public TokenStore getTokenStore() {
            return delegate.getTokenStore();
        }

        @Override
        public void setTokenStore(TokenStore tokenStore) {
            delegate.setTokenStore(tokenStore);
        }

        @Override
        public String getAdapterStateCookiePath() {
            return delegate.getAdapterStateCookiePath();
        }

        @Override
        public void setAdapterStateCookiePath(String adapterStateCookiePath) {
            delegate.setAdapterStateCookiePath(adapterStateCookiePath);
        }

        @Override
        public String getStateCookieName() {
            return delegate.getStateCookieName();
        }

        @Override
        public void setStateCookieName(String stateCookieName) {
            delegate.setStateCookieName(stateCookieName);
        }

        @Override
        public boolean isUseResourceRoleMappings() {
            return delegate.isUseResourceRoleMappings();
        }

        @Override
        public void setUseResourceRoleMappings(boolean useResourceRoleMappings) {
            delegate.setUseResourceRoleMappings(useResourceRoleMappings);
        }

        @Override
        public boolean isCors() {
            return delegate.isCors();
        }

        @Override
        public void setCors(boolean cors) {
            delegate.setCors(cors);
        }

        @Override
        public int getCorsMaxAge() {
            return delegate.getCorsMaxAge();
        }

        @Override
        public void setCorsMaxAge(int corsMaxAge) {
            delegate.setCorsMaxAge(corsMaxAge);
        }

        @Override
        public String getCorsAllowedHeaders() {
            return delegate.getCorsAllowedHeaders();
        }

        @Override
        public void setNotBefore(int notBefore) {
            delegate.setNotBefore(notBefore);
        }

        @Override
        public int getNotBefore() {
            return delegate.getNotBefore();
        }

        @Override
        public void updateNotBefore(int notBefore) {
            delegate.setNotBefore(notBefore);
            getPublicKeyLocator().reset(this);
        }

        @Override
        public void setExposeToken(boolean exposeToken) {
            delegate.setExposeToken(exposeToken);
        }

        @Override
        public boolean isExposeToken() {
            return delegate.isExposeToken();
        }

        @Override
        public void setCorsAllowedMethods(String corsAllowedMethods) {
            delegate.setCorsAllowedMethods(corsAllowedMethods);
        }

        @Override
        public String getCorsAllowedMethods() {
            return delegate.getCorsAllowedMethods();
        }

        @Override
        public void setCorsAllowedHeaders(String corsAllowedHeaders) {
            delegate.setCorsAllowedHeaders(corsAllowedHeaders);
        }

        @Override
        public boolean isAlwaysRefreshToken() {
            return delegate.isAlwaysRefreshToken();
        }

        @Override
        public void setAlwaysRefreshToken(boolean alwaysRefreshToken) {
            delegate.setAlwaysRefreshToken(alwaysRefreshToken);
        }

        @Override
        public int getRegisterNodePeriod() {
            return delegate.getRegisterNodePeriod();
        }

        @Override
        public void setRegisterNodePeriod(int registerNodePeriod) {
            delegate.setRegisterNodePeriod(registerNodePeriod);
        }

        @Override
        public void setRegisterNodeAtStartup(boolean registerNodeAtStartup) {
            delegate.setRegisterNodeAtStartup(registerNodeAtStartup);
        }

        @Override
        public boolean isRegisterNodeAtStartup() {
            return delegate.isRegisterNodeAtStartup();
        }

        @Override
        public String getPrincipalAttribute() {
            return delegate.getPrincipalAttribute();
        }

        @Override
        public void setPrincipalAttribute(String principalAttribute) {
            delegate.setPrincipalAttribute(principalAttribute);
        }

        @Override
        public boolean isTurnOffChangeSessionIdOnLogin() {
            return delegate.isTurnOffChangeSessionIdOnLogin();
        }

        @Override
        public void setTurnOffChangeSessionIdOnLogin(boolean turnOffChangeSessionIdOnLogin) {
            delegate.setTurnOffChangeSessionIdOnLogin(turnOffChangeSessionIdOnLogin);
        }

        @Override
        public int getTokenMinimumTimeToLive() {
            return delegate.getTokenMinimumTimeToLive();
        }

        @Override
        public void setTokenMinimumTimeToLive(final int tokenMinimumTimeToLive) {
            delegate.setTokenMinimumTimeToLive(tokenMinimumTimeToLive);
        }

        @Override
        public PolicyEnforcer getPolicyEnforcer() {
            return delegate.getPolicyEnforcer();
        }

        @Override
        public void setPolicyEnforcer(Callable<PolicyEnforcer> policyEnforcer) {
            delegate.setPolicyEnforcer(policyEnforcer);
        }

        @Override
        public void setMinTimeBetweenJwksRequests(int minTimeBetweenJwksRequests) {
            delegate.setMinTimeBetweenJwksRequests(minTimeBetweenJwksRequests);
        }

        @Override
        public int getMinTimeBetweenJwksRequests() {
            return delegate.getMinTimeBetweenJwksRequests();
        }

        @Override
        public int getPublicKeyCacheTtl() {
            return delegate.getPublicKeyCacheTtl();
        }

        @Override
        public void setPublicKeyCacheTtl(int publicKeyCacheTtl) {
            delegate.setPublicKeyCacheTtl(publicKeyCacheTtl);
        }

        @Override
        public boolean isVerifyTokenAudience() {
            return delegate.isVerifyTokenAudience();
        }

        @Override
        public void setVerifyTokenAudience(boolean verifyTokenAudience) {
            delegate.setVerifyTokenAudience(verifyTokenAudience);
        }
    }

    protected KeycloakUriBuilder getBaseBuilder(HttpFacade facade, String base) {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(base);
        URI request = URI.create(facade.getRequest().getURI());
        String scheme = request.getScheme();
        if (deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            scheme = "https";
            if (!request.getScheme().equals(scheme) && request.getPort() != -1) {
                log.error("request scheme: " + request.getScheme() + " ssl required");
                throw new RuntimeException("Can't resolve relative url from adapter config.");
            }
        }
        builder.scheme(scheme);
        builder.host(request.getHost());
        if (request.getPort() != -1) {
           builder.port(request.getPort());
        }
        return builder;
    }



    protected void close(HttpResponse response) {
        if (response.getEntity() != null) {
            try {
                response.getEntity().getContent().close();
            } catch (IOException e) {

            }
        }
    }

    public void updateDeployment(AdapterConfig config) {
        if (null != configResolver) {
            throw new IllegalStateException("Cannot parse an adapter config and build an updated deployment when on a multi-tenant scenario.");
        }
        deployment = KeycloakDeploymentBuilder.build(config);
    }
}
