package org.keycloak.tests.broker;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.provider.HardcodedRoleMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;
import static org.keycloak.models.IdentityProviderMapperSyncMode.LEGACY;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public class HardcodedRoleMapperTest extends AbstractRoleMapperTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @BeforeEach
    public void setupRealm() {
        super.addClients();
    }

    @Test
    public void mapperGrantsRoleOnFirstLogin() {
        createMapperThenLoginAsUserTwiceWithHardcodedRoleMapper(IMPORT);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void mapperDoesNotGrantRoleInModeImportIfMapperIsAddedLater() {
        loginAsUserThenCreateMapperAndLoginAgainWithHardcodedRoleMapper(IMPORT);

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserDoesNotGrantRoleInLegacyMode() {
        loginAsUserThenCreateMapperAndLoginAgainWithHardcodedRoleMapper(LEGACY);

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserGrantsRoleInForceMode() {
        loginAsUserThenCreateMapperAndLoginAgainWithHardcodedRoleMapper(FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserMatchDoesntDeleteRole() {
        createMapperThenLoginAsUserTwiceWithHardcodedRoleMapper(FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    private void createMapperThenLoginAsUserTwiceWithHardcodedRoleMapper(IdentityProviderMapperSyncMode syncMode) {
        loginAsUserTwiceWithMapper(syncMode, false, Collections.emptyMap());
    }

    private void loginAsUserThenCreateMapperAndLoginAgainWithHardcodedRoleMapper(IdentityProviderMapperSyncMode syncMode) {
        loginAsUserTwiceWithMapper(syncMode, true, Collections.emptyMap());
    }

    @Override
    protected void createMapperInIdp(IdentityProviderMapperSyncMode syncMode, String roleValue) {
        IdentityProviderMapperRepresentation advancedClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToRoleMapper.setName("oidc-hardcoded-role-mapper");
        advancedClaimToRoleMapper.setIdentityProviderMapper(HardcodedRoleMapper.PROVIDER_ID);
        advancedClaimToRoleMapper.setConfig(ImmutableMap.<String, String> builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(ConfigConstants.ROLE, roleValue)
                .build());

        persistMapper(advancedClaimToRoleMapper);
    }

    @Override
    protected Map<String, List<String>> createUserConfigForRole(String roleValue) {
        return Collections.emptyMap();
    }
}
