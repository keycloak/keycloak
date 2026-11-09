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

package org.keycloak.quarkus.runtime.integration.jaxrs;

import jakarta.inject.Singleton;

import org.keycloak.services.DefaultKeycloakSessionFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * Applies the Keycloak specific configuration to the {@link ObjectMapper} managed by Quarkus.
 * <p>
 * Using the Quarkus managed mapper allows {@code quarkus.jackson.*} properties to be used to work around
 * serialization or deserialization issues. Settings that change the shape of the JSON, such as
 * {@code quarkus.jackson.property-naming-strategy}, are not expected to be used. The REST API contract, including the
 * generated OpenAPI documentation, is defined with the default camel case property names, so changing them breaks
 * clients.
 */
@Singleton
public class KeycloakObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        // restore the plain Jackson defaults that Quarkus changes
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        DefaultKeycloakSessionFactory.configureObjectMapper(mapper);
    }

}
