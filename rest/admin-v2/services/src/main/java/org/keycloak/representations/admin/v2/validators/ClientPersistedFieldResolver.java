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
        return switch (fieldName) {
            case "uuid" -> client.getUuid();
            case "protocol" -> client.getProtocol();
            default -> throw new AssertionError("Unsupported field: " + fieldName);
        };
    }

    @Override
    public String getPersistedValue(ValidationContext context, Object representation, String fieldName) {
        BaseClientRepresentation client = (BaseClientRepresentation) representation;
        ClientModel persistedClient = context.realm().getClientByClientId(client.getClientId());
        if (persistedClient == null) {
            return null;
        }
        return switch (fieldName) {
            case "uuid" -> persistedClient.getId();
            case "protocol" -> persistedClient.getProtocol();
            default -> throw new AssertionError("Unsupported field: " + fieldName);
        };
    }

    @Override
    public boolean valueExists(ValidationContext context, String fieldName, String value) {
        return switch (fieldName) {
            case "uuid" -> Optional.ofNullable(context.realm().getClientById(value)).isPresent();
            default -> false;
        };
    }
}
