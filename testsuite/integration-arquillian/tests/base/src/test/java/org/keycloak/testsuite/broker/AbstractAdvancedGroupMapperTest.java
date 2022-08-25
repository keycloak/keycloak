package org.keycloak.testsuite.broker;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public abstract class AbstractAdvancedGroupMapperTest extends AbstractGroupMapperTest {

    private static final String CLAIMS_OR_ATTRIBUTES = "[\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME + "\",\n" +
            "    \"value\": \"value 1\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2 + "\",\n" +
            "    \"value\": \"value 2\"\n" +
            "  }\n" +
            "]";

    private static final String CLAIMS_OR_ATTRIBUTES_REGEX = "[\n" +
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

    @Before
    public void addMapperTestGroupToConsumerRealm() {
        GroupRepresentation mapperTestGroup = new GroupRepresentation();
        mapperTestGroup.setName(MAPPER_TEST_GROUP_NAME);
        mapperTestGroup.setPath(MAPPER_TEST_GROUP_PATH);

        adminClient.realm(bc.consumerRealmName()).groups().add(mapperTestGroup);
    }

    @Test
    public void allValuesMatch() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void valuesMismatch() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value mismatch").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasNotBeenAssignedToGroup(user);
    }

    @Test
    public void valuesMatchIfNoClaimsSpecified() {
        createAdvancedGroupMapper("[]", false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("some value").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("some value").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void allValuesMatchRegex() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES_REGEX, true);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void valuesMismatchRegex() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES_REGEX, true);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("mismatch").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasNotBeenAssignedToGroup(user);
    }

    @Test
    public void updateBrokeredUserMismatchLeavesGroup() {
        newValueForAttribute2 = "value mismatch";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, false);

        assertThatUserHasNotBeenAssignedToGroup(user);
    }

    @Test
    public void updateBrokeredUserMismatchDoesNotLeaveGroupInImportMode() {
        newValueForAttribute2 = "value mismatch";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(IMPORT, false);

        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void updateBrokeredUserMatchDoesntLeaveGroup() {
        newValueForAttribute2 = "value 2";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, false);

        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void updateBrokeredUserIsAssignedToGroupInForceModeWhenCreatingTheMapperAfterFirstLogin() {
        newValueForAttribute2 = "value 2";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, true);

        assertThatUserHasBeenAssignedToGroup(user);
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
        createMapperInIdp(idp, CLAIMS_OR_ATTRIBUTES, false, syncMode);
    }

    protected void createAdvancedGroupMapper(String claimsOrAttributeRepresentation, boolean areClaimsOrAttributeValuesRegexes) {
        IdentityProviderRepresentation idp = setupIdentityProvider();
        createMapperInIdp(idp, claimsOrAttributeRepresentation, areClaimsOrAttributeValuesRegexes, IMPORT);
    }

    abstract protected void createMapperInIdp(
            IdentityProviderRepresentation idp, String claimsOrAttributeRepresentation, boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode);
}
