package org.keycloak.testsuite.broker;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.oidc.mappers.ExternalKeycloakRoleToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;
import static org.keycloak.models.IdentityProviderMapperSyncMode.LEGACY;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class ExternalKeycloakRoleToRoleMapperTest extends AbstractRoleMapperTest {
    private boolean deleteRoleFromUser = true;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Before
    public void setupRealm() {
        super.addClients();
    }

    @Test
    public void mapperGrantsRoleOnFirstLogin() {
        createMapperThenLoginAsUserTwiceWithExternalKeycloakRoleToRoleMapper(IMPORT);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserDoesNotGrantRoleInLegacyMode() {
        loginAsUserThenCreateMapperAndLoginAgainWithExternalKeycloakRoleToRoleMapper(LEGACY);

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserGrantsRoleInForceMode() {
        loginAsUserThenCreateMapperAndLoginAgainWithExternalKeycloakRoleToRoleMapper(FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserMatchDeletesRoleInForceMode() {
        createMapperThenLoginAsUserTwiceWithExternalKeycloakRoleToRoleMapper(FORCE);

        assertThatRoleHasNotBeenAssignedInConsumerRealm();
    }

    @Test
    public void updateBrokeredUserMatchDoesNotDeleteRoleInLegacyMode() {
        createMapperThenLoginAsUserTwiceWithExternalKeycloakRoleToRoleMapper(LEGACY);

        assertThatRoleHasBeenAssignedInConsumerRealm();
    }

    private void createMapperThenLoginAsUserTwiceWithExternalKeycloakRoleToRoleMapper(
            IdentityProviderMapperSyncMode syncMode) {
        loginAsUserTwiceWithMapper(syncMode, false, Collections.emptyMap());
    }

    private void loginAsUserThenCreateMapperAndLoginAgainWithExternalKeycloakRoleToRoleMapper(
            IdentityProviderMapperSyncMode syncMode) {
        deleteRoleFromUser = false;
        loginAsUserTwiceWithMapper(syncMode, true, Collections.emptyMap());
    }

    @Override
    protected void createMapperInIdp(IdentityProviderMapperSyncMode syncMode, String roleValue) {
        IdentityProviderMapperRepresentation externalRoleToRoleMapper = new IdentityProviderMapperRepresentation();
        externalRoleToRoleMapper.setName("external-keycloak-role-mapper");
        externalRoleToRoleMapper.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        externalRoleToRoleMapper.setConfig(ImmutableMap.<String, String> builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("external.role", ROLE_USER)
                .put(ConfigConstants.ROLE, roleValue)
                .build());

        persistMapper(externalRoleToRoleMapper);
    }

    @Override
    public void updateUser() {
        if (deleteRoleFromUser) {
            RoleRepresentation role = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();
            UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
            userResource.roles().realmLevel().remove(Collections.singletonList(role));
        }
    }

    @Override
    protected Map<String, List<String>> createUserConfigForRole(String roleValue) {
        return Collections.emptyMap();
    }
}
