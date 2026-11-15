package org.keycloak.quarkus.runtime.integration.jaxrs;

import jakarta.inject.Singleton;

import org.keycloak.json.jackson3.KeycloakAnnotationIntrospector3;
import org.keycloak.ssf.subject.SubjectId;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Applies Keycloak's REST representation configuration to the mapper managed by Quarkus.
 */
@Singleton
public class KeycloakObjectMapperCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Workflow steps expose immutable properties through their getters.
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .annotationIntrospector(new KeycloakAnnotationIntrospector3())
                .addModule(new SimpleModule().addDeserializer(SubjectId.class, new SubjectIdDeserializer()))
                .changeDefaultPropertyInclusion(inclusion -> JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
        if (Boolean.getBoolean("keycloak.jsonPrettyPrint")) {
            builder.enable(SerializationFeature.INDENT_OUTPUT);
        }
        if (Boolean.parseBoolean(System.getProperty("keycloak.jsonEnableJacksonModuleDiscovery", "true"))) {
            builder.findAndAddModules();
        }
    }
}
