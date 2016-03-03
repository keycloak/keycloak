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

import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "keycloak", ignoreUnknownFields = false)
public class KeycloakSpringBootProperties extends AdapterConfig {

    private List<SecurityConstraint> securityConstraints = new ArrayList<SecurityConstraint>();

    public static class SecurityConstraint {
        private List<SecurityCollection> securityCollections = new ArrayList<SecurityCollection>();

        public List<SecurityCollection> getSecurityCollections() {
            return securityCollections;
        }

        public void setSecurityCollections(List<SecurityCollection> securityCollections) {
            this.securityCollections = securityCollections;
        }
    }

    public static class SecurityCollection {
        private String name;
        private String description;
        private List<String> authRoles = new ArrayList<String>();
        private List<String> patterns = new ArrayList<String>();
        private List<String> methods = new ArrayList<String>();
        private List<String> omittedMethods = new ArrayList<String>();

        public List<String> getAuthRoles() {
            return authRoles;
        }

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

        public void setAuthRoles(List<String> authRoles) {
            this.authRoles = authRoles;
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
