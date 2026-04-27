package org.keycloak.scim.model.user;


import org.keycloak.models.KeycloakSession;
import org.keycloak.scim.resource.schema.attribute.Attribute;

import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;

public final class UserEnterpriseModelSchema extends UserExtensionModelSchema {

    public UserEnterpriseModelSchema(KeycloakSession session) {
        super(session, ENTERPRISE_USER_SCHEMA);
    }

    @Override
    public String getId() {
        return ENTERPRISE_USER_SCHEMA;
    }

    @Override
    public String getName() {
        return "EnterpriseUser";
    }

    @Override
    public String getDescription() {
        return "Enterprise User";
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    protected boolean hasSchema(String attributeName) {
        return getId().equals(Attribute.getSchema(attributeName));
    }
}
