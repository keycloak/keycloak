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
package org.keycloak.models.utils;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperConfigException;

/**
 * Validates the protocol mappers of a container on the paths that write a whole container at
 * once, such as a client scope created inline or a realm import, which do not go through the per-mapper admin
 * endpoint and so never reach {@link ProtocolMapper#validateConfig}.
 */
public class ProtocolMapperValidationUtil {

    private ProtocolMapperValidationUtil() {
        // utility class
    }

    public static void validateProtocolMappers(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel container) throws ProtocolMapperConfigException {
        for (ProtocolMapperModel model : container.getProtocolMappersStream().toList()) {
            validateProtocolMapper(session, realm, container, model);
        }
    }

    public static void validateProtocolMapper(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel container,
                                             ProtocolMapperModel model) throws ProtocolMapperConfigException {
        ProtocolMapper mapper = (ProtocolMapper) session.getKeycloakSessionFactory()
                .getProviderFactory(ProtocolMapper.class, model.getProtocolMapper());
        // An import must not fail just because a third-party mapper provider is absent.
        if (mapper == null) {
            return;
        }

        // A mapper may carry no configuration at all, and RepresentationToModel preserves that as null. Validators
        // read the configuration without expecting that, so normalize it and let the mapper report its own error
        // instead of throwing a NullPointerException, which the admin endpoints would report as a 500.
        if (model.getConfig() == null) {
            model.setConfig(new HashMap<>());
        }

        Map<String, String> originalConfig = new HashMap<>(model.getConfig());
        mapper.validateConfig(session, realm, container, model);
        // Validators can initialize configuration, for example the pairwise subject mapper's salt.
        // Models read from the container are detached, so these changes must be saved explicitly.
        if (!originalConfig.equals(model.getConfig())) {
            container.updateProtocolMapper(model);
        }
    }
}
