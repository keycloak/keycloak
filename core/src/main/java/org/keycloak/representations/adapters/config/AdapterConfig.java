package org.keycloak.representations.adapters.config;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

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
        "auth-server-url-for-backend-requests", "always-refresh-token",
        "register-node-at-startup", "register-node-period", "token-store", "principal-attribute"
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
    @JsonProperty("auth-server-url-for-backend-requests")
    protected String authServerUrlForBackendRequests;
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

    public String getAuthServerUrlForBackendRequests() {
        return authServerUrlForBackendRequests;
    }

    public void setAuthServerUrlForBackendRequests(String authServerUrlForBackendRequests) {
        this.authServerUrlForBackendRequests = authServerUrlForBackendRequests;
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
}
