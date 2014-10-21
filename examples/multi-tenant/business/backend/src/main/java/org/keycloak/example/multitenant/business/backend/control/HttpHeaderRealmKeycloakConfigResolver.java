/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.example.multitenant.business.backend.control;

import java.io.InputStream;
import org.keycloak.adapters.HttpFacade.Request;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public class HttpHeaderRealmKeycloakConfigResolver implements KeycloakConfigResolver {

    Logger logger = LoggerFactory.getLogger(HttpHeaderRealmKeycloakConfigResolver.class);

    @Override
    public KeycloakDeployment resolve(Request request) {
        String realm = request.getHeader("X-Keycloak-Realm");
        InputStream is = getClass().getResourceAsStream("/keycloak-"+realm+".json");

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
        return deployment;
    }
}
