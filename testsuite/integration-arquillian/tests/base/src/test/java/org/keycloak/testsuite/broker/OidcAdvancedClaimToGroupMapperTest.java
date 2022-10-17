package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToGroupMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:artur.baltabayev@bosch.io">Artur Baltabayev</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class OidcAdvancedClaimToGroupMapperTest extends AbstractAdvancedGroupMapperTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Override
    protected String createMapperInIdp(IdentityProviderRepresentation idp, String claimsOrAttributeRepresentation,
            boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode, String groupPath) {
        IdentityProviderMapperRepresentation advancedClaimToGroupMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToGroupMapper.setName("advanced-claim-to-group-mapper");
        advancedClaimToGroupMapper.setIdentityProviderMapper(AdvancedClaimToGroupMapper.PROVIDER_ID);
        advancedClaimToGroupMapper.setConfig(ImmutableMap.<String, String> builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(AdvancedClaimToGroupMapper.CLAIM_PROPERTY_NAME, claimsOrAttributeRepresentation)
                .put(AdvancedClaimToGroupMapper.ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME,
                        Boolean.valueOf(areClaimsOrAttributeValuesRegexes).toString())
                .put(ConfigConstants.GROUP, groupPath)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedClaimToGroupMapper.setIdentityProviderAlias(bc.getIDPAlias());
        Response response = idpResource.addMapper(advancedClaimToGroupMapper);
        return CreatedResponseUtil.getCreatedId(response);
    }

}
