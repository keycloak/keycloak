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

package org.keycloak.servlet;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletOAuthClientBuilder {

    public static ServletOAuthClient build(InputStream is) {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
        ServletOAuthClient client = new ServletOAuthClient();
        client.setDeployment(deployment);
        return client;
    }

    public static ServletOAuthClient build(AdapterConfig adapterConfig) {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(adapterConfig);
        ServletOAuthClient client = new ServletOAuthClient();
        client.setDeployment(deployment);
        return client;
    }

    public static void build(InputStream is, ServletOAuthClient oauthClient) {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
        oauthClient.setDeployment(deployment);
    }
}
