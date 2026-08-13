package org.keycloak.testsuite.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.mappers.AdvancedAttributeToGroupMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.AccountHelper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

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
            "  },\n" + "  {\n" +
            "    \"key\": \"" + ATTRIBUTE_TO_MAP_FRIENDLY_NAME + "\",\n" +
            "    \"value\": \"value 2\"\n" +
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

        final Map<String, String> config = new HashMap<>();
        config.put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString());
        config.put(AdvancedAttributeToGroupMapper.ATTRIBUTE_PROPERTY_NAME, claimsOrAttributeRepresentation);
        config.put(AdvancedAttributeToGroupMapper.ARE_ATTRIBUTE_VALUES_REGEX_PROPERTY_NAME,
                Boolean.valueOf(areClaimsOrAttributeValuesRegexes).toString());
        config.put(ConfigConstants.GROUP, MAPPER_TEST_GROUP_PATH);
        advancedAttributeToGroupMapper.setConfig(config);

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedAttributeToGroupMapper.setIdentityProviderAlias(bc.getIDPAlias());
        Response response = idpResource.addMapper(advancedAttributeToGroupMapper);
        return CreatedResponseUtil.getCreatedId(response);
    }

    @Test
    public void attributeFriendlyNameGetsConsideredAndMatchedToGroup() {
        createAdvancedGroupMapper(ATTRIBUTES, false, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(ATTRIBUTE_TO_MAP_FRIENDLY_NAME, ImmutableList.<String>builder().add("value 1").add("value 2").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();
        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user, MAPPER_TEST_GROUP_PATH);
    }

    @Test
    public void removingAndAddingTheGroupKeepsTheGroup() {
        // Create a mapper that is always executed (force)
        String idpMapperId = createAdvancedGroupMapper(ATTRIBUTES, false, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2);
        IdentityProviderResource idp = adminClient.realm(bc.consumerRealmName()).identityProviders().get(BrokerTestConstants.IDP_SAML_ALIAS);
        IdentityProviderMapperRepresentation idpMapper = idp.getMapperById(idpMapperId);
        idpMapper.getConfig().put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString());
        idp.update(idpMapperId, idpMapper);

        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(ATTRIBUTE_TO_MAP_FRIENDLY_NAME, ImmutableList.<String>builder().add("value 1").add("value 2").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String>builder().add("value 2").build())
                .build());

        // Login once and logout on both sides
        logInAsUserInIDPForFirstTimeAndAssertSuccess();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        // Ensure that the expected group exists
        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user, MAPPER_TEST_GROUP_PATH);

        // Add a mapper to remove the group, and ensure that it has a smaller ID than the other one to ensure that it is executed first
        idpMapper.getConfig().put("attributes", "[{\"key\": \"key\", \"value\": \"value\"}]");
        idpMapper.setId("00000000-00000000-00000000-00000000");
        idpMapper.setName(idpMapper.getName() + "-2");
        CreatedResponseUtil.getCreatedId(idp.addMapper(idpMapper));

        logInAsUserInIDP();
        user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user, MAPPER_TEST_GROUP_PATH);
    }

}
