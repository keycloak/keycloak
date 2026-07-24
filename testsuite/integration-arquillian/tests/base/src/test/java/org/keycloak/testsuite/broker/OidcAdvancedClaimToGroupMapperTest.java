package org.keycloak.testsuite.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToGroupMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;

/**
 * @author <a href="mailto:artur.baltabayev@bosch.io">Artur Baltabayev</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class OidcAdvancedClaimToGroupMapperTest extends AbstractGroupBrokerMapperTest {
    protected boolean isHardcodedGroup() {
        return false;
    }

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

        final Map<String, String> config = new HashMap<>();
        config.put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString());
        config.put(AdvancedClaimToGroupMapper.CLAIM_PROPERTY_NAME, claimsOrAttributeRepresentation);
        config.put(AdvancedClaimToGroupMapper.ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME,
                Boolean.valueOf(areClaimsOrAttributeValuesRegexes).toString());
        config.put(ConfigConstants.GROUP, groupPath);
        advancedClaimToGroupMapper.setConfig(config);

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedClaimToGroupMapper.setIdentityProviderAlias(bc.getIDPAlias());
        Response response = idpResource.addMapper(advancedClaimToGroupMapper);
        return CreatedResponseUtil.getCreatedId(response);
    }

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
        if (!isHardcodedGroup()) {
            assertThatUserHasNotBeenAssignedToGroup(user);
        } else {
            assertThatUserHasBeenAssignedToGroup(user);
        }
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
        if (!isHardcodedGroup()) {
            assertThatUserHasNotBeenAssignedToGroup(user);
        } else {
            assertThatUserHasBeenAssignedToGroup(user);
        }
    }

    @Test
    public void updateBrokeredUserMismatchLeavesGroup() {
        newValueForAttribute2 = "value mismatch";
        UserRepresentation user = createMapperAndLoginAsUserTwiceWithMapper(FORCE, false, MAPPER_TEST_GROUP_PATH);

        if (!isHardcodedGroup()) {
            assertThatUserHasNotBeenAssignedToGroup(user);
        } else {
            assertThatUserHasBeenAssignedToGroup(user);
        }
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

    @Test
    public void tryToCreateBrokeredUserWithNonExistingGroupDoesNotBreakLogin() {
        setupScenarioWithNonExistingGroup();

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserDoesNotHaveGroups(user);
    }

    @Test
    public void mapperStillWorksWhenTopLevelGroupIsConvertedToSubGroup() {
        final String mapperId = setupScenarioWithGroupPath(MAPPER_TEST_GROUP_PATH);

        String newParentGroupName = "new-parent";
        GroupRepresentation newParentGroup = new GroupRepresentation();
        newParentGroup.setName(newParentGroupName);
        String newParentGroupId = CreatedResponseUtil.getCreatedId(realm.groups().add(newParentGroup));

        GroupRepresentation mappedGroup = realm.groups().group(mapperGroupId).toRepresentation();
        realm.groups().group(newParentGroupId).subGroup(mappedGroup).close();

        String expectedNewGroupPath = buildGroupPath(newParentGroupName, MAPPER_TEST_GROUP_NAME);

        assertMapperHasExpectedPathAndSucceeds(mapperId, expectedNewGroupPath);
    }

    @Test
    public void mapperStillWorksWhenSubGroupChangesParent() {
        String parentGroupName = "parent-group";
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setName(parentGroupName);
        String parentGroupId = CreatedResponseUtil.getCreatedId(realm.groups().add(parentGroup));

        GroupRepresentation mappedGroup = realm.groups().group(mapperGroupId).toRepresentation();
        realm.groups().group(parentGroupId).subGroup(mappedGroup).close();

        String initialGroupPath = buildGroupPath(parentGroupName, MAPPER_TEST_GROUP_NAME);

        final String mapperId = setupScenarioWithGroupPath(initialGroupPath);

        String newParentGroupName = "new-parent-group";
        GroupRepresentation newParentGroup = new GroupRepresentation();
        newParentGroup.setName(newParentGroupName);
        String newParentGroupId = CreatedResponseUtil.getCreatedId(realm.groups().add(newParentGroup));

        realm.groups().group(newParentGroupId).subGroup(mappedGroup).close();

        String expectedNewGroupPath = buildGroupPath(newParentGroupName, MAPPER_TEST_GROUP_NAME);

        assertMapperHasExpectedPathAndSucceeds(mapperId, expectedNewGroupPath);
    }

    @Test
    public void mapperStillWorksWhenSubGroupIsConvertedToTopLevelGroup() {
        String parentGroupName = "parent-group";
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setName(parentGroupName);
        String parentGroupId = CreatedResponseUtil.getCreatedId(realm.groups().add(parentGroup));

        GroupRepresentation mappedGroup = realm.groups().group(mapperGroupId).toRepresentation();
        realm.groups().group(parentGroupId).subGroup(mappedGroup).close();

        String initialGroupPath = buildGroupPath(parentGroupName, MAPPER_TEST_GROUP_NAME);

        final String mapperId = setupScenarioWithGroupPath(initialGroupPath);

        // convert the mapped group to a top-level group
        realm.groups().add(realm.groups().group(mapperGroupId).toRepresentation());

        String expectedNewGroupPath = buildGroupPath(MAPPER_TEST_GROUP_NAME);

        assertMapperHasExpectedPathAndSucceeds(mapperId, expectedNewGroupPath);
    }

    @Test
    public void mapperStillWorksWhenGroupIsRenamed() {
        final String mapperId = setupScenarioWithGroupPath(MAPPER_TEST_GROUP_PATH);

        String newGroupName = "new-name-" + MAPPER_TEST_GROUP_NAME;
        GroupRepresentation mappedGroup = realm.groups().group(mapperGroupId).toRepresentation();
        mappedGroup.setName(newGroupName);
        realm.groups().group(mapperGroupId).update(mappedGroup);

        String expectedNewGroupPath = buildGroupPath(newGroupName);

        assertMapperHasExpectedPathAndSucceeds(mapperId, expectedNewGroupPath);
    }

    @Test
    public void mapperStillWorksWhenAncestorGroupIsRenamed() {
        String topLevelGroupName = "top-level";
        GroupRepresentation topLevelGroup = new GroupRepresentation();
        topLevelGroup.setName(topLevelGroupName);
        String topLevelGroupId = CreatedResponseUtil.getCreatedId(realm.groups().add(topLevelGroup));

        String midLevelGroupName = "mid-level";
        GroupRepresentation midLevelGroup = new GroupRepresentation();
        midLevelGroup.setName(midLevelGroupName);
        String midLevelGroupId = CreatedResponseUtil.getCreatedId(realm.groups().add(midLevelGroup));

        midLevelGroup = realm.groups().group(midLevelGroupId).toRepresentation();
        realm.groups().group(topLevelGroupId).subGroup(midLevelGroup).close();

        GroupRepresentation mappedGroup = realm.groups().group(mapperGroupId).toRepresentation();
        realm.groups().group(midLevelGroupId).subGroup(mappedGroup).close();

        String initialGroupPath = buildGroupPath(topLevelGroupName, midLevelGroupName, MAPPER_TEST_GROUP_NAME);

        final String mapperId = setupScenarioWithGroupPath(initialGroupPath);

        String newTopLevelGroupName = "new-name-" + topLevelGroupName;
        topLevelGroup = realm.groups().group(topLevelGroupId).toRepresentation();
        topLevelGroup.setName(newTopLevelGroupName);
        realm.groups().group(topLevelGroupId).update(topLevelGroup);

        String expectedNewGroupPath = buildGroupPath(newTopLevelGroupName, midLevelGroupName, MAPPER_TEST_GROUP_NAME);

        assertMapperHasExpectedPathAndSucceeds(mapperId, expectedNewGroupPath);
    }
}
