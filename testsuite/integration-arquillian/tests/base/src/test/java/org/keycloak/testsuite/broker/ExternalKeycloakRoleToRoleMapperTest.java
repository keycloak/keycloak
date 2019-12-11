package org.keycloak.testsuite.broker;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.oidc.mappers.ExternalKeycloakRoleToRoleMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableMap;

import static org.keycloak.models.IdentityProviderMapperSyncMode.*;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class ExternalKeycloakRoleToRoleMapperTest extends AbstractRoleMapperTest {
    private RealmResource realm;
    private boolean deleteRoleFromUser = true;

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
        UserRepresentation user = createMapperThenLoginAsUserTwiceWithExternalKeycloakRoleToRoleMapper(IMPORT);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserDoesNotGrantRoleInLegacyMode() {
        UserRepresentation user = loginAsUserThenCreateMapperAndLoginAgainWithExternalKeycloakRoleToRoleMapper(LEGACY);

        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserGrantsRoleInForceMode() {
        UserRepresentation user = loginAsUserThenCreateMapperAndLoginAgainWithExternalKeycloakRoleToRoleMapper(FORCE);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserMatchDeletesRoleInForceMode() {
        UserRepresentation user = createMapperThenLoginAsUserTwiceWithExternalKeycloakRoleToRoleMapper(FORCE);

        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserMatchDoesNotDeleteRoleInLegacyMode() {
        UserRepresentation user = createMapperThenLoginAsUserTwiceWithExternalKeycloakRoleToRoleMapper(LEGACY);

        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    private UserRepresentation createMapperThenLoginAsUserTwiceWithExternalKeycloakRoleToRoleMapper(IdentityProviderMapperSyncMode syncMode) {
        return loginAsUserTwiceWithMapper(syncMode, false, ImmutableMap.<String, List<String>>builder().build());
    }

    private UserRepresentation loginAsUserThenCreateMapperAndLoginAgainWithExternalKeycloakRoleToRoleMapper(IdentityProviderMapperSyncMode syncMode) {
        deleteRoleFromUser = false;
        return loginAsUserTwiceWithMapper(syncMode, true, ImmutableMap.<String, List<String>>builder().build());
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation externalRoleToRoleMapper = new IdentityProviderMapperRepresentation();
        externalRoleToRoleMapper.setName("external-keycloak-role-mapper");
        externalRoleToRoleMapper.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        externalRoleToRoleMapper.setConfig(ImmutableMap.<String,String>builder()
            .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
            .put("external.role", ROLE_USER)
            .put("role", CLIENT_ROLE_MAPPER_REPRESENTATION)
            .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        externalRoleToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(externalRoleToRoleMapper).close();
    }

    @Override
    public void updateUser() {
        if (deleteRoleFromUser) {
            RoleRepresentation role = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();
            UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
            userResource.roles().realmLevel().remove(Collections.singletonList(role));
        }
    }
}
