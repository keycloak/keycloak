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
package org.keycloak.adapters.authorization;

import org.jboss.logging.Logger;
import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.RegistrationResponse;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.representation.ScopeRepresentation;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(PolicyEnforcer.class);

    private final KeycloakDeployment deployment;
    private final AuthzClient authzClient;
    private final PolicyEnforcerConfig enforcerConfig;
    private final List<PathConfig> paths;

    public PolicyEnforcer(KeycloakDeployment deployment, AdapterConfig adapterConfig) {
        this.deployment = deployment;
        this.enforcerConfig = adapterConfig.getPolicyEnforcerConfig();
        this.authzClient = AuthzClient.create(new Configuration(adapterConfig.getAuthServerUrl(), adapterConfig.getRealm(), adapterConfig.getResource(), adapterConfig.getCredentials(), deployment.getClient()));
        this.paths = configurePaths(this.authzClient.protection().resource(), this.enforcerConfig);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Initialization complete. Path configurations:");
            for (PathConfig pathConfig : this.paths) {
                LOGGER.debug(pathConfig);
            }
        }
    }

    public AuthorizationContext enforce(OIDCHttpFacade facade) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Policy enforcement is enable. Enforcing policy decisions for path [{0}].", facade.getRequest().getURI());
        }

        AuthorizationContext context;

        if (deployment.isBearerOnly()) {
            context = new BearerTokenPolicyEnforcer(this).authorize(facade);
        } else {
            context = new KeycloakAdapterPolicyEnforcer(this).authorize(facade);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Policy enforcement result for path [{0}] is : {1}", facade.getRequest().getURI(), context.isGranted() ? "GRANTED" : "DENIED");
            LOGGER.debugv("Returning authorization context with permissions:");
            for (Permission permission : context.getPermissions()) {
                LOGGER.debug(permission);
            }
        }

        return context;
    }

    PolicyEnforcerConfig getEnforcerConfig() {
        return enforcerConfig;
    }

    AuthzClient getClient() {
        return authzClient;
    }

    List<PathConfig> getPaths() {
        return paths;
    }

    KeycloakDeployment getDeployment() {
        return deployment;
    }

    private List<PathConfig> configurePaths(ProtectedResource protectedResource, PolicyEnforcerConfig enforcerConfig) {
        if (enforcerConfig.getPaths().isEmpty()) {
            LOGGER.info("No path provided in configuration.");
            return configureAllPathsForResourceServer(protectedResource);
        } else {
            LOGGER.info("Paths provided in configuration.");
            return configureDefinedPaths(protectedResource, enforcerConfig);
        }
    }

    private List<PathConfig> configureDefinedPaths(ProtectedResource protectedResource, PolicyEnforcerConfig enforcerConfig) {
        List<PathConfig> paths = new ArrayList<>();

        for (PathConfig pathConfig : enforcerConfig.getPaths()) {
            Set<String> search;
            String resourceName = pathConfig.getName();
            String path = pathConfig.getPath();

            if (resourceName != null) {
                LOGGER.debugf("Trying to find resource with name [%s] for path [%s].", resourceName, path);
                search = protectedResource.findByFilter("name=" + resourceName);
            } else {
                LOGGER.debugf("Trying to find resource with uri [%s] for path [%s].", path, path);
                search = protectedResource.findByFilter("uri=" + path);
            }

            if (search.isEmpty()) {
                if (enforcerConfig.isCreateResources()) {
                    LOGGER.debugf("Creating resource on server for path [%s].", pathConfig);
                    ResourceRepresentation resource = new ResourceRepresentation();

                    resource.setName(resourceName);
                    resource.setType(pathConfig.getType());
                    resource.setUri(path);

                    HashSet<ScopeRepresentation> scopes = new HashSet<>();

                    for (String scopeName : pathConfig.getScopes()) {
                        ScopeRepresentation scope = new ScopeRepresentation();

                        scope.setName(scopeName);

                        scopes.add(scope);
                    }

                    resource.setScopes(scopes);

                    RegistrationResponse registrationResponse = protectedResource.create(resource);

                    pathConfig.setId(registrationResponse.getId());
                } else {
                    throw new RuntimeException("Could not find matching resource on server with uri [" + path + "] or name [" + resourceName + ". Make sure you have created a resource on the server that matches with the path configuration.");
                }
            } else {
                pathConfig.setId(search.iterator().next());
            }

            paths.add(pathConfig);
        }

        return paths;
    }

    private List<PathConfig> configureAllPathsForResourceServer(ProtectedResource protectedResource) {
        LOGGER.info("Querying the server for all resources associated with this application.");
        List<PathConfig> paths = new ArrayList<>();

        for (String id : protectedResource.findAll()) {
            RegistrationResponse response = protectedResource.findById(id);
            ResourceRepresentation resourceDescription = response.getResourceDescription();

            if (resourceDescription.getUri() != null) {
                paths.add(createPathConfig(resourceDescription));
            }
        }

        return paths;
    }

    private PathConfig createPathConfig(ResourceRepresentation resourceDescription) {
        PathConfig pathConfig = new PathConfig();

        pathConfig.setId(resourceDescription.getId());
        pathConfig.setName(resourceDescription.getName());
        pathConfig.setPath(resourceDescription.getUri());

        List<String> scopeNames = new ArrayList<>();

        for (ScopeRepresentation scope : resourceDescription.getScopes()) {
            scopeNames.add(scope.getName());
        }

        pathConfig.setScopes(scopeNames);
        pathConfig.setType(resourceDescription.getType());

        return pathConfig;
    }
}
