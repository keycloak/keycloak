package org.keycloak.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Marks a {@code Map<String, List<String>>} field where JSON values can be either
 * a single string or an array of strings.
 * Equivalent to {@code @JsonDeserialize(using = StringListMapDeserializer.class)}.
 * <p>
 * With Jackson 2, the meta-annotation expands automatically via {@link JacksonAnnotationsInside}.
 * With Jackson 3, the {@code KeycloakAnnotationIntrospector3} handles this annotation directly.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = StringListMapDeserializer.class)
public @interface StringListMap {
}
