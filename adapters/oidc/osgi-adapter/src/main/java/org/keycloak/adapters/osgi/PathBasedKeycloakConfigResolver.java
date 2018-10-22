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
package org.keycloak.adapters.osgi;

import org.jboss.logging.Logger;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathBasedKeycloakConfigResolver implements KeycloakConfigResolver {

    protected static final Logger log = Logger.getLogger(PathBasedKeycloakConfigResolver.class);

    private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<String, KeycloakDeployment>();

    private File keycloakConfigLocation = null;

    public PathBasedKeycloakConfigResolver() {
        String location = null;
        String keycloakConfig = (String) System.getProperties().get("keycloak.config");
        if (keycloakConfig != null && !"".equals(keycloakConfig.trim())) {
            location = keycloakConfig;
        } else {
            String karafEtc = (String) System.getProperties().get("karaf.etc");
            if (karafEtc != null && !"".equals(karafEtc.trim())) {
                location = karafEtc;
            }
        }
        if (location != null) {
            File loc = new File(location);
            if (loc.isDirectory()) {
                keycloakConfigLocation = loc;
            }
        }
    }

    @Override
    public KeycloakDeployment resolve(OIDCHttpFacade.Request request) {
        String webContext = getDeploymentKeyForURI(request);

        return getOrCreateDeployment(webContext);
    }

    /**
     * {@code pathFragment} is a key for {@link KeycloakDeployment deployments}. The key is used to construct
     * a path relative to {@code keycloak.config} or {@code karaf.etc} system properties.
     * For given key, {@code <key>-keycloak.json} file is checked.
     * @param pathFragment
     * @return
     */
    protected synchronized KeycloakDeployment getOrCreateDeployment(String pathFragment) {
        KeycloakDeployment deployment = getCachedDeployment(pathFragment);
        if (null == deployment) {
            // not found on the simple cache, try to load it from the file system
            if (keycloakConfigLocation == null) {
                throw new IllegalStateException("Neither \"keycloak.config\" nor \"karaf.etc\" java properties are set." +
                        " Please set one of them.");
            }

            File configuration = new File(keycloakConfigLocation, pathFragment + ("".equals(pathFragment) ? "" : "-")
                    + "keycloak.json");
            if (!cacheConfiguration(pathFragment, configuration)) {
                throw new IllegalStateException("Not able to read the file " + configuration);
            }
            deployment = getCachedDeployment(pathFragment);
        }

        return deployment;
    }

    protected synchronized KeycloakDeployment getCachedDeployment(String pathFragment) {
        return cache.get(pathFragment);
    }

    /**
     * If there's a need, we can pre populate the cache of deployments.
     */
    protected void prepopulateCache() {
        if (keycloakConfigLocation == null || !keycloakConfigLocation.isDirectory()) {
            log.warn("Can't cache Keycloak configurations. No configuration storage is accessible." +
                    " Please set either \"keycloak.config\" or \"karaf.etc\" system properties");
            return;
        }

        File[] configs = keycloakConfigLocation.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith("keycloak.json");
            }
        });
        if (configs != null) {
            for (File config: configs) {
                String pathFragment = null;
                if ("keycloak.json".equals(config.getName())) {
                    pathFragment = "";
                } else if (config.getName().endsWith("-keycloak.json")) {
                    pathFragment = config.getName()
                            .substring(0, config.getName().length() - "-keycloak.json".length());
                }
                cacheConfiguration(pathFragment, config);
            }
        }
    }

    private boolean cacheConfiguration(String key, File config) {
        try {
            InputStream is = new FileInputStream(config);
            KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
            cache.put(key, deployment);
            return true;
        } catch (FileNotFoundException | RuntimeException e) {
            log.warn("Can't cache " + config + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Finds a context path from given {@link HttpFacade.Request}. For default context, first path segment
     * is returned.
     * @param request
     * @return
     */
    private String getDeploymentKeyForURI(HttpFacade.Request request) {
        String uri = request.getURI();
        String relativePath = request.getRelativePath();
        String webContext = null;
        if (relativePath == null || !uri.contains(relativePath)) {
            String[] urlTokens = uri.split("/");
            if (urlTokens.length <  4) {
                throw new IllegalStateException("Not able to determine the web-context to load the correspondent keycloak.json file");
            }

            webContext = urlTokens[3];
        } else {
            URI parsedURI = URI.create(uri);
            String path = parsedURI.getPath();
            if (path.contains(relativePath)) {
                path = path.substring(0, path.indexOf(relativePath));
            }
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            webContext = path;
            if ("".equals(webContext)) {
                path = relativePath;
                while (path.startsWith("/")) {
                    path = path.substring(1);
                }
                if (path.contains("/")) {
                    path = path.substring(0, path.indexOf("/"));
                }
                webContext = path;
            }
        }

        return webContext;
    }

}
