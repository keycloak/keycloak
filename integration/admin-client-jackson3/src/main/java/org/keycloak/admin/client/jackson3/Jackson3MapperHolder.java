package org.keycloak.admin.client.jackson3;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class Jackson3MapperHolder {

    static final JsonMapper MAPPER;

    static {
        JsonMapper defaultMapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(v -> JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        MAPPER = defaultMapper.rebuild()
                .annotationIntrospector(AnnotationIntrospector.pair(
                        new KeycloakAnnotationIntrospector3(),
                        defaultMapper.serializationConfig().getAnnotationIntrospector()))
                .build();
    }

    private Jackson3MapperHolder() {
    }
}
