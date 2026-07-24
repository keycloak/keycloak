package org.keycloak.testsuite.broker;

import java.util.List;
import java.util.Map;

import org.keycloak.broker.oidc.mappers.ClaimToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.LEGACY;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class OidcClaimToRoleMapperTest extends AbstractRoleMapperTest {

    protected static final String CLAIM = KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME;
    protected static final String CLAIM_VALUE = "value 1";
    private String claimOnSecondLogin = "";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Test
    public void allClaimValuesMatch() {
        createClaimToRoleMapper(CLAIM_VALUE);
        createUserInProviderRealm(createUserConfig());

        logInAsUserInIDPForFirstTime();

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void claimValuesMismatch() {
        createClaimToRoleMapper("other value");
        createUserInProviderRealm(createUserConfig());

        logInAsUserInIDPForFirstTime();

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserMismatchDeletesRoleInForceMode() {
        loginWithClaimThenChangeClaimToValue("value mismatch", FORCE, false);

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserMismatchDeletesRoleInLegacyMode() {
        createMapperThenLoginWithStandardClaimThenChangeClaimToValue("value mismatch", LEGACY);

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserNewMatchGrantsRoleAfterFirstLoginInForceMode() {
        loginWithStandardClaimThenAddMapperAndLoginAgain(FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserNewMatchDoesNotGrantRoleAfterFirstLoginInLegacyMode() {
        loginWithStandardClaimThenAddMapperAndLoginAgain(LEGACY);

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserDoesNotDeleteRoleIfClaimStillMatches() {
        createMapperThenLoginWithStandardClaimThenChangeClaimToValue(CLAIM_VALUE, FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    private void loginWithStandardClaimThenAddMapperAndLoginAgain(IdentityProviderMapperSyncMode syncMode) {
        loginWithClaimThenChangeClaimToValue(OidcClaimToRoleMapperTest.CLAIM_VALUE, syncMode, true);
    }

    private void createMapperThenLoginWithStandardClaimThenChangeClaimToValue(String claimOnSecondLogin, IdentityProviderMapperSyncMode syncMode) {
        loginWithClaimThenChangeClaimToValue(claimOnSecondLogin, syncMode, false);
    }

    private void loginWithClaimThenChangeClaimToValue(String claimOnSecondLogin, IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin) {
        this.claimOnSecondLogin = claimOnSecondLogin;
        loginAsUserTwiceWithMapper(syncMode, createAfterFirstLogin, createUserConfig());
    }

    private void createClaimToRoleMapper(String claimValue) {
        setupIdentityProvider();
        createClaimToRoleMapper(claimValue, IdentityProviderMapperSyncMode.IMPORT, CLIENT_ROLE_MAPPER_REPRESENTATION);
    }

    @Override
    protected void createMapperInIdp(IdentityProviderMapperSyncMode syncMode, String roleValue) {
        createClaimToRoleMapper(CLAIM_VALUE, syncMode, roleValue);
    }

    @Override
    protected void updateUser() {
        UserRepresentation user = findUser(bc.providerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        ImmutableMap<String, List<String>> mismatchingAttributes = ImmutableMap.<String, List<String>>builder()
            .put(CLAIM, ImmutableList.<String>builder().add(claimOnSecondLogin).build())
            .build();
        user.setAttributes(mismatchingAttributes);
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);
    }

    protected void createClaimToRoleMapper(String claimValue, IdentityProviderMapperSyncMode syncMode,
            String roleValue) {
        IdentityProviderMapperRepresentation claimToRoleMapper = new IdentityProviderMapperRepresentation();
        claimToRoleMapper.setName("claim-to-role-mapper");
        claimToRoleMapper.setIdentityProviderMapper(ClaimToRoleMapper.PROVIDER_ID);
        claimToRoleMapper.setConfig(ImmutableMap.<String, String> builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(ClaimToRoleMapper.CLAIM, OidcClaimToRoleMapperTest.CLAIM)
                .put(ClaimToRoleMapper.CLAIM_VALUE, claimValue)
                .put(ConfigConstants.ROLE, roleValue)
                .build());

        persistMapper(claimToRoleMapper);
    }

    @Override
    protected Map<String, List<String>> createUserConfigForRole(String roleValue) {
        return createUserConfig();
    }

    private static ImmutableMap<String, List<String>> createUserConfig() {
        return ImmutableMap.<String, List<String>>builder()
                .put(CLAIM, ImmutableList.<String>builder().add(CLAIM_VALUE).build())
                .build();
    }
}
