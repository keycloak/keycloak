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
package org.keycloak.subsystem.server.extension;

/**
 * This service keeps track of the entire Keycloak management model so as to provide
 * adapter configuration to each deployment at deploy time.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public final class KeycloakAdapterConfigService {

    static final KeycloakAdapterConfigService INSTANCE = new KeycloakAdapterConfigService();

    static final String DEPLOYMENT_NAME = "keycloak-server.war";

    private String webContext;


    private KeycloakAdapterConfigService() {
    }

    void setWebContext(String webContext) {
        this.webContext = webContext;
    }

    String getWebContext() {
        return webContext;
    }

    boolean isKeycloakServerDeployment(String deploymentName) {
        return DEPLOYMENT_NAME.equals(deploymentName);
    }
}
