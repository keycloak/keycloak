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

package org.keycloak.representations.adapters.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Configuration for Java based adapters
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:brad.culley@spartasystems.com">Brad Culley</a>
 * @author <a href="mailto:john.ament@spartasystems.com">John D. Ament</a>
 * @version $Revision: 1 $
 */
@JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-required",
        "resource", "public-client", "credentials",
        "use-resource-role-mappings",
        "enable-cors", "cors-max-age", "cors-allowed-methods", "cors-exposed-headers",
        "expose-token", "bearer-only", "autodetect-bearer-only",
        "connection-pool-size", "socket-timeout-millis", "connection-ttl-millis", "connection-timeout-millis",
        "allow-any-hostname", "disable-trust-manager", "truststore", "truststore-password",
        "client-keystore", "client-keystore-password", "client-key-password",
        "always-refresh-token",
        "register-node-at-startup", "register-node-period", "token-store", "adapter-state-cookie-path", "principal-attribute",
        "proxy-url", "turn-off-change-session-id-on-login", "token-minimum-time-to-live",
        "min-time-between-jwks-requests", "public-key-cache-ttl",
        "policy-enforcer", "ignore-oauth-query-parameter", "verify-token-audience"
})
public class AdapterConfig extends BaseAdapterConfig implements AdapterHttpClientConfig {

    @JsonProperty("allow-any-hostname")
    protected boolean allowAnyHostname;
    @JsonProperty("disable-trust-manager")
    protected boolean disableTrustManager;
    @JsonProperty("truststore")
    protected String truststore;
    @JsonProperty("truststore-password")
    protected String truststorePassword;
    @JsonProperty("client-keystore")
    protected String clientKeystore;
    @JsonProperty("client-keystore-password")
    protected String clientKeystorePassword;
    @JsonProperty("client-key-password")
    protected String clientKeyPassword;
    @JsonProperty("connection-pool-size")
    protected int connectionPoolSize = 20;
    @JsonProperty("always-refresh-token")
    protected boolean alwaysRefreshToken = false;
    @JsonProperty("register-node-at-startup")
    protected boolean registerNodeAtStartup = false;
    @JsonProperty("register-node-period")
    protected int registerNodePeriod = -1;
    @JsonProperty("token-store")
    protected String tokenStore;
    @JsonProperty("adapter-state-cookie-path")
    protected String tokenCookiePath;
    @JsonProperty("principal-attribute")
    protected String principalAttribute;
    @JsonProperty("turn-off-change-session-id-on-login")
    protected Boolean turnOffChangeSessionIdOnLogin;
    @JsonProperty("token-minimum-time-to-live")
    protected int tokenMinimumTimeToLive = 0;
    @JsonProperty("min-time-between-jwks-requests")
    protected int minTimeBetweenJwksRequests = 10;
    @JsonProperty("public-key-cache-ttl")
    protected int publicKeyCacheTtl = 86400; // 1 day
    @JsonProperty("policy-enforcer")
    protected PolicyEnforcerConfig policyEnforcerConfig;
    // https://tools.ietf.org/html/rfc7636
    @JsonProperty("enable-pkce")
    protected boolean pkce = false;
    @JsonProperty("ignore-oauth-query-parameter")
    protected boolean ignoreOAuthQueryParameter = false;
    @JsonProperty("verify-token-audience")
    protected boolean verifyTokenAudience = false;

    @JsonProperty("socket-timeout-millis")
    protected long socketTimeout = -1L;
    @JsonProperty("connection-timeout-millis")
    protected long connectionTimeout = -1L;
    @JsonProperty("connection-ttl-millis")
    protected long connectionTTL = -1L;

    /**
     * The Proxy url to use for requests to the auth-server, configurable via the adapter config property {@code proxy-url}.
     */
    @JsonProperty("proxy-url")
    protected String proxyUrl;

    @Override
    public boolean isAllowAnyHostname() {
        return allowAnyHostname;
    }

    public void setAllowAnyHostname(boolean allowAnyHostname) {
        this.allowAnyHostname = allowAnyHostname;
    }

    @Override
    public boolean isDisableTrustManager() {
        return disableTrustManager;
    }

    public void setDisableTrustManager(boolean disableTrustManager) {
        this.disableTrustManager = disableTrustManager;
    }

    @Override
    public String getTruststore() {
        return truststore;
    }

    public void setTruststore(String truststore) {
        this.truststore = truststore;
    }

    @Override
    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    @Override
    public String getClientKeystore() {
        return clientKeystore;
    }

    public void setClientKeystore(String clientKeystore) {
        this.clientKeystore = clientKeystore;
    }

    @Override
    public String getClientKeystorePassword() {
        return clientKeystorePassword;
    }

    public void setClientKeystorePassword(String clientKeystorePassword) {
        this.clientKeystorePassword = clientKeystorePassword;
    }

    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        this.clientKeyPassword = clientKeyPassword;
    }

    @Override
    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public boolean isAlwaysRefreshToken() {
        return alwaysRefreshToken;
    }

    public void setAlwaysRefreshToken(boolean alwaysRefreshToken) {
        this.alwaysRefreshToken = alwaysRefreshToken;
    }

    public boolean isRegisterNodeAtStartup() {
        return registerNodeAtStartup;
    }

    public void setRegisterNodeAtStartup(boolean registerNodeAtStartup) {
        this.registerNodeAtStartup = registerNodeAtStartup;
    }

    public int getRegisterNodePeriod() {
        return registerNodePeriod;
    }

    public void setRegisterNodePeriod(int registerNodePeriod) {
        this.registerNodePeriod = registerNodePeriod;
    }

    public String getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(String tokenStore) {
        this.tokenStore = tokenStore;
    }

    public String getTokenCookiePath() {
        return tokenCookiePath;
    }

    public void setTokenCookiePath(String tokenCookiePath) {
        this.tokenCookiePath = tokenCookiePath;
    }

    public String getPrincipalAttribute() {
        return principalAttribute;
    }

    public void setPrincipalAttribute(String principalAttribute) {
        this.principalAttribute = principalAttribute;
    }

    public Boolean getTurnOffChangeSessionIdOnLogin() {
        return turnOffChangeSessionIdOnLogin;
    }

    public void setTurnOffChangeSessionIdOnLogin(Boolean turnOffChangeSessionIdOnLogin) {
        this.turnOffChangeSessionIdOnLogin = turnOffChangeSessionIdOnLogin;
    }

    public PolicyEnforcerConfig getPolicyEnforcerConfig() {
        return policyEnforcerConfig;
    }

    public void setPolicyEnforcerConfig(PolicyEnforcerConfig policyEnforcerConfig) {
        this.policyEnforcerConfig = policyEnforcerConfig;
    }

    @Override
    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public int getTokenMinimumTimeToLive() {
        return tokenMinimumTimeToLive;
    }

    public void setTokenMinimumTimeToLive(final int tokenMinimumTimeToLive) {
        this.tokenMinimumTimeToLive = tokenMinimumTimeToLive;
    }

    public int getMinTimeBetweenJwksRequests() {
        return minTimeBetweenJwksRequests;
    }

    public void setMinTimeBetweenJwksRequests(int minTimeBetweenJwksRequests) {
        this.minTimeBetweenJwksRequests = minTimeBetweenJwksRequests;
    }

    public int getPublicKeyCacheTtl() {
        return publicKeyCacheTtl;
    }

    public void setPublicKeyCacheTtl(int publicKeyCacheTtl) {
        this.publicKeyCacheTtl = publicKeyCacheTtl;
    }

    // https://tools.ietf.org/html/rfc7636
    public boolean isPkce() {
        return pkce;
    }

    public void setPkce(boolean pkce) {
        this.pkce = pkce;
    }

    public boolean isIgnoreOAuthQueryParameter() {
        return ignoreOAuthQueryParameter;
    }

    public void setIgnoreOAuthQueryParameter(boolean ignoreOAuthQueryParameter) {
        this.ignoreOAuthQueryParameter = ignoreOAuthQueryParameter;
    }

    public boolean isVerifyTokenAudience() {
        return verifyTokenAudience;
    }

    public void setVerifyTokenAudience(boolean verifyTokenAudience) {
        this.verifyTokenAudience = verifyTokenAudience;
    }

    public long getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(long socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public long getConnectionTTL() {
        return connectionTTL;
    }

    public void setConnectionTTL(long connectionTTL) {
        this.connectionTTL = connectionTTL;
    }
}
