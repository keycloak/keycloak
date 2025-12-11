/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.representations.adapters.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcerConfig {

    @JsonProperty("enforcement-mode")
    private EnforcementMode enforcementMode = EnforcementMode.ENFORCING;

    @JsonProperty("paths")
    @JsonInclude(Include.NON_EMPTY)
    private List<PathConfig> paths = new ArrayList<>();

    @JsonProperty("path-cache")
    @JsonInclude(Include.NON_EMPTY)
    private PathCacheConfig pathCacheConfig;

    @JsonProperty("lazy-load-paths")
    private Boolean lazyLoadPaths = Boolean.FALSE;

    @JsonProperty("on-deny-redirect-to")
    @JsonInclude(Include.NON_NULL)
    private String onDenyRedirectTo;

    @JsonProperty("user-managed-access")
    @JsonInclude(Include.NON_NULL)
    private UserManagedAccessConfig userManagedAccess;

    @JsonProperty("claim-information-point")
    @JsonInclude(Include.NON_NULL)
    private Map<String, Map<String, Object>> claimInformationPointConfig;

    @JsonProperty("http-method-as-scope")
    private Boolean httpMethodAsScope;

    private String realm;

    @JsonProperty("auth-server-url")
    private String authServerUrl;

    @JsonProperty("credentials")
    protected Map<String, Object> credentials = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @JsonProperty("resource")
    private String resource;

    public List<PathConfig> getPaths() {
        return this.paths;
    }

    public PathCacheConfig getPathCacheConfig() {
        return pathCacheConfig;
    }

    public Boolean getLazyLoadPaths() {
        return lazyLoadPaths;
    }

    public void setLazyLoadPaths(Boolean lazyLoadPaths) {
        this.lazyLoadPaths = lazyLoadPaths;
    }

    public EnforcementMode getEnforcementMode() {
        return this.enforcementMode;
    }

    public void setEnforcementMode(EnforcementMode enforcementMode) {
        this.enforcementMode = enforcementMode;
    }

    public UserManagedAccessConfig getUserManagedAccess() {
        return this.userManagedAccess;
    }

    public void setPaths(List<PathConfig> paths) {
        this.paths = paths;
    }

    public void setPathCacheConfig(PathCacheConfig pathCacheConfig) {
        this.pathCacheConfig = pathCacheConfig;
    }

    public String getOnDenyRedirectTo() {
        return onDenyRedirectTo;
    }

    public void setUserManagedAccess(UserManagedAccessConfig userManagedAccess) {
        this.userManagedAccess = userManagedAccess;
    }

    public void setOnDenyRedirectTo(String onDenyRedirectTo) {
        this.onDenyRedirectTo = onDenyRedirectTo;
    }

    public Map<String, Map<String, Object>> getClaimInformationPointConfig() {
        return claimInformationPointConfig;
    }

    public void setClaimInformationPointConfig(Map<String, Map<String, Object>> config) {
        this.claimInformationPointConfig = config;
    }

    public Boolean getHttpMethodAsScope() {
        return httpMethodAsScope;
    }

    public void setHttpMethodAsScope(Boolean httpMethodAsScope) {
        this.httpMethodAsScope = httpMethodAsScope;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public static class PathConfig {

        public static Set<PathConfig> createPathConfigs(ResourceRepresentation resourceDescription) {
            Set<PathConfig> pathConfigs = new HashSet<>();

            for (String uri : resourceDescription.getUris()) {

                PathConfig pathConfig = new PathConfig();

                pathConfig.setId(resourceDescription.getId());
                pathConfig.setName(resourceDescription.getName());

                if (uri == null || "".equals(uri.trim())) {
                    throw new RuntimeException("Failed to configure paths. Resource [" + resourceDescription.getName() + "] has an invalid or empty URI [" + uri + "].");
                }

                pathConfig.setPath(uri);

                List<String> scopeNames = new ArrayList<>();

                for (ScopeRepresentation scope : resourceDescription.getScopes()) {
                    scopeNames.add(scope.getName());
                }

                pathConfig.setScopes(scopeNames);
                pathConfig.setType(resourceDescription.getType());

                pathConfigs.add(pathConfig);
            }

            return pathConfigs;
        }

        private String name;
        private String type;
        private String path;
        private List<MethodConfig> methods = new ArrayList<>();
        private List<String> scopes = new ArrayList<>();
        private String id;

        @JsonProperty("enforcement-mode")
        private EnforcementMode enforcementMode = EnforcementMode.ENFORCING;

        @JsonProperty("claim-information-point")
        private Map<String, Map<String, Object>> claimInformationPointConfig;

        @JsonIgnore
        private PathConfig parentConfig;

        private boolean invalidated;

        private boolean staticPath;

        public String getPath() {
            return this.path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<String> getScopes() {
            return this.scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public List<MethodConfig> getMethods() {
            return methods;
        }

        public void setMethods(List<MethodConfig> methods) {
            this.methods = methods;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public EnforcementMode getEnforcementMode() {
            return enforcementMode;
        }

        public void setEnforcementMode(EnforcementMode enforcementMode) {
            this.enforcementMode = enforcementMode;
        }

        public Map<String, Map<String, Object>> getClaimInformationPointConfig() {
            return claimInformationPointConfig;
        }

        public void setClaimInformationPointConfig(Map<String, Map<String, Object>> claimInformationPointConfig) {
            this.claimInformationPointConfig = claimInformationPointConfig;
        }

        @Override
        public String toString() {
            return "PathConfig{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", path='" + path + '\'' +
                    ", scopes=" + scopes +
                    ", id='" + id + '\'' +
                    ", enforcerMode='" + enforcementMode + '\'' +
                    '}';
        }

        @JsonIgnore
        public boolean hasPattern() {
            return getPath().indexOf("{") != -1;
        }

        @JsonIgnore
        public boolean isInstance() {
            return this.parentConfig != null;
        }

        public void setParentConfig(PathConfig parentConfig) {
            this.parentConfig = parentConfig;
        }

        public PathConfig getParentConfig() {
            return parentConfig;
        }

        public void invalidate() {
            this.invalidated = true;
        }

        public boolean isInvalidated() {
            return invalidated;
        }

        public boolean isStatic() {
            return staticPath;
        }

        public void setStatic(boolean staticPath) {
            this.staticPath = staticPath;
        }
    }

    public static class MethodConfig {

        private String method;
        private List<String> scopes = new ArrayList<>();

        @JsonProperty("scopes-enforcement-mode")
        private ScopeEnforcementMode scopesEnforcementMode = ScopeEnforcementMode.ALL;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public void setScopesEnforcementMode(ScopeEnforcementMode scopesEnforcementMode) {
            this.scopesEnforcementMode = scopesEnforcementMode;
        }

        public ScopeEnforcementMode getScopesEnforcementMode() {
            return scopesEnforcementMode;
        }
    }

    public static class PathCacheConfig {

        @JsonProperty("max-entries")
        int maxEntries = 1000;
        @JsonProperty("lifespan")
        long lifespan = 30000;

        public int getMaxEntries() {
            return maxEntries;
        }

        public void setMaxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
        }

        public long getLifespan() {
            return lifespan;
        }

        public void setLifespan(long lifespan) {
            this.lifespan = lifespan;
        }
    }

    public enum EnforcementMode {
        PERMISSIVE,
        ENFORCING,
        DISABLED
    }

    public enum ScopeEnforcementMode {
        ALL,
        ANY,
        DISABLED
    }

    public static class UserManagedAccessConfig {

    }
}
