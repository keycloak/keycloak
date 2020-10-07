package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.mappers.AdvancedAttributeToRoleMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

import static org.keycloak.testsuite.broker.KcSamlBrokerConfiguration.ATTRIBUTE_TO_MAP_FRIENDLY_NAME;

/**
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class KcSamlAdvancedAttributeToRoleMapperTest extends AbstractAdvancedRoleMapperTest {

    private static final String ATTRIBUTES = "[\n" +
            "  {\n" +
            "    \"key\": \"" + ATTRIBUTE_TO_MAP_FRIENDLY_NAME + "\",\n" +
            "    \"value\": \"value 1\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2 + "\",\n" +
            "    \"value\": \"value 2\"\n" +
            "  }\n" +
            "]";


    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration();
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, String claimsOrAttributeRepresentation, boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation advancedAttributeToRoleMapper = new IdentityProviderMapperRepresentation();
        advancedAttributeToRoleMapper.setName("advanced-attribute-to-role-mapper");
        advancedAttributeToRoleMapper.setIdentityProviderMapper(AdvancedAttributeToRoleMapper.PROVIDER_ID);
        advancedAttributeToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(AdvancedAttributeToRoleMapper.ATTRIBUTE_PROPERTY_NAME, claimsOrAttributeRepresentation)
                .put(AdvancedAttributeToRoleMapper.ARE_ATTRIBUTE_VALUES_REGEX_PROPERTY_NAME, areClaimsOrAttributeValuesRegexes ? "true" : "false")
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedAttributeToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(advancedAttributeToRoleMapper).close();
    }

    @Test
    public void attributeFriendlyNameGetsConsideredAndMatchedToRole() {
        createAdvancedRoleMapper(ATTRIBUTES, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(ATTRIBUTE_TO_MAP_FRIENDLY_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

}
