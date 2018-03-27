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

import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthenticator;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.representation.ScopeRepresentation;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcer {

    private static Logger LOG = LoggerFactory.getLogger(PolicyEnforcer.class);

    private final KeycloakDeployment deployment;
    private final AuthzClient authzClient;
    private final PolicyEnforcerConfig enforcerConfig;
    private final Map<String, PathConfig> paths;
    private final PathMatcher pathMatcher;

    public PolicyEnforcer(KeycloakDeployment deployment, AdapterConfig adapterConfig) {
        this.deployment = deployment;
        this.enforcerConfig = adapterConfig.getPolicyEnforcerConfig();
        Configuration configuration = new Configuration(adapterConfig.getAuthServerUrl(), adapterConfig.getRealm(), adapterConfig.getResource(), adapterConfig.getCredentials(), deployment.getClient());
        this.authzClient = AuthzClient.create(configuration, new ClientAuthenticator() {
            @Override
            public void configureClientCredentials(Map<String, List<String>> requestParams, Map<String, String> requestHeaders) {
                Map<String, String> formparams = new HashMap<>();
                ClientCredentialsProviderUtils.setClientCredentials(PolicyEnforcer.this.deployment, requestHeaders, formparams);
                for (Entry<String, String> param : formparams.entrySet()) {
                    requestParams.put(param.getKey(), Arrays.asList(param.getValue()));
                }
            }
        });
        this.pathMatcher = new PathMatcher(this.authzClient);
        this.paths = configurePaths(this.authzClient.protection().resource(), this.enforcerConfig);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Initialization complete. Path configurations:");
            for (PathConfig pathConfig : this.paths.values()) {
                LOG.debug(pathConfig.toString());
            }
        }
    }

    public AuthorizationContext enforce(OIDCHttpFacade facade) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Policy enforcement is enable. Enforcing policy decisions for path [{}].", facade.getRequest().getURI());
        }

        AuthorizationContext context;

        if (deployment.isBearerOnly()) {
            context = new BearerTokenPolicyEnforcer(this).authorize(facade);
        } else {
            context = new KeycloakAdapterPolicyEnforcer(this).authorize(facade);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Policy enforcement result for path [{}] is : {}", facade.getRequest().getURI(), context.isGranted() ? "GRANTED" : "DENIED");
            LOG.debug("Returning authorization context with permissions:");
            for (Permission permission : context.getPermissions()) {
                LOG.debug(permission.toString());
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

    public Map<String, PathConfig> getPaths() {
        return paths;
    }

    void addPath(PathConfig pathConfig) {
        paths.put(pathConfig.getPath(), pathConfig);
    }

    KeycloakDeployment getDeployment() {
        return deployment;
    }

    private Map<String, PathConfig> configurePaths(ProtectedResource protectedResource, PolicyEnforcerConfig enforcerConfig) {
        boolean loadPathsFromServer = true;

        for (PathConfig pathConfig : enforcerConfig.getPaths()) {
            if (!PolicyEnforcerConfig.EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                loadPathsFromServer = false;
                break;
            }
        }

        if (loadPathsFromServer) {
            LOG.info("No path provided in configuration.");
            return configureAllPathsForResourceServer(protectedResource);
        } else {
            LOG.info("Paths provided in configuration.");
            return configureDefinedPaths(protectedResource, enforcerConfig);
        }
    }

    private Map<String, PathConfig> configureDefinedPaths(ProtectedResource protectedResource, PolicyEnforcerConfig enforcerConfig) {
        Map<String, PathConfig> paths = Collections.synchronizedMap(new HashMap<String, PathConfig>());

        for (PathConfig pathConfig : enforcerConfig.getPaths()) {
            ResourceRepresentation resource;
            String resourceName = pathConfig.getName();
            String path = pathConfig.getPath();

            if (resourceName != null) {
                LOG.debug("Trying to find resource with name [{}] for path [{}].", resourceName, path);
                resource = protectedResource.findByName(resourceName);
            } else {
                LOG.debug("Trying to find resource with uri [{}] for path [{}].", path, path);
                List<ResourceRepresentation> resources = protectedResource.findByUri(path);

                if (resources.size() == 1) {
                    resource = resources.get(0);
                } else if (resources.size() > 1) {
                    throw new RuntimeException("Multiple resources found with the same uri");
                } else {
                    resource = null;
                }
            }

            if (resource == null) {
                if (enforcerConfig.isCreateResources()) {
                    LOG.debug("Creating resource on server for path [{}].", pathConfig);
                    ResourceRepresentation representation = new ResourceRepresentation();

                    representation.setName(resourceName);
                    representation.setType(pathConfig.getType());
                    representation.setUri(path);

                    HashSet<ScopeRepresentation> scopes = new HashSet<>();

                    for (String scopeName : pathConfig.getScopes()) {
                        ScopeRepresentation scope = new ScopeRepresentation();

                        scope.setName(scopeName);

                        scopes.add(scope);
                    }

                    representation.setScopes(scopes);

                    ResourceRepresentation registrationResponse = protectedResource.create(representation);

                    pathConfig.setId(registrationResponse.getId());
                } else {
                    throw new RuntimeException("Could not find matching resource on server with uri [" + path + "] or name [" + resourceName + "]. Make sure you have created a resource on the server that matches with the path configuration.");
                }
            } else {
                pathConfig.setId(resource.getId());
            }

            PathConfig existingPath = null;

            for (PathConfig current : paths.values()) {
                if (current.getId().equals(pathConfig.getId()) && current.getPath().equals(pathConfig.getPath())) {
                    existingPath = current;
                    break;
                }
            }

            if (existingPath == null) {
                paths.put(pathConfig.getPath(), pathConfig);
            } else {
                existingPath.getMethods().addAll(pathConfig.getMethods());
                existingPath.getScopes().addAll(pathConfig.getScopes());
            }
        }

        return paths;
    }

    private Map<String, PathConfig> configureAllPathsForResourceServer(ProtectedResource protectedResource) {
        LOG.info("Querying the server for all resources associated with this application.");
        Map<String, PathConfig> paths = Collections.synchronizedMap(new HashMap<String, PathConfig>());

        for (String id : protectedResource.findAll()) {
            ResourceRepresentation resourceDescription = protectedResource.findById(id);

            if (resourceDescription.getUri() != null) {
                PathConfig pathConfig = createPathConfig(resourceDescription);
                paths.put(pathConfig.getPath(), pathConfig);
            }
        }

        return paths;
    }

    static PathConfig createPathConfig(ResourceRepresentation resourceDescription) {
        PathConfig pathConfig = new PathConfig();

        pathConfig.setId(resourceDescription.getId());
        pathConfig.setName(resourceDescription.getName());

        String uri = resourceDescription.getUri();

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

        return pathConfig;
    }

    public PathMatcher getPathMatcher() {
        return pathMatcher;
    }
}
