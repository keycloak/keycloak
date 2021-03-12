package org.keycloak.social.nia;

import org.keycloak.broker.saml.mappers.AdvancedAttributeToRoleMapper;

public class NiaAdvancedAttributeToRoleMapper extends AdvancedAttributeToRoleMapper {

    private static final String MAPPER_NAME = "NIA-advanced-attribute-to-role-mapper";

    @Override
    public String[] getCompatibleProviders() {
        return NiaIdentityProviderFactory.COMPATIBLE_PROVIDER;

    }

    @Override
    public String getId() {
        return MAPPER_NAME;
    }
}
