/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectIdJsonDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Provider
@ApplicationScoped
public class QuarkusObjectMapperResolver implements ContextResolver<ObjectMapper> {

    private final com.fasterxml.jackson.databind.ObjectMapper legacyMapper = new ObjectMapperResolver().getContext(Object.class);
    private final ObjectMapper mapper = createMapper(legacyMapper);

    @Produces
    public com.fasterxml.jackson.databind.ObjectMapper getObjectMapper() {
        return legacyMapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

    private static ObjectMapper createMapper(com.fasterxml.jackson.databind.ObjectMapper legacyMapper) {
        var subjectMapper = legacyMapper.copy().registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
                .addDeserializer(SubjectId.class, new SubjectIdJsonDeserializer()));
        var builder = JsonMapper.builder()
                .configureForJackson2()
                .annotationIntrospector(new LegacyNamingAnnotationIntrospector())
                .addModule(new LegacyJsonNodeAdapter(legacyMapper.getNodeFactory()))
                .addModule(new SimpleModule().addDeserializer(SubjectId.class, new ValueDeserializer<SubjectId>() {
                    @Override
                    public SubjectId deserialize(JsonParser parser, DeserializationContext context) {
                        try {
                            var tree = LegacyJsonNodeAdapter.toLegacy(context.readTree(parser), subjectMapper.getNodeFactory());
                            try (var subjectParser = tree.traverse(subjectMapper)) {
                                return subjectMapper.readValue(subjectParser, SubjectId.class);
                            }
                        } catch (IOException exception) {
                            return context.reportInputMismatch(SubjectId.class, "Unable to read the subject identifier");
                        }
                    }
                }))
                .changeDefaultPropertyInclusion(inclusion -> JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
                .configure(SerializationFeature.INDENT_OUTPUT, Boolean.getBoolean("keycloak.jsonPrettyPrint"));
        if (Boolean.parseBoolean(System.getProperty("keycloak.jsonEnableJacksonModuleDiscovery", "true"))) {
            builder.findAndAddModules();
        }
        return builder.build();
    }
}
