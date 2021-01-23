package org.keycloak.testsuite.broker;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.util.WaitUtils.pause;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * @author hmlnarik,
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 */
public abstract class AbstractRoleMapperTest extends AbstractIdentityProviderMapperTest {

    private static final String CLIENT = "realm-management";
    private static final String CLIENT_ROLE = "view-realm";
    public static final String ROLE_USER = "user";
    public static final String CLIENT_ROLE_MAPPER_REPRESENTATION = CLIENT + "." + CLIENT_ROLE;

    protected abstract void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode);

    protected void updateUser() {
    }

    protected UserRepresentation loginAsUserTwiceWithMapper(
        IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin, Map<String, List<String>> userConfig) {
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        if (!createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode);
        }
        createUserInProviderRealm(userConfig);
        createUserRoleAndGrantToUserInProviderRealm();

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        if (!createAfterFirstLogin) {
            assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
        } else {
            assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
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

    protected void createUserRoleAndGrantToUserInProviderRealm() {
        RoleRepresentation userRole = new RoleRepresentation(ROLE_USER,null, false);
        adminClient.realm(bc.providerRealmName()).roles().create(userRole);
        RoleRepresentation role = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();
        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(role));
    }

    protected void assertThatRoleHasBeenAssignedInConsumerRealmTo(UserRepresentation user) {
        assertThat(user.getClientRoles().get(CLIENT), contains(CLIENT_ROLE));
    }

    protected void assertThatRoleHasNotBeenAssignedInConsumerRealmTo(UserRepresentation user) {
        assertThat(user.getClientRoles().get(CLIENT), not(contains(CLIENT_ROLE)));
    }
}
