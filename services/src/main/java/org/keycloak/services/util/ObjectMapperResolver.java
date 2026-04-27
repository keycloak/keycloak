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

package org.keycloak.services.util;

import java.util.stream.Stream;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.StreamSerializer;

/**
 * Any class with package org.jboss.resteasy.skeleton.key will use NON_DEFAULT inclusion
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {
    protected ObjectMapper mapper;

    public ObjectMapperResolver() {
        mapper = ObjectMapperInitializer.OBJECT_MAPPER;
    }

    public static ObjectMapper createStreamSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = TypeFactory.unknownType();
        JavaType streamType = mapper.getTypeFactory().constructParametricType(Stream.class, type);

        SimpleModule module = new SimpleModule();
        module.addSerializer(new StreamSerializer(streamType, type));
        mapper.registerModule(module);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (Boolean.parseBoolean(System.getProperty("keycloak.jsonPrettyPrint", "false"))) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        // allow to discover jackson mappers on the classpath
        if (Boolean.parseBoolean(System.getProperty("keycloak.jsonEnableJacksonModuleDiscovery", "true"))) {
            mapper.findAndRegisterModules();
        }

        return mapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

    private static class ObjectMapperInitializer {

        private static final ObjectMapper OBJECT_MAPPER = createStreamSerializer();
    }
}
