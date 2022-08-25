package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToGroupMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import com.google.common.collect.ImmutableMap;

public class OidcAdvancedClaimToGroupMapperTest extends AbstractAdvancedGroupMapperTest {
    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, String claimsOrAttributeRepresentation,
            boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation advancedClaimToGroupMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToGroupMapper.setName("advanced-claim-to-group-mapper");
        advancedClaimToGroupMapper.setIdentityProviderMapper(AdvancedClaimToGroupMapper.PROVIDER_ID);
        advancedClaimToGroupMapper.setConfig(ImmutableMap.<String, String> builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(AdvancedClaimToGroupMapper.CLAIM_PROPERTY_NAME, claimsOrAttributeRepresentation)
                .put(AdvancedClaimToGroupMapper.ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME,
                        areClaimsOrAttributeValuesRegexes ? "true" : "false")
                .put(ConfigConstants.GROUP, MAPPER_TEST_GROUP_PATH)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedClaimToGroupMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(advancedClaimToGroupMapper).close();
    }
}
