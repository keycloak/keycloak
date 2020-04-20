package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;

/**
 * @author hmlnarik,
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public abstract class AbstractAdvancedRoleMapperTest extends AbstractRoleMapperTest {

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
        createAdvancedRoleMapper(CLAIMS_OR_ATTRIBUTES, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void valuesMismatch() {
        createAdvancedRoleMapper(CLAIMS_OR_ATTRIBUTES, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value mismatch").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void valuesMatchIfNoClaimsSpecified() {
        createAdvancedRoleMapper("[]", false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("some value").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("some value").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void allValuesMatchRegex() {
        createAdvancedRoleMapper(CLAIMS_OR_ATTRIBUTES_REGEX, true);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void valuesMismatchRegex() {
        createAdvancedRoleMapper(CLAIMS_OR_ATTRIBUTES_REGEX, true);
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
        createMapperInIdp(idp, CLAIMS_OR_ATTRIBUTES, false, syncMode);
    }

    protected void createAdvancedRoleMapper(String claimsOrAttributeRepresentation, boolean areClaimsOrAttributeValuesRegexes) {
        IdentityProviderRepresentation idp = setupIdentityProvider();
        createMapperInIdp(idp, claimsOrAttributeRepresentation, areClaimsOrAttributeValuesRegexes, IMPORT);
    }

    abstract protected void createMapperInIdp(
            IdentityProviderRepresentation idp, String claimsOrAttributeRepresentation, boolean areClaimsOrAttributeValuesRegexes, IdentityProviderMapperSyncMode syncMode);
}
