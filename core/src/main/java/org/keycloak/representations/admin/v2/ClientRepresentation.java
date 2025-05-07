package org.keycloak.representations.admin.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.LinkedHashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class ClientRepresentation {

    public static final String OIDC = "openid-connect";

    @JsonProperty(required = true)
    @JsonPropertyDescription("ID uniquely identifying this client")
    private String clientId;

    @JsonPropertyDescription("Human readable name of the client")
    private String displayName;

    @JsonPropertyDescription("Human readable description of the client")
    private String description;

    @JsonProperty(defaultValue = OIDC)
    @JsonPropertyDescription("The protocol used to communicate with the client")
    private String protocol = OIDC;

    @JsonPropertyDescription("Whether this client is enabled")
    private Boolean enabled;

    @JsonPropertyDescription("URL to the application's homepage that is represented by this client")
    private String appUrl;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("URLs that the browser can redirect to after login")
    private Set<String> appRedirectUrls = new LinkedHashSet<String>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Login flows that are enabled for this client")
    private Set<String> loginFlows = new LinkedHashSet<String>();

    @JsonPropertyDescription("Authentication configuration for this client")
    private Auth auth;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Web origins that are allowed to make requests to this client")
    private Set<String> webOrigins = new LinkedHashSet<String>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Roles associated with this client")
    private Set<String> roles = new LinkedHashSet<String>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Service account configuration for this client")
    private ServiceAccount serviceAccount;

    public ClientRepresentation() {}

    public ClientRepresentation(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        if (protocol == null) {
            protocol = OIDC;
        }
        this.protocol = protocol;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public Set<String> getAppRedirectUrls() {
        return appRedirectUrls;
    }

    public void setAppRedirectUrls(Set<String> appRedirectUrls) {
        this.appRedirectUrls = appRedirectUrls;
    }

    public Set<String> getLoginFlows() {
        return loginFlows;
    }

    public void setLoginFlows(Set<String> loginFlows) {
        this.loginFlows = loginFlows;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public ServiceAccount getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(ServiceAccount serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public static class Auth {

        @JsonPropertyDescription("Whether authentication is enabled for this client")
        private Boolean enabled;

        @JsonPropertyDescription("Which authentication method is used for this client")
        private String method;

        @JsonPropertyDescription("Secret used to authenticate this client with Secret authentication")
        private String secret;

        @JsonPropertyDescription("Public key used to authenticate this client with Signed JWT authentication")
        private String certificate;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public static class ServiceAccount {

        @JsonPropertyDescription("Whether the service account is enabled")
        private Boolean enabled;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonPropertyDescription("Roles assigned to the service account")
        private Set<String> roles = new LinkedHashSet<String>();

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }
    }
}

