/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.cors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public class DefaultCorsFactory implements CorsFactory {

    private static final String PROVIDER_ID = "default";
    private String allowedHeaders;

    @Override
    public Cors create(KeycloakSession session) {
        return new DefaultCors(session, allowedHeaders);
    }

    @Override
    public void init(Config.Scope config) {
        Set<String> allowedHeaders = new HashSet<>(Cors.DEFAULT_ALLOW_HEADERS);

        String[] customAllowedHeaders = config.getArray("allowedHeaders");
        if (customAllowedHeaders != null) {
            allowedHeaders.addAll(Arrays.asList(customAllowedHeaders));
        }

        this.allowedHeaders = String.join(", ", allowedHeaders);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("allowedHeaders")
                .type("string")
                .helpText("A comma-separated list of additional allowed headers for CORS requests")
                .defaultValue(false)
                .add()
                .build();
    }
}
