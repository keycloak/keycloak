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
package org.keycloak.adapters;

import org.junit.Test;
import org.keycloak.representations.adapters.config.AdapterConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:brad.culley@spartasystems.com">Brad Culley</a>
 * @author <a href="mailto:john.ament@spartasystems.com">John D. Ament</a>
 */
public class KeycloakDeploymentTest {
    @Test
    public void shouldNotEnableOAuthQueryParamWhenIgnoreIsTrue() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeployment();
        keycloakDeployment.setIgnoreOAuthQueryParameter(true);
        assertFalse(keycloakDeployment.isOAuthQueryParameterEnabled());
    }

    @Test
    public void shouldEnableOAuthQueryParamWhenIgnoreIsFalse() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeployment();
        keycloakDeployment.setIgnoreOAuthQueryParameter(false);
        assertTrue(keycloakDeployment.isOAuthQueryParameterEnabled());
    }

    @Test
    public void shouldEnableOAuthQueryParamWhenIgnoreNotSet() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeployment();

        assertTrue(keycloakDeployment.isOAuthQueryParameterEnabled());
    }

    @Test
    public void stripDefaultPorts() {
        KeycloakDeployment keycloakDeployment = new KeycloakDeployment();
        keycloakDeployment.setRealm("test");
        AdapterConfig config = new AdapterConfig();
        config.setAuthServerUrl("http://localhost:80/auth");
        keycloakDeployment.setAuthServerBaseUrl(config);

        assertEquals("http://localhost/auth", keycloakDeployment.getAuthServerBaseUrl());

        config.setAuthServerUrl("https://localhost:443/auth");
        keycloakDeployment.setAuthServerBaseUrl(config);

        assertEquals("https://localhost/auth", keycloakDeployment.getAuthServerBaseUrl());
    }

}