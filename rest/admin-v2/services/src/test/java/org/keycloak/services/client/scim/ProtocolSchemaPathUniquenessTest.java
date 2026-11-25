package org.keycloak.services.client.scim;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Projection, sorting and querying resolve a field path by matching against the polymorphic client
 * representations. To avoid ambiguity when projecting or querying across different client protocols,
 * sibling subtypes must not declare colliding property names, and the provider schema registry
 * must be consistent with the registered subtypes.
 */
class ProtocolSchemaPathUniquenessTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void schemasMatchJsonSubTypes() {
        JsonSubTypes subTypes = BaseClientRepresentation.class.getAnnotation(JsonSubTypes.class);
        assertNotNull(subTypes, "BaseClientRepresentation must be annotated with @JsonSubTypes");

        Map<String, Class<?>> subTypeClasses = new TreeMap<>();
        for (JsonSubTypes.Type type : subTypes.value()) {
            Class<?> previous = subTypeClasses.put(type.name(), type.value());
            assertNull(previous, () -> "Duplicate @JsonSubTypes name '" + type.name() + "' declared by both '"
                    + previous.getSimpleName() + "' and '" + type.value().getSimpleName() + "'");
        }

        Map<String, Class<?>> schemaClasses = ClientResourceTypeProvider.SCHEMAS.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().createRepresentation().getClass(),
                        (a, b) -> a,
                        TreeMap::new));

        assertEquals(subTypeClasses, schemaClasses,
                "ClientResourceTypeProvider.SCHEMAS must match @JsonSubTypes on BaseClientRepresentation");
    }

    @Test
    void polymorphicClientSubtypesDoNotSharePropertyNames() {
        JsonSubTypes subTypes = BaseClientRepresentation.class.getAnnotation(JsonSubTypes.class);
        assertNotNull(subTypes, "BaseClientRepresentation must be annotated with @JsonSubTypes");

        Set<String> baseProperties = getPropertyNames(BaseClientRepresentation.class);
        Map<String, String> propertyOwners = new TreeMap<>();
        Set<String> duplicates = new TreeSet<>();

        for (JsonSubTypes.Type type : subTypes.value()) {
            Class<?> subTypeClass = type.value();
            Set<String> subProperties = getPropertyNames(subTypeClass);
            subProperties.removeAll(baseProperties);

            for (String prop : subProperties) {
                String existingOwner = propertyOwners.put(prop, subTypeClass.getSimpleName());
                if (existingOwner != null) {
                    duplicates.add("Property '" + prop + "' is declared by both '" + existingOwner + "' and '" + subTypeClass.getSimpleName() + "'");
                }
            }
        }

        assertEquals(Set.of(), duplicates,
                "Field paths must be unique across polymorphic client representations; declare shared fields in BaseClientRepresentation instead");
    }

    private static Set<String> getPropertyNames(Class<?> clazz) {
        JavaType javaType = MAPPER.constructType(clazz);
        BeanDescription beanDesc = MAPPER.getSerializationConfig().introspect(javaType);
        return beanDesc.findProperties().stream()
                .map(BeanPropertyDefinition::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
