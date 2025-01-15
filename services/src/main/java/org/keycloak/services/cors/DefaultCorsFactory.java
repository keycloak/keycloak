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
import java.util.List;
import org.keycloak.Config;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public class DefaultCorsFactory implements CorsFactory {

    private static final String PROVIDER_ID = "default";
    private static final String HEADERS = "headers";

    private String defaultAllowHeaders = Cors.DEFAULT_ALLOW_HEADERS;

    @Override
    public Cors create(KeycloakSession session) {
        return new DefaultCors(session, defaultAllowHeaders);
    }

    @Override
    public void init(Config.Scope config) {
        String[] configHeaders = config.getArray(HEADERS);
        if (configHeaders != null && configHeaders.length > 0) {
            List<String> headers = Arrays.asList(configHeaders);
                defaultAllowHeaders = String.format("%s, %s", defaultAllowHeaders, CollectionUtil.join(headers));
        }
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

}
