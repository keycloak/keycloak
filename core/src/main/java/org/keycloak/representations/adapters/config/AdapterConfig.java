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
 * @version $Revision: 1 $
 */
@JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-required",
        "resource", "public-client", "credentials",
        "use-resource-role-mappings",
        "enable-cors", "cors-max-age", "cors-allowed-methods",
        "expose-token", "bearer-only",
        "connection-pool-size",
        "allow-any-hostname", "disable-trust-manager", "truststore", "truststore-password",
        "client-keystore", "client-keystore-password", "client-key-password",
        "always-refresh-token",
        "register-node-at-startup", "register-node-period", "token-store", "principal-attribute",
        "proxy-url", "turn-off-change-session-id-on-login", "token-minimum-time-to-live",
        "policy-enforcer"
})
public class AdapterConfig extends BaseAdapterConfig {

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
    @JsonProperty("principal-attribute")
    protected String principalAttribute;
    @JsonProperty("turn-off-change-session-id-on-login")
    protected Boolean turnOffChangeSessionIdOnLogin;
    @JsonProperty("token-minimum-time-to-live")
    protected int tokenMinimumTimeToLive = 0;
    @JsonProperty("policy-enforcer")
    protected PolicyEnforcerConfig policyEnforcerConfig;

    /**
     * The Proxy url to use for requests to the auth-server, configurable via the adapter config property {@code proxy-url}.
     */
    @JsonProperty("proxy-url")
    protected String proxyUrl;

    public boolean isAllowAnyHostname() {
        return allowAnyHostname;
    }

    public void setAllowAnyHostname(boolean allowAnyHostname) {
        this.allowAnyHostname = allowAnyHostname;
    }

    public boolean isDisableTrustManager() {
        return disableTrustManager;
    }

    public void setDisableTrustManager(boolean disableTrustManager) {
        this.disableTrustManager = disableTrustManager;
    }

    public String getTruststore() {
        return truststore;
    }

    public void setTruststore(String truststore) {
        this.truststore = truststore;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getClientKeystore() {
        return clientKeystore;
    }

    public void setClientKeystore(String clientKeystore) {
        this.clientKeystore = clientKeystore;
    }

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

}
