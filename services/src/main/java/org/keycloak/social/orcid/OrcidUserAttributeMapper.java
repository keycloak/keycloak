package org.keycloak.social.orcid;

import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;

public class OrcidUserAttributeMapper extends AbstractJsonUserAttributeMapper {

    public static final String PROVIDER_ID = "orcid-user-attribute-mapper";
    private static final String[] cp = new String[] { OrcidIdentityProviderFactory.PROVIDER_ID };

    @Override
    public String[] getCompatibleProviders() {
        return cp;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}