package org.keycloak.testsuite.broker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractGroupMapperTest extends AbstractIdentityProviderMapperTest {

    public static final String MAPPER_TEST_GROUP_NAME = "mapper-test";
    public static final String MAPPER_TEST_GROUP_PATH = "/" + MAPPER_TEST_GROUP_NAME;

    protected abstract void createMapperInIdp(
            IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode);

    protected void updateUser() {
    }

    protected UserRepresentation loginAsUserTwiceWithMapper(
            IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin,
            Map<String, List<String>> userConfig) {
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        if (!createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode);
        }
        createUserInProviderRealm(userConfig);

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        if (!createAfterFirstLogin) {
            assertThatUserHasBeenAssignedToGroup(user);
        } else {
            assertThatUserHasNotBeenAssignedToGroup(user);
        }

        if (createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode);
        }
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        updateUser();

        logInAsUserInIDP();
        user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        return user;
    }

    protected void assertThatUserHasBeenAssignedToGroup(UserRepresentation user) {
        List<String> groupNames = new ArrayList<>();

        realm.users().get(user.getId()).groups().forEach(group -> {
            groupNames.add(group.getName());
        });

        assertTrue(groupNames.contains(MAPPER_TEST_GROUP_NAME));
    }

    protected void assertThatUserHasNotBeenAssignedToGroup(UserRepresentation user) {
        List<String> groupNames = new ArrayList<>();

        realm.users().get(user.getId()).groups().forEach(group -> {
            groupNames.add(group.getName());
        });

        assertFalse(groupNames.contains(MAPPER_TEST_GROUP_NAME));
    }
}
