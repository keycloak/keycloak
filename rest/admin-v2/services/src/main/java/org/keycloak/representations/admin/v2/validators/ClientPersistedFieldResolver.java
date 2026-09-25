package org.keycloak.representations.admin.v2.validators;


import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.client.scim.ClientResourceTypeProvider;
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
        return ClientResourceTypeProvider.resolveField(fieldName, representation);
    }

    @Override
    public BaseClientRepresentation getPersisted(ValidationContext context, BaseClientRepresentation representation) {
        ClientModel persistedClient = context.realm().getClientByClientId(representation.getClientId());
        if (persistedClient == null) {
            return null;
        }
        return ClientResourceTypeProvider.fromModel(persistedClient);
    }

}
