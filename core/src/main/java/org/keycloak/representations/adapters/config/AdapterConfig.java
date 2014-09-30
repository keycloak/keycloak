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
        "resource", "credentials",
        "use-resource-role-mappings",
        "enable-cors", "cors-max-age", "cors-allowed-methods",
        "expose-token", "bearer-only",
        "connection-pool-size",
        "allow-any-hostname", "disable-trust-manager", "truststore", "truststore-password",
        "client-keystore", "client-keystore-password", "client-key-password",
        "use-hostname-for-local-requests", "local-requests-scheme", "local-requests-port"
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
    @JsonProperty("use-hostname-for-local-requests")
    protected boolean useHostnameForLocalRequests;
    @JsonProperty("local-requests-scheme")
    protected String localRequestsScheme = "http";
    @JsonProperty("local-requests-port")
    protected int localRequestsPort = 8080;

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

    public boolean isUseHostnameForLocalRequests() {
        return useHostnameForLocalRequests;
    }

    public void setUseHostnameForLocalRequests(boolean useHostnameForLocalRequests) {
        this.useHostnameForLocalRequests = useHostnameForLocalRequests;
    }

    public String getLocalRequestsScheme() {
        return localRequestsScheme;
    }

    public void setLocalRequestsScheme(String localRequestsScheme) {
        this.localRequestsScheme = localRequestsScheme;
    }

    public int getLocalRequestsPort() {
        return localRequestsPort;
    }

    public void setLocalRequestsPort(int localRequestsPort) {
        this.localRequestsPort = localRequestsPort;
    }
}
