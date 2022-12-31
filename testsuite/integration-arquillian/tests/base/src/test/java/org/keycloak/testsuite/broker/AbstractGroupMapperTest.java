package org.keycloak.testsuite.broker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:artur.baltabayev@bosch.io">Artur Baltabayev</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public abstract class AbstractGroupMapperTest extends AbstractIdentityProviderMapperTest {

    public static final String MAPPER_TEST_GROUP_NAME = "mapper-test";
    public static final String MAPPER_TEST_GROUP_PATH = buildGroupPath(MAPPER_TEST_GROUP_NAME);

    public static final String MAPPER_TEST_NOT_EXISTING_GROUP_PATH = buildGroupPath("mapper-test-not-existing");

    protected String mapperGroupId;

    protected abstract String createMapperInIdp(
            IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode, String groupPath);

    /**
     * Sets up a scenario with the given group path.
     * @return the ID of the mapper
     */
    protected abstract String setupScenarioWithGroupPath(String groupPath);

    protected abstract void setupScenarioWithNonExistingGroup();

    protected void updateUser() {
    }

    @Before
    public void addMapperTestGroupToConsumerRealm() {
        GroupRepresentation mapperTestGroup = new GroupRepresentation();
        mapperTestGroup.setName(MAPPER_TEST_GROUP_NAME);

        Response response = adminClient.realm(bc.consumerRealmName()).groups().add(mapperTestGroup);
        mapperGroupId = CreatedResponseUtil.getCreatedId(response);
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

    protected UserRepresentation loginAsUserTwiceWithMapper(
            IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin,
            Map<String, List<String>> userConfig, String groupPath) {
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        if (!createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode, groupPath);
        }
        createUserInProviderRealm(userConfig);

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        if (!createAfterFirstLogin) {
            assertThatUserHasBeenAssignedToGroup(user);
        } else {
            assertThatUserHasNotBeenAssignedToGroup(user);
        }

        if (createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode, groupPath);
        }
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        updateUser();

        logInAsUserInIDP();
        assertLoggedInAccountManagement();

        user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        return user;
    }

    private void assertMapperHasExpectedPathAndSucceeds(String mapperId, String expectedGroupPath) {
        IdentityProviderMapperRepresentation mapper =
                realm.identityProviders().get(bc.getIDPAlias()).getMapperById(mapperId);
        Map<String, String> config = mapper.getConfig();
        assertThat(config.get(ConfigConstants.GROUP), equalTo(expectedGroupPath));

        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatUserHasBeenAssignedToGroup(user, expectedGroupPath);
    }

    protected void assertThatUserHasBeenAssignedToGroup(UserRepresentation user) {
        assertThatUserHasBeenAssignedToGroup(user, MAPPER_TEST_GROUP_PATH);
    }

    protected void assertThatUserHasBeenAssignedToGroup(UserRepresentation user, String groupPath) {
        assertThat(getUserGroupPaths(user), contains(groupPath));
    }

    protected void assertThatUserHasNotBeenAssignedToGroup(UserRepresentation user) {
        assertThat(getUserGroupPaths(user), not(contains(MAPPER_TEST_GROUP_PATH)));
    }

    protected void assertThatUserDoesNotHaveGroups(UserRepresentation user) {
        assertThat(getUserGroupPaths(user), empty());
    }

    protected static String buildGroupPath(String firstSegment, String... furtherSegments) {
        String separator = KeycloakModelUtils.GROUP_PATH_SEPARATOR;
        StringBuilder sb = new StringBuilder(separator).append(firstSegment);
        for (String furtherSegment : furtherSegments) {
            sb.append(separator).append(furtherSegment);
        }
        return sb.toString();
    }

    private List<String> getUserGroupPaths(UserRepresentation user) {
        return realm.users().get(user.getId()).groups().stream().map(GroupRepresentation::getPath)
                .collect(Collectors.toList());
    }
}
