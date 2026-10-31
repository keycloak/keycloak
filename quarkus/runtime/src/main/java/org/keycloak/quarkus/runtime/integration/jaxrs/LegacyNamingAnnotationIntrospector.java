package org.keycloak.quarkus.runtime.integration.jaxrs;

import java.io.Serial;

import org.keycloak.json.jackson3.KeycloakAnnotationIntrospector3;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;

/** Server compatibility for providers still using Jackson 2 naming annotations. */
final class LegacyNamingAnnotationIntrospector extends KeycloakAnnotationIntrospector3 {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Object findNamingStrategy(MapperConfig<?> config, AnnotatedClass annotated) {
        Object strategy = super.findNamingStrategy(config, annotated);
        if (strategy != null) {
            return strategy;
        }
        JsonNaming naming = annotated.getAnnotation(JsonNaming.class);
        if (naming != null && naming.value() == com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class) {
            return PropertyNamingStrategies.SNAKE_CASE;
        }
        return null;
    }
}
