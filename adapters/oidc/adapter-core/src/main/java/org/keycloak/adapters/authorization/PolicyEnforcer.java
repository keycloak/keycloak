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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;

import org.jboss.logging.Logger;
import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthenticator;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.common.util.PathMatcher;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathCacheConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(PolicyEnforcer.class);

    private final KeycloakDeployment deployment;
    private final AuthzClient authzClient;
    private final PolicyEnforcerConfig enforcerConfig;
    private final PathConfigMatcher pathMatcher;
    private final Map<String, PathConfig> paths;
    private final Map<String, ClaimInformationPointProviderFactory> claimInformationPointProviderFactories = new HashMap<>();

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

        paths = configurePaths(this.authzClient.protection().resource(), this.enforcerConfig);
        pathMatcher = new PathConfigMatcher(paths, enforcerConfig, authzClient);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Initialization complete. Path configurations:");
            for (PathConfig pathConfig : this.paths.values()) {
                LOGGER.debug(pathConfig);
            }
        }

        loadClaimInformationPointProviders(ServiceLoader.load(ClaimInformationPointProviderFactory.class, ClaimInformationPointProviderFactory.class.getClassLoader()));
        loadClaimInformationPointProviders(ServiceLoader.load(ClaimInformationPointProviderFactory.class, Thread.currentThread().getContextClassLoader()));
    }

    public AuthorizationContext enforce(OIDCHttpFacade facade) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Policy enforcement is enabled. Enforcing policy decisions for path [{0}].", facade.getRequest().getURI());
        }

        AuthorizationContext context = new KeycloakAdapterPolicyEnforcer(this).authorize(facade);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Policy enforcement result for path [{0}] is : {1}", facade.getRequest().getURI(), context.isGranted() ? "GRANTED" : "DENIED");
            LOGGER.debugv("Returning authorization context with permissions:");
            for (Permission permission : context.getPermissions()) {
                LOGGER.debug(permission);
            }
        }

        return context;
    }

    public PolicyEnforcerConfig getEnforcerConfig() {
        return enforcerConfig;
    }

    public AuthzClient getClient() {
        return authzClient;
    }

    public Map<String, PathConfig> getPaths() {
        return paths;
    }

    public PathConfigMatcher getPathMatcher() {
        return pathMatcher;
    }

    public KeycloakDeployment getDeployment() {
        return deployment;
    }

    public Map<String, ClaimInformationPointProviderFactory> getClaimInformationPointProviderFactories() {
        return claimInformationPointProviderFactories;
    }

    private void loadClaimInformationPointProviders(ServiceLoader<ClaimInformationPointProviderFactory> loader) {

        for (ClaimInformationPointProviderFactory factory : loader) {
            factory.init(this);

            claimInformationPointProviderFactories.put(factory.getName(), factory);
        }
    }

    private Map<String, PathConfig> configurePaths(ProtectedResource protectedResource, PolicyEnforcerConfig enforcerConfig) {
        boolean loadPathsFromServer = !enforcerConfig.getLazyLoadPaths();

        for (PathConfig pathConfig : enforcerConfig.getPaths()) {
            if (!PolicyEnforcerConfig.EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                loadPathsFromServer = false;
                break;
            }
        }

        if (loadPathsFromServer) {
            LOGGER.info("No path provided in configuration.");
            Map<String, PathConfig> paths = configureAllPathsForResourceServer(protectedResource);

            paths.putAll(configureDefinedPaths(protectedResource, enforcerConfig));

            return paths;
        } else {
            LOGGER.info("Paths provided in configuration.");
            return configureDefinedPaths(protectedResource, enforcerConfig);
        }
    }

    private Map<String, PathConfig> configureDefinedPaths(ProtectedResource protectedResource, PolicyEnforcerConfig enforcerConfig) {
        Map<String, PathConfig> paths = Collections.synchronizedMap(new LinkedHashMap<String, PathConfig>());

        for (PathConfig pathConfig : enforcerConfig.getPaths()) {
            ResourceRepresentation resource;
            String resourceName = pathConfig.getName();
            String path = pathConfig.getPath();

            if (resourceName != null) {
                LOGGER.debugf("Trying to find resource with name [%s] for path [%s].", resourceName, path);
                resource = protectedResource.findByName(resourceName);
            } else {
                LOGGER.debugf("Trying to find resource with uri [%s] for path [%s].", path, path);
                List<ResourceRepresentation> resources = protectedResource.findByUri(path);

                if (resources.isEmpty()) {
                    resources = protectedResource.findByMatchingUri(path);
                }

                if (resources.size() == 1) {
                    resource = resources.get(0);
                } else if (resources.size() > 1) {
                    throw new RuntimeException("Multiple resources found with the same uri");
                } else {
                    resource = null;
                }
            }

            if (resource != null) {
                pathConfig.setId(resource.getId());
                // if the resource is statically bound to a resource it means the config can not be invalidated
                if (resourceName != null) {
                    pathConfig.setStatic(true);
                }
            }

            if (PolicyEnforcerConfig.EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                pathConfig.setStatic(true);
            }

            PathConfig existingPath = null;

            for (PathConfig current : paths.values()) {
                if (current.getPath().equals(pathConfig.getPath())) {
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
        LOGGER.info("Querying the server for all resources associated with this application.");
        Map<String, PathConfig> paths = Collections.synchronizedMap(new HashMap<String, PathConfig>());

        if (!enforcerConfig.getLazyLoadPaths()) {
            for (String id : protectedResource.findAll()) {
                ResourceRepresentation resourceDescription = protectedResource.findById(id);

                if (resourceDescription.getUris() != null && !resourceDescription.getUris().isEmpty()) {
                    for(PathConfig pathConfig : PathConfig.createPathConfigs(resourceDescription)) {
                        paths.put(pathConfig.getPath(), pathConfig);
                    }
                }
            }
        }

        return paths;
    }

    public static class PathConfigMatcher extends PathMatcher<PathConfig> {

        private final Map<String, PathConfig> paths;
        private final PathCache pathCache;
        private final AuthzClient authzClient;
        private final PolicyEnforcerConfig enforcerConfig;

        public PathConfigMatcher(Map<String, PathConfig> paths, PolicyEnforcerConfig enforcerConfig, AuthzClient authzClient) {
            this.paths = paths;
            this.enforcerConfig = enforcerConfig;
            PathCacheConfig cacheConfig = enforcerConfig.getPathCacheConfig();

            if (cacheConfig == null) {
                cacheConfig = new PathCacheConfig();
            }

            pathCache = new PathCache(cacheConfig.getMaxEntries(), cacheConfig.getLifespan(), paths);
            this.authzClient = authzClient;
        }

        @Override
        public PathConfig matches(String targetUri) {
            PathConfig pathConfig = pathCache.get(targetUri);

            if (pathCache.containsKey(targetUri) || pathConfig != null) {
                return pathConfig;
            }

            pathConfig = super.matches(targetUri);

            if (enforcerConfig.getLazyLoadPaths() || enforcerConfig.getPathCacheConfig() != null) {
                if ((pathConfig == null || pathConfig.isInvalidated() || pathConfig.getPath().contains("*"))) {
                    try {
                        List<ResourceRepresentation> matchingResources = authzClient.protection().resource().findByMatchingUri(targetUri);

                        if (matchingResources.isEmpty()) {
                            // if this config is invalidated (e.g.: due to cache expiration) we remove and return null
                            if (pathConfig != null && pathConfig.isInvalidated()) {
                                paths.remove(targetUri);
                                return null;
                            }
                        } else {
                            Map<String, Map<String, Object>> cipConfig = null;
                            PolicyEnforcerConfig.EnforcementMode enforcementMode = PolicyEnforcerConfig.EnforcementMode.ENFORCING;
                            ResourceRepresentation targetResource = matchingResources.get(0);
                            List<PolicyEnforcerConfig.MethodConfig> methodConfig = null;
                            boolean isStatic = false;

                            if (pathConfig != null) {
                                cipConfig = pathConfig.getClaimInformationPointConfig();
                                enforcementMode = pathConfig.getEnforcementMode();
                                methodConfig = pathConfig.getMethods();
                                isStatic = pathConfig.isStatic();
                            } else {
                                for (PathConfig existingPath : paths.values()) {
                                    if (targetResource.getId().equals(existingPath.getId())
                                            && existingPath.isStatic()
                                            && !PolicyEnforcerConfig.EnforcementMode.DISABLED.equals(existingPath.getEnforcementMode())) {
                                        return null;
                                    }
                                }
                            }

                            pathConfig = PathConfig.createPathConfigs(targetResource).iterator().next();

                            if (cipConfig != null) {
                                pathConfig.setClaimInformationPointConfig(cipConfig);
                            }

                            if (methodConfig != null) {
                                pathConfig.setMethods(methodConfig);
                            }

                            pathConfig.setStatic(isStatic);
                            pathConfig.setEnforcementMode(enforcementMode);
                        }
                    } catch (Exception cause) {
                        LOGGER.errorf(cause, "Could not lazy load resource with path [" + targetUri + "] from server");
                        return null;
                    }
                }
            }

            pathCache.put(targetUri, pathConfig);

            return pathConfig;
        }

        @Override
        protected String getPath(PathConfig entry) {
            return entry.getPath();
        }

        @Override
        protected Collection<PathConfig> getPaths() {
            return paths.values();
        }

        public PathCache getPathCache() {
            return pathCache;
        }

        @Override
        protected PathConfig resolvePathConfig(PathConfig originalConfig, String path) {
            if (originalConfig.hasPattern()) {
                ProtectedResource resource = authzClient.protection().resource();

                // search by an exact match
                List<ResourceRepresentation> search = resource.findByUri(path);

                // if exact match not found, try to obtain from current path the parent path.
                // if path is /resource/1/test and pattern from pathConfig is /resource/{id}/*, parent path is /resource/1
                // this logic allows to match sub resources of a resource instance (/resource/1) to the parent resource,
                // so any permission granted to parent also applies to sub resources
                if (search.isEmpty()) {
                    search = resource.findByUri(buildUriFromTemplate(originalConfig.getPath(), path, true));
                }

                if (!search.isEmpty()) {
                    ResourceRepresentation targetResource = search.get(0);
                    PathConfig config = PathConfig.createPathConfigs(targetResource).iterator().next();

                    config.setScopes(originalConfig.getScopes());
                    config.setMethods(originalConfig.getMethods());
                    config.setParentConfig(originalConfig);
                    config.setEnforcementMode(originalConfig.getEnforcementMode());
                    config.setClaimInformationPointConfig(originalConfig.getClaimInformationPointConfig());

                    return config;
                }
            }

            return null;
        }

        public void removeFromCache(String pathConfig) {
            pathCache.remove(pathConfig);
        }
    };
}
