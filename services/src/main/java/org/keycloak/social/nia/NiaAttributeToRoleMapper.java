package org.keycloak.social.nia;

import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;

public class NiaAttributeToRoleMapper extends AttributeToRoleMapper {

    private static final String MAPPER_NAME = "NIA-attribute-to-role-mapper";

    @Override
    public String[] getCompatibleProviders() {
        return NiaIdentityProviderFactory.COMPATIBLE_PROVIDER;

    }

    @Override
    public String getId() {
        return MAPPER_NAME;
    }
}
