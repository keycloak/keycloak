package org.keycloak.services.client.scim;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

/**
 * Schema singleton for OIDC clients. Defines the 8 JPA-queryable fields and
 * provides attribute-filtered population of {@link OIDCClientRepresentation}.
 */
public final class OIDCClientModelSchema extends BaseClientModelSchema<OIDCClientRepresentation> {

    public static final OIDCClientModelSchema INSTANCE = new OIDCClientModelSchema();

    private OIDCClientModelSchema() {
    }

    @Override
    public OIDCClientRepresentation createRepresentation() {
        return new OIDCClientRepresentation();
    }
}
