package org.keycloak.testsuite.broker;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;

import org.junit.Test;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:artur.baltabayev@bosch.io">Artur Baltabayev</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
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

    @Test
    public void allValuesMatch() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES, false, MAPPER_TEST_GROUP_PATH);
        createUserInProviderRealm(createMatchingAttributes());

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void valuesMismatch() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES, false, MAPPER_TEST_GROUP_PATH);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value mismatch").build())
                .build());

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasNotBeenAssignedToGroup(user);
    }

    @Test
    public void valuesMatchIfNoClaimsSpecified() {
        createAdvancedGroupMapper("[]", false, MAPPER_TEST_GROUP_PATH);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("some value").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("some value").build())
                .build());

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void allValuesMatchRegex() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES_REGEX, true, MAPPER_TEST_GROUP_PATH);
        createUserInProviderRealm(createMatchingAttributes());

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void valuesMismatchRegex() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES_REGEX, true, MAPPER_TEST_GROUP_PATH);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("mismatch").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasNotBeenAssignedToGroup(user);
    }

    @Test
    public void updateBrokeredUserMismatchLeavesGroup() {
        newValueForAttribute2 = "value mismatch";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, false, MAPPER_TEST_GROUP_PATH);

        assertThatUserHasNotBeenAssignedToGroup(user);
    }

    @Test
    public void updateBrokeredUserMismatchDoesNotLeaveGroupInImportMode() {
        newValueForAttribute2 = "value mismatch";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(IMPORT, false, MAPPER_TEST_GROUP_PATH);

        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void updateBrokeredUserMatchDoesntLeaveGroup() {
        newValueForAttribute2 = "value 2";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, false, MAPPER_TEST_GROUP_PATH);

        assertThatUserHasBeenAssignedToGroup(user);
    }

    @Test
    public void tryToUpdateBrokeredUserWithMissingGroupDoesNotBreakLogin() {
        newValueForAttribute2 = "value 2";
        UserRepresentation user =
                createMapperAndLoginAsUserTwiceWithMapper(FORCE, true, MAPPER_TEST_NOT_EXISTING_GROUP_PATH);

        assertThatUserDoesNotHaveGroups(user);
    }

    @Test
    public void updateBrokeredUserIsAssignedToGroupInForceModeWhenCreatingTheMapperAfterFirstLogin() {
        newValueForAttribute2 = "value 2";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, true, MAPPER_TEST_GROUP_PATH);

        assertThatUserHasBeenAssignedToGroup(user);
    }

    public UserRepresentation createMapperAndLoginAsUserTwiceWithMapper(IdentityProviderMapperSyncMode syncMode,
            boolean createAfterFirstLogin, String groupPath) {
        return loginAsUserTwiceWithMapper(syncMode, createAfterFirstLogin, createMatchingAttributes(), groupPath);
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
    protected String createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode,
            String groupPath) {
        return createMapperInIdp(idp, CLAIMS_OR_ATTRIBUTES, false, syncMode, groupPath);
    }

    @Override
    protected String setupScenarioWithMatchingGroup() {
        String mapperId = createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES, false, MAPPER_TEST_GROUP_PATH);
        createUserInProviderRealm(createMatchingAttributes());
        return mapperId;
    }

    @Override
    protected void setupScenarioWithNonExistingGroup() {
        createAdvancedGroupMapper(CLAIMS_OR_ATTRIBUTES, false, MAPPER_TEST_NOT_EXISTING_GROUP_PATH);
        createUserInProviderRealm(createMatchingAttributes());
    }

    protected String createAdvancedGroupMapper(String claimsOrAttributeRepresentation,
                                               boolean areClaimsOrAttributeValuesRegexes, String groupPath) {
        IdentityProviderRepresentation idp = setupIdentityProvider();
        return createMapperInIdp(idp, claimsOrAttributeRepresentation, areClaimsOrAttributeValuesRegexes, IMPORT,
                groupPath);
    }

    abstract protected String createMapperInIdp(
            IdentityProviderRepresentation idp, String claimsOrAttributeRepresentation,
            boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode, String groupPath);

    private static Map<String, List<String>> createMatchingAttributes() {
        return ImmutableMap.<String, List<String>> builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME,
                        ImmutableList.<String> builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String> builder().add("value 2").build())
                .build();
    }
}
