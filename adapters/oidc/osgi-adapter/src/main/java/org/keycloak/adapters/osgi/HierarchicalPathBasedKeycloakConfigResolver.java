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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;

/**
 * This {@link KeycloakConfigResolver} tries to resolve most specific configuration for given URI path. If not found,
 * <em>parent</em> path is checked up to top-level path.
 */
public class HierarchicalPathBasedKeycloakConfigResolver extends PathBasedKeycloakConfigResolver {

    protected static final Logger log = Logger.getLogger(HierarchicalPathBasedKeycloakConfigResolver.class);

    public HierarchicalPathBasedKeycloakConfigResolver() {
        prepopulateCache();
    }

    @Override
    public KeycloakDeployment resolve(OIDCHttpFacade.Request request) {
        // we cached all available deployments initially and now we'll try to check them from
        // most specific to most general
        URI uri = URI.create(request.getURI());
        String path = uri.getPath();
        if (path != null) {
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            String[] segments = path.split("/");
            List<String> paths = collectPaths(segments);
            for (String pathFragment: paths) {
                KeycloakDeployment cachedDeployment = super.getCachedDeployment(pathFragment);
                if (cachedDeployment != null) {
                    return cachedDeployment;
                }
            }
        }

        throw new IllegalStateException("Can't find Keycloak configuration related to URI path " + uri);
    }

    /**
     * <p>For segments like "a, b, c, d", returns:<ul>
     *     <li>"a-b-c-d"</li>
     *     <li>"a-b-c"</li>
     *     <li>"a-b"</li>
     *     <li>"a"</li>
     *     <li>""</li>
     * </ul></p>
     * @param segments
     * @return
     */
    private List<String> collectPaths(String[] segments) {
        List<String> result = new ArrayList<>(segments.length + 1);
        for (int idx = segments.length; idx >= 0; idx--) {
            StringBuilder sb = null;
            for (int i = 0; i < idx; i++) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append("-").append(segments[i]);
            }
            result.add(sb == null ? "" : sb.toString().substring(1));
        }
        return result;
    }

}
