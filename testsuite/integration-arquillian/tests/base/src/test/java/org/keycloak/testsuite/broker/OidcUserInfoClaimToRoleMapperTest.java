package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.ClaimToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.LEGACY;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class OidcUserInfoClaimToRoleMapperTest extends AbstractRoleMapperTest {

    private static final String USER_INFO_CLAIM = KcOidcBrokerConfigurationUserInfoOnlyMappers.ATTRIBUTE_TO_MAP_USER_INFO;
    private static final String USER_INFO_CLAIM_VALUE = "value 1";
    private String claimOnSecondLogin = "";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationUserInfoOnlyMappers();
    }

    @Test
    public void singleClaimValueInUserInfoMatches() {
        createClaimToRoleMapper(USER_INFO_CLAIM_VALUE);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(USER_INFO_CLAIM, ImmutableList.<String>builder().add(USER_INFO_CLAIM_VALUE).build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void noRoleAddedIfUserInfoDisabledAndOnlyClaimIsInUserInfo() {
        createClaimToRoleMapperWithUserInfoDisabledInIdP(USER_INFO_CLAIM_VALUE);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(USER_INFO_CLAIM, ImmutableList.<String>builder().add(USER_INFO_CLAIM_VALUE).build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void claimValuesMismatch() {
        createClaimToRoleMapper("other value");
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(USER_INFO_CLAIM, ImmutableList.<String>builder().add(USER_INFO_CLAIM_VALUE).build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserMismatchDeletesRoleInForceMode() {
        UserRepresentation user = loginWithClaimThenChangeClaimToValue("value mismatch", FORCE, false);

        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserMismatchDeletesRoleInLegacyMode() {
        UserRepresentation user = createMapperThenLoginWithStandardClaimThenChangeClaimToValue("value mismatch", LEGACY);

        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserNewMatchGrantsRoleAfterFirstLoginInForceMode() {
        UserRepresentation user = loginWithStandardClaimThenAddMapperAndLoginAgain(FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserNewMatchDoesNotGrantRoleAfterFirstLoginInLegacyMode() {
        UserRepresentation user = loginWithStandardClaimThenAddMapperAndLoginAgain(LEGACY);

        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserDoesNotDeleteRoleIfClaimStillMatches() {
        UserRepresentation user = createMapperThenLoginWithStandardClaimThenChangeClaimToValue(USER_INFO_CLAIM_VALUE, FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    private UserRepresentation loginWithStandardClaimThenAddMapperAndLoginAgain(IdentityProviderMapperSyncMode syncMode) {
        return loginWithClaimThenChangeClaimToValue(OidcUserInfoClaimToRoleMapperTest.USER_INFO_CLAIM_VALUE, syncMode, true);
    }

    private UserRepresentation createMapperThenLoginWithStandardClaimThenChangeClaimToValue(String claimOnSecondLogin, IdentityProviderMapperSyncMode syncMode) {
        return loginWithClaimThenChangeClaimToValue(claimOnSecondLogin, syncMode, false);
    }

    @NotNull
    private UserRepresentation loginWithClaimThenChangeClaimToValue(String claimOnSecondLogin, IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin) {
        this.claimOnSecondLogin = claimOnSecondLogin;
        return loginAsUserTwiceWithMapper(syncMode, createAfterFirstLogin,
            ImmutableMap.<String, List<String>>builder()
            .put(USER_INFO_CLAIM, ImmutableList.<String>builder().add(USER_INFO_CLAIM_VALUE).build())
            .build());
    }

    private void createClaimToRoleMapper(String claimValue) {
        IdentityProviderRepresentation idp = setupIdentityProvider();
        createClaimToRoleMapper(idp, claimValue, IdentityProviderMapperSyncMode.IMPORT);
    }

    private void createClaimToRoleMapperWithUserInfoDisabledInIdP(String claimValue) {
        IdentityProviderRepresentation idp = setupIdentityProviderDisableUserInfo();
        createClaimToRoleMapper(idp, claimValue, IdentityProviderMapperSyncMode.IMPORT);
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        createClaimToRoleMapper(idp, USER_INFO_CLAIM_VALUE, syncMode);
    }


    @Override
    protected void updateUser() {
        UserRepresentation user = findUser(bc.providerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        ImmutableMap<String, List<String>> mismatchingAttributes = ImmutableMap.<String, List<String>>builder()
            .put(USER_INFO_CLAIM, ImmutableList.<String>builder().add(claimOnSecondLogin).build())
            .build();
        user.setAttributes(mismatchingAttributes);
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);
    }

    private void createClaimToRoleMapper(IdentityProviderRepresentation idp, String claimValue, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation claimToRoleMapper = new IdentityProviderMapperRepresentation();
        claimToRoleMapper.setName("userinfo-claim-to-role-mapper");
        claimToRoleMapper.setIdentityProviderMapper(ClaimToRoleMapper.PROVIDER_ID);
        claimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
            .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
            .put(ClaimToRoleMapper.CLAIM, OidcUserInfoClaimToRoleMapperTest.USER_INFO_CLAIM)
            .put(ClaimToRoleMapper.CLAIM_VALUE, claimValue)
            .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
            .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        claimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(claimToRoleMapper).close();
    }

}
