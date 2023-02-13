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

package org.keycloak.adapters.springboot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "keycloak", ignoreUnknownFields = false)
public class KeycloakSpringBootProperties extends AdapterConfig {

    /* this is a dummy property to avoid re-rebinding problem with property keycloak.config.resolver
       when using spring cloud - see KEYCLOAK-2977 */
    @JsonIgnore
    private Map config = new HashMap();

    /**
     * Allow enabling of Keycloak Spring Boot adapter by configuration.
     */
    private boolean enabled = true;

    public Map getConfig() {
        return config;
    }

    /**
     * To provide Java EE security constraints
     */
    private List<SecurityConstraint> securityConstraints = new ArrayList<SecurityConstraint>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * This matches security-constraint of the servlet spec
     */
    @ConfigurationProperties()
    public static class SecurityConstraint {
        /**
         * A list of security collections
         */
        private List<SecurityCollection> securityCollections = new ArrayList<SecurityCollection>();
        private List<String> authRoles = new ArrayList<String>();

        public List<String> getAuthRoles() {
            return authRoles;
        }

        public List<SecurityCollection> getSecurityCollections() {
            return securityCollections;
        }

        public void setSecurityCollections(List<SecurityCollection> securityCollections) {
            this.securityCollections = securityCollections;
        }

        public void setAuthRoles(List<String> authRoles) {
            this.authRoles = authRoles;
        }

    }

    /**
     * This matches web-resource-collection of the servlet spec
     */
    @ConfigurationProperties()
    public static class SecurityCollection {
        /**
         * The name of your security constraint
         */
        private String name;
        /**
         * The description of your security collection
         */
        private String description;
        /**
         * A list of URL patterns that should match to apply the security collection
         */
        private List<String> patterns = new ArrayList<String>();
        /**
         * A list of HTTP methods that applies for this security collection
         */
        private List<String> methods = new ArrayList<String>();
        /**
         * A list of HTTP methods that will be omitted for this security collection
         */
        private List<String> omittedMethods = new ArrayList<String>();

        public List<String> getPatterns() {
            return patterns;
        }

        public List<String> getMethods() {
            return methods;
        }

        public String getDescription() {
            return description;
        }

        public String getName() {
            return name;
        }

        public List<String> getOmittedMethods() {
            return omittedMethods;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setPatterns(List<String> patterns) {
            this.patterns = patterns;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public void setOmittedMethods(List<String> omittedMethods) {
            this.omittedMethods = omittedMethods;
        }
    }

    public List<SecurityConstraint> getSecurityConstraints() {
        return securityConstraints;
    }

    public void setSecurityConstraints(List<SecurityConstraint> securityConstraints) {
        this.securityConstraints = securityConstraints;
    }
}
