package org.keycloak.representations.admin.v2.validators;

import java.util.Objects;
import java.util.Optional;

import org.keycloak.models.ClientModel;
import org.keycloak.models.mapper.ClientModelMappers;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Resolves persisted field values for clients.
 */
public class ClientPersistedFieldResolver implements PersistedFieldResolver {

    private static final ClientModelMappers MAPPERS = new ClientModelMappers();

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
        var converted = MAPPERS.getMapper(client.getProtocol()).get().fromModel(persistedClient);
        return fieldValue(fieldName, converted);
    }

    @Override
    public boolean valueExists(ValidationContext context, String fieldName, String value) {
        if (Objects.equals(fieldName, "uuid")) {
            return Optional.ofNullable(context.realm().getClientById(value)).isPresent();
        }
        return false;
    }

    private String fieldValue(String fieldName, BaseClientRepresentation target) throws AssertionError {
        return toStringValue(MAPPERS.resolveFieldValue(fieldName, target));
    }

    private static String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
