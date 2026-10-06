package org.keycloak.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.representations.workflows.MultivaluedHashMapValueDeserializer;
import org.keycloak.representations.workflows.MultivaluedHashMapValueSerializer;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Marks a {@code MultivaluedHashMap<String, String>} field with compact serialization:
 * single-valued entries become plain values, boolean-looking strings become JSON booleans.
 * Equivalent to {@code @JsonSerialize(using = MultivaluedHashMapValueSerializer.class)}
 * and {@code @JsonDeserialize(using = MultivaluedHashMapValueDeserializer.class)}.
 * <p>
 * With Jackson 2, the meta-annotation expands automatically via {@link JacksonAnnotationsInside}.
 * With Jackson 3, the {@code KeycloakAnnotationIntrospector3} handles this annotation directly.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = MultivaluedHashMapValueSerializer.class)
@JsonDeserialize(using = MultivaluedHashMapValueDeserializer.class)
public @interface MultivaluedHashMapValue {
}
