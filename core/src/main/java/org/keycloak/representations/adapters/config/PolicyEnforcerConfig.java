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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcerConfig {

    @JsonProperty("create-resources")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean createResources = Boolean.FALSE;

    @JsonProperty("enforcement-mode")
    private EnforcementMode enforcementMode = EnforcementMode.ENFORCING;

    @JsonProperty("user-managed-access")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UmaProtocolConfig userManagedAccess;

    @JsonProperty("entitlement")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private EntitlementProtocolConfig entitlement;

    @JsonProperty("paths")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<PathConfig> paths = new ArrayList<>();

    @JsonProperty("online-introspection")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean onlineIntrospection = Boolean.FALSE;

    @JsonProperty("on-deny-redirect-to")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String onDenyRedirectTo;

    public Boolean isCreateResources() {
        return this.createResources;
    }

    public List<PathConfig> getPaths() {
        return this.paths;
    }

    public EnforcementMode getEnforcementMode() {
        return this.enforcementMode;
    }

    public void setEnforcementMode(EnforcementMode enforcementMode) {
        this.enforcementMode = enforcementMode;
    }

    public UmaProtocolConfig getUserManagedAccess() {
        return this.userManagedAccess;
    }

    public EntitlementProtocolConfig getEntitlement() {
        return this.entitlement;
    }

    public Boolean isOnlineIntrospection() {
        return onlineIntrospection;
    }

    public void setCreateResources(Boolean createResources) {
        this.createResources = createResources;
    }

    public void setOnlineIntrospection(Boolean onlineIntrospection) {
        this.onlineIntrospection = onlineIntrospection;
    }

    public void setPaths(List<PathConfig> paths) {
        this.paths = paths;
    }

    public String getOnDenyRedirectTo() {
        return onDenyRedirectTo;
    }

    public void setUserManagedAccess(UmaProtocolConfig userManagedAccess) {
        this.userManagedAccess = userManagedAccess;
    }

    public void setEntitlement(EntitlementProtocolConfig entitlement) {
        this.entitlement = entitlement;
    }

    public void setOnDenyRedirectTo(String onDenyRedirectTo) {
        this.onDenyRedirectTo = onDenyRedirectTo;
    }

    public static class PathConfig {

        private String name;
        private String type;
        private String path;
        private List<MethodConfig> methods = new ArrayList<>();
        private List<String> scopes = Collections.emptyList();
        private String id;

        @JsonProperty("enforcement-mode")
        private EnforcementMode enforcementMode = EnforcementMode.ENFORCING;

        @JsonIgnore
        private PathConfig parentConfig;

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
    }

    public static class MethodConfig {

        private String method;
        private List<String> scopes = Collections.emptyList();

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
    }

    public enum EnforcementMode {
        PERMISSIVE,
        ENFORCING,
        DISABLED
    }

    public static class UmaProtocolConfig {

    }

    public static class EntitlementProtocolConfig {

    }
}
