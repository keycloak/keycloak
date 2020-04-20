package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class OidcAdvancedClaimToRoleMapperTest extends AbstractAdvancedRoleMapperTest {
    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, String claimsOrAttributeRepresentation, boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation advancedClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToRoleMapper.setName("advanced-claim-to-role-mapper");
        advancedClaimToRoleMapper.setIdentityProviderMapper(AdvancedClaimToRoleMapper.PROVIDER_ID);
        advancedClaimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(AdvancedClaimToRoleMapper.CLAIM_PROPERTY_NAME, claimsOrAttributeRepresentation)
                .put(AdvancedClaimToRoleMapper.ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME, areClaimsOrAttributeValuesRegexes ? "true" : "false")
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedClaimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(advancedClaimToRoleMapper).close();
    }
}
