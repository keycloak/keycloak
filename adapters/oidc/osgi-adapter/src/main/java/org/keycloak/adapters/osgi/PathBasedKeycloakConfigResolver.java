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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;

public class PathBasedKeycloakConfigResolver implements KeycloakConfigResolver {

    private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<String, KeycloakDeployment>();

    @Override
    public KeycloakDeployment resolve(OIDCHttpFacade.Request request) {
        String path = request.getURI();
        String[] urlTokens = path.split("/");
        if (urlTokens.length <  4) {
            throw new IllegalStateException("Not able to determine the web-context to load the correspondent keycloak.json file");
        }

        String webContext = urlTokens[3];

        KeycloakDeployment deployment = cache.get(webContext);
        if (null == deployment) {
            // not found on the simple cache, try to load it from the file system
            String keycloakConfig = (String) System.getProperties().get("keycloak.config");
            if(keycloakConfig == null || "".equals(keycloakConfig.trim())){
                String karafEtc = (String) System.getProperties().get("karaf.etc");
                if(karafEtc == null || "".equals(karafEtc.trim())){
                    throw new IllegalStateException("Neither \"keycloak.config\" nor \"karaf.etc\" java properties are set. Please set one of them.");
                }
                keycloakConfig = karafEtc;
            }

            String absolutePath = keycloakConfig + File.separator + webContext + "-keycloak.json";
            InputStream is = null;
            try {
                is = new FileInputStream(absolutePath);
            } catch (FileNotFoundException e){
                throw new IllegalStateException("Not able to find the file " + absolutePath);
            }
            deployment = KeycloakDeploymentBuilder.build(is);
            cache.put(webContext, deployment);
        }

        return deployment;
    }

}
