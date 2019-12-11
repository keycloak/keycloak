package org.keycloak.testsuite.broker;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;
import static org.keycloak.models.IdentityProviderMapperSyncMode.LEGACY;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.provider.HardcodedRoleMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableMap;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class HardcodedRoleMapperTest extends AbstractRoleMapperTest {
    private RealmResource realm;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Before
    public void setupRealm() {
        super.addClients();
        realm = adminClient.realm(bc.consumerRealmName());
    }

    @Test
    public void mapperGrantsRoleOnFirstLogin() {
        UserRepresentation user = createMapperThenLoginAsUserTwiceWithHardcodedRoleMapper(IMPORT);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void mapperDoesNotGrantRoleInModeImportIfMapperIsAddedLater() {
        UserRepresentation user = loginAsUserThenCreateMapperAndLoginAgainWithHardcodedRoleMapper(IMPORT);

        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserDoesNotGrantRoleInLegacyMode() {
        UserRepresentation user = loginAsUserThenCreateMapperAndLoginAgainWithHardcodedRoleMapper(LEGACY);

        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserGrantsRoleInForceMode() {
        UserRepresentation user = loginAsUserThenCreateMapperAndLoginAgainWithHardcodedRoleMapper(FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserMatchDoesntDeleteRole() {
        UserRepresentation user = createMapperThenLoginAsUserTwiceWithHardcodedRoleMapper(FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    private UserRepresentation createMapperThenLoginAsUserTwiceWithHardcodedRoleMapper(IdentityProviderMapperSyncMode syncMode) {
        return loginAsUserTwiceWithMapper(syncMode, false, new HashMap<>());
    }

    private UserRepresentation loginAsUserThenCreateMapperAndLoginAgainWithHardcodedRoleMapper(IdentityProviderMapperSyncMode syncMode) {
        return loginAsUserTwiceWithMapper(syncMode, true, new HashMap<>());
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation advancedClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToRoleMapper.setName("oidc-hardcoded-role-mapper");
        advancedClaimToRoleMapper.setIdentityProviderMapper(HardcodedRoleMapper.PROVIDER_ID);
        advancedClaimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedClaimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(advancedClaimToRoleMapper).close();
    }
}
