package org.keycloak.services.client.scim;

import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

/**
 * Schema singleton for SAML clients. Defines the 8 JPA-queryable fields and
 * provides attribute-filtered population of {@link SAMLClientRepresentation}.
 */
public final class SAMLClientModelSchema extends BaseClientModelSchema<SAMLClientRepresentation> {

    public static final SAMLClientModelSchema INSTANCE = new SAMLClientModelSchema();

    private SAMLClientModelSchema() {
    }

    @Override
    public SAMLClientRepresentation createRepresentation() {
        return new SAMLClientRepresentation();
    }
}
