package org.keycloak.testsuite.broker;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import java.util.HashMap;

import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.provider.HardcodedAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableMap;

/**
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 */
public class HardcodedUserAttributeMapperTest extends AbstractIdentityProviderMapperTest {

    private static final String USER_ATTRIBUTE = "user-attribute";
    private static final String USER_ATTRIBUTE_VALUE = "user-attribute";

    @Test
    public void addHardcodedAttributeOnFirstLogin() {
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        createMapperInIdp(idp, IMPORT);
        createUserInProviderRealm();

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatAttributeHasBeenAssigned(user);
    }

    @Test
    public void hardcodedAttributeGetsAddedEvenIfMapperIsAddedLaterInSyncModeForce() {
        UserRepresentation user = loginAsUserTwiceWithMapper(FORCE, true);

        assertThatAttributeHasBeenAssigned(user);
    }

    @Test
    public void hardcodedAttributeDoesNotGetAddedIfMapperIsAddedLaterInSyncModeImport() {
        UserRepresentation user = loginAsUserTwiceWithMapper(IMPORT, true);

        assertThatAttributeHasNotBeenAssigned(user);
    }

    @Test
    public void hardcodedAttributeDoesNotGetAddedAgainInSyncModeImport() {
        UserRepresentation user = loginAsUserTwiceWithMapper(IMPORT, false);

        assertThatAttributeHasNotBeenAssigned(user);
    }

    @Test
    public void hardcodedAttributeGetsUpdatedInSyncModeForce() {
        UserRepresentation user = loginAsUserTwiceWithMapper(FORCE, false);

        assertThatAttributeHasBeenAssigned(user);
    }

    protected UserRepresentation loginAsUserTwiceWithMapper(
        IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin) {
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        if (!createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode);
        }
        createUserInProviderRealm();

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        if (!createAfterFirstLogin) {
            assertThatAttributeHasBeenAssigned(user);
        } else {
            assertThatAttributeHasNotBeenAssigned(user);
        }

        if (createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode);
        }
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        if (user.getAttributes() != null) {
            user.setAttributes(new HashMap<>());
        }
        adminClient.realm(bc.consumerRealmName()).users().get(user.getId()).update(user);

        logInAsUserInIDP();
        return findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
    }

    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation advancedClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToRoleMapper.setName("hardcoded-attribute-mapper");
        advancedClaimToRoleMapper.setIdentityProviderMapper(HardcodedAttributeMapper.PROVIDER_ID);
        advancedClaimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
            .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
            .put(HardcodedAttributeMapper.ATTRIBUTE, USER_ATTRIBUTE)
            .put(HardcodedAttributeMapper.ATTRIBUTE_VALUE, USER_ATTRIBUTE_VALUE)
            .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedClaimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(advancedClaimToRoleMapper).close();
    }

    protected void createUserInProviderRealm() {
        createUserInProviderRealm(new HashMap<>());
    }

    protected void assertThatAttributeHasBeenAssigned(UserRepresentation user) {
        assertThat(user.getAttributes().get(USER_ATTRIBUTE), contains(USER_ATTRIBUTE_VALUE));
    }

    protected void assertThatAttributeHasNotBeenAssigned(UserRepresentation user) {
        if (user.getAttributes() != null) {
            assertThat(user.getAttributes().get(USER_ATTRIBUTE), not(contains(USER_ATTRIBUTE_VALUE)));
        }
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }
}
