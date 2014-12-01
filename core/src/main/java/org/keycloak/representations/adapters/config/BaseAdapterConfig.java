package org.keycloak.representations.adapters.config;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

/**
 * Common Adapter configuration
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-required",
        "resource", "public-client", "credentials",
        "use-resource-role-mappings",
        "enable-cors", "cors-max-age", "cors-allowed-methods",
        "expose-token", "bearer-only", "enable-basic-auth"})
public class BaseAdapterConfig extends BaseRealmConfig {
    @JsonProperty("resource")
    protected String resource;
    @JsonProperty("use-resource-role-mappings")
    protected boolean useResourceRoleMappings;
    @JsonProperty("enable-cors")
    protected boolean cors;
    @JsonProperty("cors-max-age")
    protected int corsMaxAge = -1;
    @JsonProperty("cors-allowed-headers")
    protected String corsAllowedHeaders;
    @JsonProperty("cors-allowed-methods")
    protected String corsAllowedMethods;
    @JsonProperty("expose-token")
    protected boolean exposeToken;
    @JsonProperty("bearer-only")
    protected boolean bearerOnly;
    @JsonProperty("enable-basic-auth")
    protected boolean enableBasicAuth;
    @JsonProperty("public-client")
    protected boolean publicClient;
    @JsonProperty("credentials")
    protected Map<String, String> credentials = new HashMap<String, String>();


    public boolean isUseResourceRoleMappings() {
        return useResourceRoleMappings;
    }

    public void setUseResourceRoleMappings(boolean useResourceRoleMappings) {
        this.useResourceRoleMappings = useResourceRoleMappings;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public boolean isCors() {
         return cors;
     }

    public void setCors(boolean cors) {
         this.cors = cors;
     }

    public int getCorsMaxAge() {
         return corsMaxAge;
     }

    public void setCorsMaxAge(int corsMaxAge) {
         this.corsMaxAge = corsMaxAge;
     }

    public String getCorsAllowedHeaders() {
         return corsAllowedHeaders;
     }

    public void setCorsAllowedHeaders(String corsAllowedHeaders) {
         this.corsAllowedHeaders = corsAllowedHeaders;
     }

    public String getCorsAllowedMethods() {
         return corsAllowedMethods;
     }

    public void setCorsAllowedMethods(String corsAllowedMethods) {
         this.corsAllowedMethods = corsAllowedMethods;
     }

    public boolean isExposeToken() {
         return exposeToken;
     }

    public void setExposeToken(boolean exposeToken) {
         this.exposeToken = exposeToken;
     }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public boolean isEnableBasicAuth() {
        return enableBasicAuth;
    }

    public void setEnableBasicAuth(boolean enableBasicAuth) {
        this.enableBasicAuth = enableBasicAuth;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }
}
