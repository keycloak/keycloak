package org.keycloak.testsuite.broker;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author hmlnarik,
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class OidcAdvancedClaimToRoleMapperTest extends AbstractRoleMapperTest {

    private static final String CLAIMS = "[\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME + "\",\n" +
            "    \"value\": \"value 1\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2 + "\",\n" +
            "    \"value\": \"value 2\"\n" +
            "  }\n" +
            "]";

    private static final String CLAIMS_REGEX = "[\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME + "\",\n" +
            "    \"value\": \"va.*\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2 + "\",\n" +
            "    \"value\": \"value 2\"\n" +
            "  }\n" +
            "]";

    private String newValueForAttribute2 = "";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Test
    public void valueMatchesRegexTest() {
        AdvancedClaimToRoleMapper advancedClaimToRoleMapper = new AdvancedClaimToRoleMapper();

        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("AB.*", "AB_ADMIN"), is(true));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("AB.*", "AA_ADMIN"), is(false));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("99.*", 999), is(true));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("98.*", 999), is(false));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("99\\..*", 99.9), is(true));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("AB.*", null), is(false));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("AB.*", Arrays.asList("AB_ADMIN", "AA_ADMIN")), is(true));
    }

    @Test
    public void allClaimValuesMatch() {
        createAdvancedClaimToRoleMapper(CLAIMS, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void claimValuesMismatch() {
        createAdvancedClaimToRoleMapper(CLAIMS, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value mismatch").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void claimValuesMatchIfNoClaimsSpecified() {
        createAdvancedClaimToRoleMapper("[]", false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("some value").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("some value").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void allClaimValuesMatchRegex() {
        createAdvancedClaimToRoleMapper(CLAIMS_REGEX, true);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }


    @Test
    public void claimValuesMismatchRegex() {
        createAdvancedClaimToRoleMapper(CLAIMS_REGEX, true);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("mismatch").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserMismatchDeletesRole() {
        newValueForAttribute2 = "value mismatch";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, false);

        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserMismatchDoesNotDeleteRoleInImportMode() {
        newValueForAttribute2 = "value mismatch";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(IMPORT, false);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserMatchDoesntDeleteRole() {
        newValueForAttribute2 = "value 2";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, false);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserAssignsRoleInForceModeWhenCreatingTheMapperAfterFirstLogin() {
        newValueForAttribute2 = "value 2";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, true);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    public UserRepresentation createMapperAndLoginAsUserTwiceWithMapper(IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin) {
        return loginAsUserTwiceWithMapper(syncMode, createAfterFirstLogin, ImmutableMap.<String, List<String>>builder()
            .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
            .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
            .build());
    }

    @Override
    protected void updateUser() {
        UserRepresentation user = findUser(bc.providerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        ImmutableMap<String, List<String>> matchingAttributes = ImmutableMap.<String, List<String>>builder()
            .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
            .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add(newValueForAttribute2).build())
            .put("some.other.attribute", ImmutableList.<String>builder().add("some value").build())
            .build();
        user.setAttributes(matchingAttributes);
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        createAdvancedClaimToRoleMapperInIdp(idp, CLAIMS, false, syncMode);
    }

    private void createAdvancedClaimToRoleMapper(String claimsRepresentation, boolean areClaimValuesRegex) {
        IdentityProviderRepresentation idp = setupIdentityProvider();
        createAdvancedClaimToRoleMapperInIdp(idp, claimsRepresentation, areClaimValuesRegex, IMPORT);
    }

    protected void createAdvancedClaimToRoleMapperInIdp(IdentityProviderRepresentation idp , String claimsRepresentation, boolean areClaimValuesRegex, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation advancedClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToRoleMapper.setName("advanced-claim-to-role-mapper");
        advancedClaimToRoleMapper.setIdentityProviderMapper(AdvancedClaimToRoleMapper.PROVIDER_ID);
        advancedClaimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(AdvancedClaimToRoleMapper.CLAIM_PROPERTY_NAME, claimsRepresentation)
                .put(AdvancedClaimToRoleMapper.ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME, areClaimValuesRegex ? "true" : "false")
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedClaimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(advancedClaimToRoleMapper).close();
    }
}
