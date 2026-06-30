package org.keycloak.representations.admin.v2.validators;

import java.util.Optional;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class ClientPersistedFieldResolver implements PersistedFieldResolver {

    @Override
    public boolean supports(Class<?> representationType) {
        return BaseClientRepresentation.class.isAssignableFrom(representationType);
    }

    @Override
    public String getProvidedValue(Object representation, String fieldName) {
        BaseClientRepresentation client = (BaseClientRepresentation) representation;
        return fieldValue(fieldName, client, BaseClientRepresentation.class);
    }


    @Override
    public String getPersistedValue(ValidationContext context, Object representation, String fieldName) {
        BaseClientRepresentation client = (BaseClientRepresentation) representation;
        ClientModel persistedClient = context.realm().getClientByClientId(client.getClientId());
        if (persistedClient == null) {
            return null;
        }
        return fieldValue(fieldName, persistedClient, ClientModel.class);
    }

    @Override
    public boolean valueExists(ValidationContext context, String fieldName, String value) {
        return switch (fieldName) {
            case "uuid" -> Optional.ofNullable(context.realm().getClientById(value)).isPresent();
            default -> false;
        };
    }

    private String fieldValue(String fieldName, Object client, Class<?> clazz) throws AssertionError {
        try {
            var accessor = clazz.getDeclaredField(fieldName);
            accessor.setAccessible(true);
            return (String) accessor.get(client);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Unsupported field:" + fieldName, e);
        }
    }
}
