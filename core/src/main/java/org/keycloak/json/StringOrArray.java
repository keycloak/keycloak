package org.keycloak.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Marks a {@code String[]} field that can be either a single JSON string or an array of strings.
 * Equivalent to {@code @JsonSerialize(using = StringOrArraySerializer.class)}
 * and {@code @JsonDeserialize(using = StringOrArrayDeserializer.class)}.
 * <p>
 * With Jackson 2, the meta-annotation expands automatically via {@link JacksonAnnotationsInside}.
 * With Jackson 3, the {@code KeycloakAnnotationIntrospector3} handles this annotation directly.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = StringOrArraySerializer.class)
@JsonDeserialize(using = StringOrArrayDeserializer.class)
public @interface StringOrArray {
}
