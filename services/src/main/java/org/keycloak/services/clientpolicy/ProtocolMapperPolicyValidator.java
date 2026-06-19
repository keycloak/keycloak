/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.events.Errors;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.ServicesLogger;

/**
 * Validates protocol mapper types against an allowed whitelist.
 */
public final class ProtocolMapperPolicyValidator {

    public static final String PROTOCOL_MAPPER_TYPE_NOT_ALLOWED = "ProtocolMapper type not allowed";

    private ProtocolMapperPolicyValidator() {
    }

    public static void validateProtocolMappers(List<ProtocolMapperRepresentation> protocolMappers,
            List<String> allowedMapperProviders, ProtocolMapperContainerModel container) throws ClientPolicyException {
        if (protocolMappers == null || protocolMappers.isEmpty()) {
            return;
        }
        for (ProtocolMapperRepresentation mapperRepresentation : protocolMappers) {
            validateProtocolMapper(mapperRepresentation, allowedMapperProviders, container);
        }
    }

    public static void validateProtocolMapper(ProtocolMapperRepresentation mapperRepresentation,
            List<String> allowedMapperProviders, ProtocolMapperContainerModel container) throws ClientPolicyException {
        if (mapperRepresentation == null) {
            return;
        }

        String mapperType = mapperRepresentation.getProtocolMapper();
        if (allowedMapperProviders.contains(mapperType)) {
            return;
        }

        if (container == null) {
            failWithProtocolMapperTypeNotAllowed(mapperRepresentation);
        }

        String mapperRepresentationId = mapperRepresentation.getId();
        if (mapperRepresentationId == null) {
            failWithProtocolMapperTypeNotAllowed(mapperRepresentation);
        }

        ProtocolMapperModel mapperModel = container.getProtocolMapperById(mapperRepresentationId);
        if (mapperModel == null) {
            String message = "No existing mapper model found for id '%s'".formatted(mapperRepresentationId);
            ServicesLogger.LOGGER.warn(message);
            throw new ClientPolicyException(Errors.INVALID_REGISTRATION, message);
        }

        Map<String, String> modelConfig = mapperModel.getConfig();
        Map<String, String> representationConfig = mapperRepresentation.getConfig();
        if (!Objects.equals(representationConfig, modelConfig)) {
            failWithProtocolMapperTypeNotAllowed(mapperRepresentation);
        }
    }

    private static void failWithProtocolMapperTypeNotAllowed(ProtocolMapperRepresentation mapper) throws ClientPolicyException {
        ServicesLogger.LOGGER.clientRegistrationMapperNotAllowed(mapper.getName(), mapper.getProtocolMapper());
        throw new ClientPolicyException(Errors.INVALID_REGISTRATION, PROTOCOL_MAPPER_TYPE_NOT_ALLOWED);
    }
}
