package org.keycloak.testsuite.broker;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.Before;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

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

    protected UserRepresentation loginAsUserTwiceWithMapper(
            IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin,
            Map<String, List<String>> userConfig, String groupPath) throws IOException {
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
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());

        updateUser();

        logInAsUserInIDP();
        appPage.assertCurrent();

        user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        return user;
    }

    protected void assertMapperHasExpectedPathAndSucceeds(String mapperId, String expectedGroupPath) {
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
