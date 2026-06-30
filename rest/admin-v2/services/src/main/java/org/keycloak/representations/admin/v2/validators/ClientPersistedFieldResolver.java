package org.keycloak.representations.admin.v2.validators;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.models.ClientModel;
import org.keycloak.models.utils.reflection.NamedPropertyCriteria;
import org.keycloak.models.utils.reflection.Property;
import org.keycloak.models.utils.reflection.PropertyQueries;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Resolves persisted field values for clients.
 */
public class ClientPersistedFieldResolver implements PersistedFieldResolver {

    private static final Map<String, String> GETTER_PROPERTY_ALIASES = Map.of(
            "uuid", "id",
            "updatedTimestamp", "lastModifiedTimestamp",
            "displayName", "name",
            "appUrl", "baseUrl");

    @Override
    public boolean supports(Class<?> representationType) {
        return BaseClientRepresentation.class.isAssignableFrom(representationType);
    }

    @Override
    public String getProvidedValue(Object representation, String fieldName) {
        BaseClientRepresentation client = (BaseClientRepresentation) representation;
        return fieldValue(fieldName, client);
    }

    @Override
    public String getPersistedValue(ValidationContext context, Object representation, String fieldName) {
        BaseClientRepresentation client = (BaseClientRepresentation) representation;
        ClientModel persistedClient = context.realm().getClientByClientId(client.getClientId());
        if (persistedClient == null) {
            return null;
        }
        return fieldValue(fieldName, persistedClient);
    }

    @Override
    public boolean valueExists(ValidationContext context, String fieldName, String value) {
        if (Objects.equals(fieldName, "uuid")) {
            return Optional.ofNullable(context.realm().getClientById(value)).isPresent();
        }
        return false;
    }

    private String fieldValue(String fieldName, Object target) throws AssertionError {
        for (String propertyName : getterPropertyNames(fieldName)) {
            Property<Object> property = PropertyQueries.createQuery(target.getClass())
                    .addCriteria(new NamedPropertyCriteria(propertyName))
                    .getFirstResult();
            if (property != null) {
                return toStringValue(property.getValue(target));
            }
        }

        Field field = Reflections.findDeclaredField(target.getClass(), fieldName);
        if (field != null) {
            return toStringValue(Reflections.getFieldValue(field, target));
        }

        throw new AssertionError("Unsupported field:" + fieldName);
    }

    private static List<String> getterPropertyNames(String fieldName) {
        String alias = GETTER_PROPERTY_ALIASES.get(fieldName);
        if (alias == null) {
            return List.of(fieldName);
        }
        return List.of(fieldName, alias);
    }

    private static String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
