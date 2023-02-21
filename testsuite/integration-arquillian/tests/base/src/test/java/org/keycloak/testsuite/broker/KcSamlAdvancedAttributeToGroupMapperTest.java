package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.mappers.AdvancedAttributeToGroupMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.keycloak.testsuite.broker.KcSamlBrokerConfiguration.ATTRIBUTE_TO_MAP_FRIENDLY_NAME;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class KcSamlAdvancedAttributeToGroupMapperTest extends AbstractGroupBrokerMapperTest {

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
    protected String createMapperInIdp(IdentityProviderRepresentation idp, String claimsOrAttributeRepresentation,
                                     boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode, String groupValue) {
        IdentityProviderMapperRepresentation advancedAttributeToGroupMapper = new IdentityProviderMapperRepresentation();
        advancedAttributeToGroupMapper.setName("advanced-attribute-to-group-mapper");
        advancedAttributeToGroupMapper.setIdentityProviderMapper(AdvancedAttributeToGroupMapper.PROVIDER_ID);
        advancedAttributeToGroupMapper.setConfig(ImmutableMap.<String, String> builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(AdvancedAttributeToGroupMapper.ATTRIBUTE_PROPERTY_NAME, claimsOrAttributeRepresentation)
                .put(AdvancedAttributeToGroupMapper.ARE_ATTRIBUTE_VALUES_REGEX_PROPERTY_NAME,
                        Boolean.valueOf(areClaimsOrAttributeValuesRegexes).toString())
                .put(ConfigConstants.GROUP, MAPPER_TEST_GROUP_PATH)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedAttributeToGroupMapper.setIdentityProviderAlias(bc.getIDPAlias());
        Response response = idpResource.addMapper(advancedAttributeToGroupMapper);
        return CreatedResponseUtil.getCreatedId(response);
    }

    @Test
    public void attributeFriendlyNameGetsConsideredAndMatchedToGroup() {
        createAdvancedGroupMapper(ATTRIBUTES, false,KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2);
        createUserInProviderRealm(ImmutableMap.<String, List<String>> builder()
                .put(ATTRIBUTE_TO_MAP_FRIENDLY_NAME, ImmutableList.<String> builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String> builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();
        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user, MAPPER_TEST_GROUP_PATH);
    }

}
