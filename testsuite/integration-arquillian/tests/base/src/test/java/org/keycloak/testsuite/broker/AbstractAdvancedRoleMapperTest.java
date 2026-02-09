package org.keycloak.testsuite.broker;

import java.util.List;
import java.util.Map;

import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;

/**
 * @author hmlnarik,
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public abstract class AbstractAdvancedRoleMapperTest extends AbstractRoleMapperTest {

    private static final String CLAIMS_OR_ATTRIBUTES = "[\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME + "\",\n" +
            "    \"value\": \"value 1\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME + "\",\n" +
            "    \"value\": \"value 2\"\n" +
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
        createAdvancedRoleMapper(CLAIMS_OR_ATTRIBUTES, false);
        createUserInProviderRealm(createMatchingUserConfig());

        logInAsUserInIDPForFirstTime();

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void valuesMismatch() {
        createAdvancedRoleMapper(CLAIMS_OR_ATTRIBUTES, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>> builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME,
                        ImmutableList.<String> builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String> builder().add("value mismatch").build())
                .build());

        logInAsUserInIDPForFirstTime();

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void valuesMatchIfNoClaimsSpecified() {
        createAdvancedRoleMapper("[]", false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>> builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME,
                        ImmutableList.<String> builder().add("some value").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String> builder().add("some value").build())
                .build());

        logInAsUserInIDPForFirstTime();

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void allValuesMatchRegex() {
        createAdvancedRoleMapper(CLAIMS_OR_ATTRIBUTES_REGEX, true);
        createUserInProviderRealm(createMatchingUserConfig());

        logInAsUserInIDPForFirstTime();

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void valuesMismatchRegex() {
        createAdvancedRoleMapper(CLAIMS_OR_ATTRIBUTES_REGEX, true);
        createUserInProviderRealm(ImmutableMap.<String, List<String>> builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME,
                        ImmutableList.<String> builder().add("mismatch").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String> builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserMismatchDeletesRole() {
        newValueForAttribute2 = "value mismatch";
        createMapperAndLoginAsUserTwiceWithMapper(FORCE, false);

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserMismatchDoesNotDeleteRoleInImportMode() {
        newValueForAttribute2 = "value mismatch";
        createMapperAndLoginAsUserTwiceWithMapper(IMPORT, false);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserMatchDoesntDeleteRole() {
        newValueForAttribute2 = "value 2";
        createMapperAndLoginAsUserTwiceWithMapper(FORCE, false);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserAssignsRoleInForceModeWhenCreatingTheMapperAfterFirstLogin() {
        newValueForAttribute2 = "value 2";
        createMapperAndLoginAsUserTwiceWithMapper(FORCE, true);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void valuesMatchIfNullClaimsSpecified() {
        createAdvancedRoleMapper(null, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME,
                        ImmutableList.<String>builder().add("some value").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String>builder().add("some value").build())
                .build());

        logInAsUserInIDPForFirstTime();

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    public void createMapperAndLoginAsUserTwiceWithMapper(IdentityProviderMapperSyncMode syncMode,
            boolean createAfterFirstLogin) {
        loginAsUserTwiceWithMapper(syncMode, createAfterFirstLogin, createMatchingUserConfig());
    }

    @Override
    protected void updateUser() {
        UserRepresentation user = findUser(bc.providerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        ImmutableMap<String, List<String>> matchingAttributes = ImmutableMap.<String, List<String>> builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME,
                        ImmutableList.<String> builder().add("value 1").add("value 2").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String> builder().add(newValueForAttribute2).build())
                .put("some.other.attribute", ImmutableList.<String> builder().add("some value").build())
                .build();
        user.setAttributes(matchingAttributes);
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);
    }

    @Override
    protected void createMapperInIdp(IdentityProviderMapperSyncMode syncMode, String roleValue) {
        createMapperInIdp(CLAIMS_OR_ATTRIBUTES, false, syncMode, roleValue);
    }

    @Override
    protected Map<String, List<String>> createUserConfigForRole(String roleValue) {
        return createMatchingUserConfig();
    }

    private static Map<String, List<String>> createMatchingUserConfig() {
        return ImmutableMap.<String, List<String>> builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME,
                        ImmutableList.<String> builder().add("value 1").add("value 2").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2,
                        ImmutableList.<String> builder().add("value 2").build())
                .build();
    }

    protected void createAdvancedRoleMapper(String claimsOrAttributeRepresentation,
            boolean areClaimsOrAttributeValuesRegexes) {
        setupIdentityProvider();
        createMapperInIdp(claimsOrAttributeRepresentation, areClaimsOrAttributeValuesRegexes, IMPORT,
                CLIENT_ROLE_MAPPER_REPRESENTATION);
    }

    abstract protected void createMapperInIdp(String claimsOrAttributeRepresentation,
            boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode, String roleValue);
}
