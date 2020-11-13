package org.keycloak.social.nia;

import org.keycloak.broker.saml.mappers.UserAttributeMapper;

public class NiaUserAttributeMapper extends UserAttributeMapper {

    private static final String MAPPER_NAME = "NIA-attribute-mapper";

    @Override
    public String[] getCompatibleProviders() {
        return NiaIdentityProviderFactory.COMPATIBLE_PROVIDER;

    }

    @Override
    public String getId() {
        return MAPPER_NAME;
    }
}
