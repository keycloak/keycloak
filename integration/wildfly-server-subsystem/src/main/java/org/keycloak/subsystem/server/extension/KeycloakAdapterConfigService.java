/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.subsystem.server.extension;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

/**
 * This service keeps track of the entire Keycloak management model so as to provide
 * adapter configuration to each deployment at deploy time.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public final class KeycloakAdapterConfigService {

    private static final KeycloakAdapterConfigService INSTANCE = new KeycloakAdapterConfigService();

    public static KeycloakAdapterConfigService getInstance() {
        return INSTANCE;
    }

    // key=auth-server deployment name; value=web-context
    private final Map<String, String> webContexts = new HashMap<String, String>();



    private KeycloakAdapterConfigService() {
    }

    public void addServerDeployment(String deploymentName, String webContext) {
        this.webContexts.put(deploymentName, webContext);
    }

    public String getWebContext(String deploymentName) {
        return webContexts.get(deploymentName);
    }

    public void removeServerDeployment(String deploymentName) {
        this.webContexts.remove(deploymentName);
    }

    public boolean isWebContextUsed(String webContext) {
        return webContexts.containsValue(webContext);
    }

    public boolean isKeycloakServerDeployment(String deploymentName) {
        return this.webContexts.containsKey(deploymentName);
    }
}
