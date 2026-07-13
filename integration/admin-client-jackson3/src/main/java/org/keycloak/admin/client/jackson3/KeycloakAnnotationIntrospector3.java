package org.keycloak.admin.client.jackson3;

import java.io.Serial;

import org.keycloak.json.MultivaluedHashMapValue;
import org.keycloak.json.StringListMap;
import org.keycloak.json.StringOrArray;

import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Jackson 3 annotation introspector that handles Keycloak custom annotations.
 * With Jackson 2, these annotations auto-expand via {@code @JacksonAnnotationsInside}.
 * Jackson 3 cannot expand the inner {@code @JsonDeserialize} (wrong package),
 * so this introspector maps them to the Jackson 3 equivalents directly.
 */
class KeycloakAnnotationIntrospector3 extends JacksonAnnotationIntrospector {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Object findDeserializer(MapperConfig<?> config, Annotated am) {
        if (am.hasAnnotation(StringOrArray.class)) {
            return StringOrArrayDeserializer3.class;
        }
        if (am.hasAnnotation(StringListMap.class)) {
            return StringListMapDeserializer3.class;
        }
        if (am.hasAnnotation(MultivaluedHashMapValue.class)) {
            return MultivaluedHashMapValueDeserializer3.class;
        }
        return super.findDeserializer(config, am);
    }

    @Override
    public Object findSerializer(MapperConfig<?> config, Annotated am) {
        if (am.hasAnnotation(StringOrArray.class)) {
            return StringOrArraySerializer3.class;
        }
        if (am.hasAnnotation(MultivaluedHashMapValue.class)) {
            return MultivaluedHashMapValueSerializer3.class;
        }
        return super.findSerializer(config, am);
    }
}
