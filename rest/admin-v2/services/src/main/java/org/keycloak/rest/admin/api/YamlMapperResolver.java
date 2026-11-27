package org.keycloak.rest.admin.api;

import java.util.stream.Stream;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.admin.api.PatchTypeNames;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jdk8.StreamSerializer;

/**
 * Provides the {@link YAMLMapper} used to serialize and deserialize {@code application/yaml} payloads.
 * It is configured like the JSON mapper, for example to support {@link Stream} results and omit null values.
 */
@Provider
@Produces({PatchTypeNames.APPLICATION_YAML, PatchTypeNames.YAML_MERGE})
public class YamlMapperResolver implements ContextResolver<YAMLMapper> {

    private static final YAMLMapper MAPPER = createMapper();

    private static YAMLMapper createMapper() {
        YAMLMapper mapper = new YAMLMapper();
        JavaType type = TypeFactory.unknownType();
        JavaType streamType = mapper.getTypeFactory().constructParametricType(Stream.class, type);

        SimpleModule module = new SimpleModule();
        module.addSerializer(new StreamSerializer(streamType, type));
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Override
    public YAMLMapper getContext(Class<?> type) {
        return MAPPER;
    }
}
