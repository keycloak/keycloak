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

import java.util.Map;
import java.util.TreeMap;

/**
 * Common Adapter configuration
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-required",
        "resource", "public-client", "credentials",
        "use-resource-role-mappings",
        "enable-cors", "cors-max-age", "cors-allowed-methods", "cors-exposed-headers",
        "expose-token", "bearer-only", "autodetect-bearer-only", "enable-basic-auth"})
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
    @JsonProperty("cors-exposed-headers")
    protected String corsExposedHeaders;
    @JsonProperty("expose-token")
    protected boolean exposeToken;
    @JsonProperty("bearer-only")
    protected boolean bearerOnly;
    @JsonProperty("autodetect-bearer-only")
    protected boolean autodetectBearerOnly;
    @JsonProperty("enable-basic-auth")
    protected boolean enableBasicAuth;
    @JsonProperty("public-client")
    protected boolean publicClient;
    @JsonProperty("credentials")
    protected Map<String, Object> credentials = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
     @JsonProperty("redirect-rewrite-rules")
    protected Map<String, String> redirectRewriteRules;

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

    public String getCorsExposedHeaders() {
        return corsExposedHeaders;
    }

    public void setCorsExposedHeaders(String corsExposedHeaders) {
        this.corsExposedHeaders = corsExposedHeaders;
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

    public boolean isAutodetectBearerOnly() {
        return autodetectBearerOnly;
    }

    public void setAutodetectBearerOnly(boolean autodetectBearerOnly) {
        this.autodetectBearerOnly = autodetectBearerOnly;
    }

    public boolean isEnableBasicAuth() {
        return enableBasicAuth;
    }

    public void setEnableBasicAuth(boolean enableBasicAuth) {
        this.enableBasicAuth = enableBasicAuth;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public Map<String, String> getRedirectRewriteRules() {
        return redirectRewriteRules;
    }

    public void setRedirectRewriteRules(Map<String, String> redirectRewriteRules) {
        this.redirectRewriteRules = redirectRewriteRules;
    }
    
    
}
