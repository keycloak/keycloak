package org.keycloak.testsuite.broker;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.broker.oidc.mappers.AdvancedClaimToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;

/**
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class OidcAdvancedClaimToRoleMapperTest extends AbstractAdvancedRoleMapperTest {
    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Override
    protected void createMapperInIdp(String claimsOrAttributeRepresentation,
            boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode, String roleValue) {
        IdentityProviderMapperRepresentation advancedClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToRoleMapper.setName("advanced-claim-to-role-mapper");
        advancedClaimToRoleMapper.setIdentityProviderMapper(AdvancedClaimToRoleMapper.PROVIDER_ID);

        final Map<String, String> config = new HashMap<>();
        config.put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString());
        config.put(AdvancedClaimToRoleMapper.CLAIM_PROPERTY_NAME, claimsOrAttributeRepresentation);
        config.put(AdvancedClaimToRoleMapper.ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME,
                Boolean.valueOf(areClaimsOrAttributeValuesRegexes).toString());
        config.put(ConfigConstants.ROLE, roleValue);
        advancedClaimToRoleMapper.setConfig(config);

        persistMapper(advancedClaimToRoleMapper);
    }
}
