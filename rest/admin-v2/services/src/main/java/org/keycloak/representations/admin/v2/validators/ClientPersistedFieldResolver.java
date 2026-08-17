package org.keycloak.representations.admin.v2.validators;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.client.DefaultClientService;
import org.keycloak.services.client.query.FieldResolver;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Resolves persisted field values for clients.
 */
public class ClientPersistedFieldResolver implements PersistedFieldResolver<BaseClientRepresentation> {

    @Override
    public boolean supports(Class<? extends BaseClientRepresentation> representationType) {
        return BaseClientRepresentation.class.isAssignableFrom(representationType);
    }

    @Override
    public Object getValue(BaseClientRepresentation representation, String fieldName) {
        // TODO: if this can ever return non-simple types we have to ensure the objects implement the equals method
        // if not, and we could consider converting to JsonNode or Map via Jackson logic
        return FieldResolver.resolve(fieldName, representation);
    }

    @Override
    public BaseClientRepresentation getPersisted(ValidationContext context, BaseClientRepresentation representation) {
        ClientModel persistedClient = context.realm().getClientByClientId(representation.getClientId());
        if (persistedClient == null) {
            return null;
        }
        // TODO: any fields materialized by secondary logic will not be populated
        var schema = DefaultClientService.SCHEMAS.get(representation.getProtocol());
        return schema != null ? schema.fromModel(persistedClient) : null;
    }

}
