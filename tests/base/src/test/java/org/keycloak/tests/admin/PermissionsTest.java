/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.admin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.services.resources.admin.AdminAuth.Resource;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.CredentialBuilder;
import org.keycloak.testframework.realm.FederatedIdentityBuilder;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.hamcrest.Matchers;
import org.jgroups.util.UUID;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class PermissionsTest extends AbstractPermissionsTest {

    @InjectRealm(config = PermissionsTestRealmConfig1.class, ref = "realm1")
    ManagedRealm managedRealm1;

    @InjectRealm(config = PermissionsTestRealmConfig2.class, ref = "realm2")
    ManagedRealm managedRealm2;

    @Test
    public void realmLogoutAll() {
        Invocation invocation = RealmResource::logoutAll;

        invoke(invocation, clients.get("master-none"), false);
        invoke(invocation, clients.get("master-view-realm"), false);

        invoke(invocation, clients.get("REALM2"), false);

        invoke(invocation, clients.get("none"), false);
        invoke(invocation, clients.get("view-users"), false);
        invoke(invocation, clients.get("manage-realm"), false);
        invoke(invocation, clients.get("master-manage-realm"), false);

        invoke(invocation, clients.get("manage-users"), true);
        invoke(invocation, clients.get("master-manage-users"), true);
    }

    @Test
    public void events() {
        invoke(RealmResource::getRealmEventsConfig, Resource.EVENTS, false);
        invoke(realm -> realm.updateRealmEventsConfig(new RealmEventsConfigRepresentation()), Resource.EVENTS, true);
        invoke(RealmResource::getEvents, Resource.EVENTS, false);
        invoke(RealmResource::getAdminEvents, Resource.EVENTS, false);
        invoke(RealmResource::clearEvents, Resource.EVENTS, true);
        invoke(RealmResource::clearAdminEvents, Resource.EVENTS, true);
    }

    @Test
    public void attackDetection() {
        UserRepresentation newUser = UserBuilder.create()
                .username("attacked")
                .enabled(true)
                .build();
        String userUuid = ApiUtil.getCreatedId(managedRealm1.admin().users().create(newUser));
        managedRealm1.cleanup().add(r -> r.users().delete(userUuid).close());

        UserRepresentation user = managedRealm1.admin().users().get(userUuid).toRepresentation();
        invoke(realm -> realm.attackDetection().bruteForceUserStatus(userUuid), Resource.USER, false);
        invoke(realm -> realm.attackDetection().clearBruteForceForUser(userUuid), Resource.USER, true);
        invoke(realm -> realm.attackDetection().clearAllBruteForce(), Resource.USER, true);
    }

    @Test
    public void clients() {
        invoke(realm -> realm.clients().findAll(), Resource.CLIENT, false, true);
        List<ClientRepresentation> l = clients.get(AdminRoles.QUERY_CLIENTS).realm(REALM_NAME).clients().findAll();
        assertThat(l, Matchers.empty());

        l = clients.get(AdminRoles.VIEW_CLIENTS).realm(REALM_NAME).clients().findAll();
        assertThat(l, Matchers.not(Matchers.empty()));

        ClientRepresentation client = l.get(0);
        invoke((realm, response) ->
                        response.set(clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().create(client)),
                clients.get(AdminRoles.QUERY_USERS), false);
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().get(client.getId()).toRepresentation(),
                clients.get(AdminRoles.QUERY_USERS), false);
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().get(client.getId()).update(client),
                clients.get(AdminRoles.QUERY_USERS), false);
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().get(client.getId()).remove(),
                clients.get(AdminRoles.QUERY_USERS), false);

        invoke(realm -> realm.convertClientDescription("blahblah"), Resource.CLIENT, true);
        invoke((realm, response) ->
                        response.set(realm.clients().create(ClientBuilder.create().clientId("foo").build())),
                Resource.CLIENT, true);
        // Create/update with protocol mappers are covered by clientProtocolMappers()

        ClientRepresentation foo = managedRealm1.admin().clients().findByClientId("foo").get(0);
        // Register cleanup by clientId so it survives the remove/re-create cycle below.
        managedRealm1.cleanup().add(r -> {
            List<ClientRepresentation> found = r.clients().findByClientId("foo");
            if (!found.isEmpty()) {
                r.clients().get(found.get(0).getId()).remove();
            }
        });

        invoke(realm -> realm.clients().get(foo.getId()).toRepresentation(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getInstallationProvider("nosuch"), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).update(foo), Resource.CLIENT, true);

        invoke(realm -> {
            realm.clients().get(foo.getId()).remove();
            realm.clients().create(foo);
            ClientRepresentation temp = realm.clients().findByClientId("foo").get(0);
            foo.setId(temp.getId());
        }, Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).generateNewSecret(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).regenerateRegistrationAccessToken(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getSecret(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getClientRotatedSecret(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getServiceAccountUser(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).pushRevocation(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getApplicationSessionCount(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getUserSessions(0, 100), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getOfflineSessionCount(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getOfflineUserSessions(0, 100), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).registerNode(Map.of()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).unregisterNode("nosuch"), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).testNodesAvailable(), Resource.CLIENT, true);

        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").generate(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").generateAndGetKeystore(new KeyStoreConfig()),
                Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").getKeyInfo(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").getKeystore(new KeyStoreConfig()),
                Resource.CLIENT, false);

        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").uploadJks(null), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").uploadJksCertificate(null),
                Resource.CLIENT, true);

        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().getMapperById("nosuch"), Resource.CLIENT, false, true);
        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().getMappers(), Resource.CLIENT, false, true);
        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().getMappersPerProtocol("nosuch"),
                Resource.CLIENT, false, true);
        // Write operations (create/update/delete) via ProtocolMappersResource are covered by clientProtocolMappersResource()

        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().getAll(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().listAll(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().listEffective(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().listAvailable(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().add(List.of()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().remove(List.of()), Resource.CLIENT, true);

        invoke(realm -> realm.clients().get(UUID.randomUUID().toString()).roles().list(), Resource.CLIENT, false, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().create(new RoleRepresentation()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").toRepresentation(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).roles().deleteRole("nosuch"), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").update(new RoleRepresentation()),
                Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").addComposites(List.of()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").deleteComposites(List.of()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").getRoleComposites(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").getRealmRoleComposites(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").getClientRoleComposites("nosuch"), Resource.CLIENT, false);
        // users with query-client role should be able to query flows so the client detail page can be rendered successfully when fine-grained permissions are enabled.
        invoke(realm -> realm.flows().getFlows(), clients.get(AdminRoles.QUERY_CLIENTS), true);
        // the same for ClientAuthenticatorProviders and PerClientConfigDescription
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), clients.get(AdminRoles.QUERY_CLIENTS), true);
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), clients.get(AdminRoles.VIEW_CLIENTS), true);
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), clients.get(AdminRoles.MANAGE_CLIENTS), true);
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), clients.get(AdminRoles.QUERY_USERS), false);
        invoke(realm -> realm.flows().getPerClientConfigDescription(), clients.get(AdminRoles.QUERY_CLIENTS), true);
        for (String role : AdminRoles.ALL_QUERY_ROLES) {
            invoke(realm -> realm.clients().get(foo.getId()).roles().list(), clients.get(role), false);
        }
    }

    /**
     * Tests create/update permissions for client representations that include protocolMappers.
     *
     * <p>manage-realm users are always permitted. manage-clients users are permitted only when every
     * mapper type in the representation that is new or changed is listed in
     * {@link Constants#ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS}.
     * Unchanged mappers already persisted on the client are not checked.
     */
    @Test
    public void clientProtocolMappers() {
        // mapper whose type will be placed in the allowlist
        ProtocolMapperRepresentation allowedMapper = new ProtocolMapperRepresentation();
        allowedMapper.setName("test-mapper");
        allowedMapper.setProtocol("openid-connect");
        allowedMapper.setProtocolMapper("oidc-usermodel-property-mapper");
        allowedMapper.setConfig(Map.of("user.attribute", "username", "claim.name", "preferred_username"));

        // --- Create: baseline (no allowlist) → manage-clients always denied for mapper writes ---
        InvocationWithResponse createClientWithMappers = (realm, response) -> {
            ClientRepresentation rep = ClientBuilder.create().clientId(UUID.randomUUID().toString()).build();
            rep.setProtocolMappers(List.of(allowedMapper));
            response.set(realm.clients().create(rep));
            if (response.get().getStatus() == Response.Status.CREATED.getStatusCode()) {
                String createdId = ApiUtil.getCreatedId(response.get());
                realm.clients().get(createdId).remove();
            }
        };
        invoke(createClientWithMappers, clients.get("none"), false);
        invoke(createClientWithMappers, clients.get("master-none"), false);
        invoke(createClientWithMappers, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(createClientWithMappers, clients.get("master-admin"), true);
        // "multi" has manage-realm and view-clients, but lacks manage-clients
        invoke(createClientWithMappers, clients.get("multi"), false);
        // manage-clients only: passes requireManage() but mapper type not in (empty) allowlist → 403
        invoke(createClientWithMappers, clients.get(AdminRoles.MANAGE_CLIENTS), false);
        // manage-realm only: fails requireManage() before reaching the mapper check
        invoke(createClientWithMappers, clients.get(AdminRoles.MANAGE_REALM), false);

        // --- Create: allowlist configured → manage-clients allowed for listed types ---
        managedRealm1.updateWithCleanup(r -> r.attribute(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS,
                "oidc-usermodel-property-mapper"));
        invoke(createClientWithMappers, clients.get(AdminRoles.MANAGE_CLIENTS), true);
        invoke(createClientWithMappers, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(createClientWithMappers, clients.get("master-admin"), true);

        ProtocolMapperRepresentation disallowedMapper = new ProtocolMapperRepresentation();
        disallowedMapper.setName("disallowed-mapper");
        disallowedMapper.setProtocol("openid-connect");
        disallowedMapper.setProtocolMapper("oidc-acr-mapper");
        InvocationWithResponse createClientWithDisallowedMapper = (realm, response) -> {
            ClientRepresentation rep = ClientBuilder.create().clientId(UUID.randomUUID().toString()).build();
            rep.setProtocolMappers(List.of(disallowedMapper));
            response.set(realm.clients().create(rep));
            if (response.get().getStatus() == Response.Status.CREATED.getStatusCode()) {
                String createdId = ApiUtil.getCreatedId(response.get());
                realm.clients().get(createdId).remove();
            }
        };
        // type NOT in allowlist → manage-clients denied
        invoke(createClientWithDisallowedMapper, clients.get(AdminRoles.MANAGE_CLIENTS), false);
        invoke(createClientWithDisallowedMapper, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(createClientWithDisallowedMapper, clients.get("master-admin"), true);

        // Clear the allowlist so the Update-baseline block below runs without it
        RealmRepresentation realmRep = managedRealm1.admin().toRepresentation();
        realmRep.getAttributes().remove(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS);
        managedRealm1.admin().update(realmRep);

        // Create a fresh client with no pre-existing protocol mappers for the update tests.
        Response fooCreateResponse = managedRealm1.admin().clients()
                .create(ClientBuilder.create().clientId("foo-pm-update-test").build());
        String fooId = ApiUtil.getCreatedId(fooCreateResponse);
        managedRealm1.cleanup().add(r -> r.clients().get(fooId).remove());

        // --- Update: baseline (no allowlist) → manage-clients is denied even for a new mapper ---
        // Only test the principals that should be denied: permitted principals (realm-admin) would
        // persist the mapper into the DB, causing subsequent invocations to see it as unchanged.
        ClientRepresentation fooWithNewMapper = managedRealm1.admin().clients().get(fooId).toRepresentation();
        fooWithNewMapper.setProtocolMappers(List.of(allowedMapper));
        InvocationWithResponse updateClientWithNewMapper = (realm, response) ->
                realm.clients().get(fooWithNewMapper.getId()).update(fooWithNewMapper);

        invoke(updateClientWithNewMapper, clients.get("none"), false);
        invoke(updateClientWithNewMapper, clients.get("master-none"), false);
        // "multi" has manage-realm and view-clients, but lacks manage-clients (configure)
        invoke(updateClientWithNewMapper, clients.get("multi"), false);
        // manage-clients only: passes requireConfigure() but mapper type not in (empty) allowlist → 403
        invoke(updateClientWithNewMapper, clients.get(AdminRoles.MANAGE_CLIENTS), false);
        // manage-realm only: fails requireConfigure() before reaching the mapper check
        invoke(updateClientWithNewMapper, clients.get(AdminRoles.MANAGE_REALM), false);

        // --- Update: allowlist configured → manage-clients allowed/denied based on what actually changed ---
        managedRealm1.updateWithCleanup(r -> r.attribute(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS,
                "oidc-usermodel-property-mapper"));

        // New mapper whose type is in the allowlist → manage-clients allowed
        invoke(updateClientWithNewMapper, clients.get(AdminRoles.MANAGE_CLIENTS), true);
        invoke(updateClientWithNewMapper, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(updateClientWithNewMapper, clients.get("master-admin"), true);

        // New mapper whose type is NOT in the allowlist → manage-clients denied
        ClientRepresentation fooWithDisallowedMapper = managedRealm1.admin().clients().get(fooId).toRepresentation();
        ProtocolMapperRepresentation disallowedMapper2 = new ProtocolMapperRepresentation();
        disallowedMapper2.setName("disallowed-mapper");
        disallowedMapper2.setProtocol("openid-connect");
        disallowedMapper2.setProtocolMapper("oidc-acr-mapper");
        fooWithDisallowedMapper.setProtocolMappers(List.of(disallowedMapper2));
        InvocationWithResponse updateClientWithNewDisallowedMapper = (realm, response) ->
                realm.clients().get(fooWithDisallowedMapper.getId()).update(fooWithDisallowedMapper);
        invoke(updateClientWithNewDisallowedMapper, clients.get(AdminRoles.MANAGE_CLIENTS), false);
        invoke(updateClientWithNewDisallowedMapper, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(updateClientWithNewDisallowedMapper, clients.get("master-admin"), true);

        // Pre-persist a disallowed-type mapper on foo via admin so we can test unchanged vs. changed.
        ProtocolMapperRepresentation persistedDisallowedMapper = new ProtocolMapperRepresentation();
        persistedDisallowedMapper.setName("persisted-disallowed-mapper");
        persistedDisallowedMapper.setProtocol("openid-connect");
        persistedDisallowedMapper.setProtocolMapper("oidc-acr-mapper");
        managedRealm1.admin().clients().get(fooId).getProtocolMappers()
                .createMapper(persistedDisallowedMapper);

        // Fetch foo with its now-persisted mapper so the representation matches the DB exactly.
        ClientRepresentation fooWithPersistedMapper = managedRealm1.admin().clients().get(fooId).toRepresentation();

        // Unchanged disallowed-type mapper: no field differs → no type check fires → manage-clients allowed
        InvocationWithResponse updateClientWithUnchangedDisallowedMapper = (realm, response) ->
                realm.clients().get(fooWithPersistedMapper.getId()).update(fooWithPersistedMapper);
        invoke(updateClientWithUnchangedDisallowedMapper, clients.get(AdminRoles.MANAGE_CLIENTS), true);

        // Changed disallowed-type mapper: config modified → type check fires → manage-clients denied
        ClientRepresentation fooWithChangedDisallowedMapper = managedRealm1.admin().clients().get(fooId).toRepresentation();
        fooWithChangedDisallowedMapper.getProtocolMappers().stream()
                .filter(m -> "persisted-disallowed-mapper".equals(m.getName()))
                .forEach(m -> m.setConfig(Map.of("some.config", "changed")));
        InvocationWithResponse updateClientWithChangedDisallowedMapper = (realm, response) ->
                realm.clients().get(fooWithChangedDisallowedMapper.getId()).update(fooWithChangedDisallowedMapper);
        invoke(updateClientWithChangedDisallowedMapper, clients.get(AdminRoles.MANAGE_CLIENTS), false);
    }

    /**
     * Tests create/update/delete permissions for protocol mappers on clients via ProtocolMappersResource.
     *
     * <p>The realm attribute {@link Constants#ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS}
     * is set to {@code oidc-acr-mapper,oidc-amr-mapper} for the duration of the test.
     * Users with {@code manage-realm} are always permitted regardless of the mapper type.
     * Users with {@code manage-clients} (but not {@code manage-realm}) are permitted only when the
     * mapper type is explicitly in the allowed list; otherwise they are denied.
     */
    @Test
    public void clientProtocolMappersResource() {
        // Create the client used throughout this test and ensure it is removed afterwards.
        Response createResponse = managedRealm1.admin().clients().create(ClientBuilder.create().clientId("foo-mapper-test").build());
        String fooId = ApiUtil.getCreatedId(createResponse);
        managedRealm1.cleanup().add(r -> r.clients().get(fooId).remove());
        ClientRepresentation foo = managedRealm1.admin().clients().get(fooId).toRepresentation();

        // Configure the allowed-types list on the realm.
        managedRealm1.updateWithCleanup(r -> r.attribute(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS,
                "oidc-acr-mapper,oidc-amr-mapper"));
        // --- Allowed mapper type: both realm-admin and manage-clients users are permitted ---
            ProtocolMapperRepresentation allowedMapper = new ProtocolMapperRepresentation();
            allowedMapper.setProtocolMapper("oidc-acr-mapper");
            allowedMapper.setName("allowed-mapper");
            allowedMapper.setProtocol("openid-connect");

            // createMapper (bulk) and update perform the auth check before the existence check,
            // so denied callers receive 403 even when the mapper id does not exist.
            for (Invocation writeOp : List.<Invocation>of(
                    realm -> realm.clients().get(foo.getId()).getProtocolMappers().createMapper(List.of(allowedMapper)),
                    realm -> realm.clients().get(foo.getId()).getProtocolMappers().update("nosuch", allowedMapper))) {
                invoke(writeOp, clients.get("none"), false);
                invoke(writeOp, clients.get("master-none"), false);
                invoke(writeOp, clients.get(AdminRoles.REALM_ADMIN), true);
                invoke(writeOp, clients.get("master-admin"), true);
                // manage-clients: managePermission passes, type is in allowlist
                invoke(writeOp, clients.get(AdminRoles.MANAGE_CLIENTS), true);
                // manage-realm only: fails managePermission (requires manage-clients)
                invoke(writeOp, clients.get(AdminRoles.MANAGE_REALM), false);
            }
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get("none"), false);
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get("master-none"), false);
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get(AdminRoles.REALM_ADMIN), true);
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get("master-admin"), true);
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get(AdminRoles.MANAGE_CLIENTS), true);
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get(AdminRoles.MANAGE_REALM), false);

            // delete resolves the mapper model before the auth check, so a non-existent id always
            // yields 404. Test delete only with permitted callers; denied callers are covered above.
            invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().delete("nosuch"),
                    clients.get(AdminRoles.REALM_ADMIN), true);
            invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().delete("nosuch"),
                    clients.get("master-admin"), true);
            invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().delete("nosuch"),
                    clients.get(AdminRoles.MANAGE_CLIENTS), true);

            // --- Disallowed mapper type: only realm-admin is permitted; manage-clients is denied ---
            ProtocolMapperRepresentation disallowedMapper = new ProtocolMapperRepresentation();
            disallowedMapper.setProtocolMapper("oidc-usermodel-property-mapper");
            disallowedMapper.setName("disallowed-mapper");
            disallowedMapper.setProtocol("openid-connect");

            for (Invocation writeOp : List.<Invocation>of(
                    realm -> realm.clients().get(foo.getId()).getProtocolMappers().createMapper(List.of(disallowedMapper)),
                    realm -> realm.clients().get(foo.getId()).getProtocolMappers().update("nosuch", disallowedMapper))) {
                invoke(writeOp, clients.get("none"), false);
                invoke(writeOp, clients.get("master-none"), false);
                invoke(writeOp, clients.get(AdminRoles.REALM_ADMIN), true);
                invoke(writeOp, clients.get("master-admin"), true);
                // manage-clients: managePermission passes, but type is not in allowlist
                invoke(writeOp, clients.get(AdminRoles.MANAGE_CLIENTS), false);
                invoke(writeOp, clients.get(AdminRoles.MANAGE_REALM), false);
            }
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(disallowedMapper)),
                    clients.get(AdminRoles.MANAGE_CLIENTS), false);
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(disallowedMapper)),
                    clients.get(AdminRoles.REALM_ADMIN), true);
            invoke((realm, response) ->
                            response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(disallowedMapper)),
                    clients.get("master-admin"), true);

            // --- Update changing the provider type: the stored type must be allowed too ---
            // Persisted mapper of a disallowed type; manage-clients must not be able to replace it by an allowed type.
            ProtocolMapperRepresentation storedDisallowed = new ProtocolMapperRepresentation();
            storedDisallowed.setName("stored-disallowed-mapper");
            storedDisallowed.setProtocol("openid-connect");
            storedDisallowed.setProtocolMapper("oidc-usermodel-property-mapper");
            storedDisallowed.setConfig(Map.of("user.attribute", "username", "claim.name", "preferred_username"));
            String storedDisallowedId = ApiUtil.getCreatedId(
                    managedRealm1.admin().clients().get(fooId).getProtocolMappers().createMapper(storedDisallowed));

            ProtocolMapperRepresentation replacement = new ProtocolMapperRepresentation();
            replacement.setName("stored-disallowed-mapper");
            replacement.setProtocol("openid-connect");
            replacement.setProtocolMapper("oidc-acr-mapper");
            invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().update(storedDisallowedId, replacement),
                    clients.get(AdminRoles.MANAGE_CLIENTS), false);

            // --- Delete authorization: guard at line 313 is only reached for existing mappers ---
            // manage-clients may delete an allowed-type mapper.
            ProtocolMapperRepresentation storedAllowed = new ProtocolMapperRepresentation();
            storedAllowed.setName("stored-allowed-mapper");
            storedAllowed.setProtocol("openid-connect");
            storedAllowed.setProtocolMapper("oidc-acr-mapper");
            String storedAllowedId = ApiUtil.getCreatedId(
                    managedRealm1.admin().clients().get(fooId).getProtocolMappers().createMapper(storedAllowed));
            invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().delete(storedAllowedId),
                    clients.get(AdminRoles.MANAGE_CLIENTS), true);

        // manage-clients must not delete a disallowed-type mapper.
        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().delete(storedDisallowedId),
                clients.get(AdminRoles.MANAGE_CLIENTS), false);
    }

    @Test
    public void clientScopes() {
        invoke((RealmResource realm) -> realm.clientScopes().findAll(), Resource.CLIENT, false, true);
        invoke((RealmResource realm, AtomicReference<Response> response) -> {
            ClientScopeRepresentation scope = new ClientScopeRepresentation();
            scope.setName("scope");
            response.set(realm.clientScopes().create(scope));
        }, Resource.CLIENT, true);
        // Create/update with protocol mappers are covered by clientScopeProtocolMappers()

        // Use a dedicated scope to avoid mutating pre-existing system scopes and to ensure
        // cleanup of any state left behind (mappers, remove/re-create cycles, etc.).
        ClientScopeRepresentation dedicatedClientScope = new ClientScopeRepresentation();
        dedicatedClientScope.setName("client-scopes-test-scope");
        dedicatedClientScope.setProtocol("openid-connect");
        Response dedicatedClientScopeResponse = managedRealm1.admin().clientScopes().create(dedicatedClientScope);
        String dedicatedClientScopeId = ApiUtil.getCreatedId(dedicatedClientScopeResponse);
        // Track the live ID via AtomicReference so it survives remove/re-create below.
        AtomicReference<String> scopeId = new AtomicReference<>(dedicatedClientScopeId);
        managedRealm1.cleanup().add(r -> {
            String liveId = scopeId.get();
            List<ClientScopeRepresentation> found = r.clientScopes().findAll().stream()
                    .filter(s -> "client-scopes-test-scope".equals(s.getName()))
                    .toList();
            String id = found.isEmpty() ? liveId : found.get(0).getId();
            try { r.clientScopes().get(id).remove(); } catch (Exception ignored) {}
        });

        ClientScopeRepresentation scope = managedRealm1.admin().clientScopes().get(dedicatedClientScopeId).toRepresentation();
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).toRepresentation(), Resource.CLIENT, false);

        // Updating client scope without protocol mappers only requires manage-clients
        scope.setProtocolMappers(null);
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).update(scope), Resource.CLIENT, true);
        invoke((RealmResource realm) -> {
            realm.clientScopes().get(scopeId.get()).remove();
            Response r = realm.clientScopes().create(scope);
            if (r.getStatus() == Response.Status.CREATED.getStatusCode()) {
                scopeId.set(ApiUtil.getCreatedId(r));
                scope.setId(scopeId.get());
            }
        }, Resource.CLIENT, true);

        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getProtocolMappers().getMappers(),
                Resource.CLIENT, false, true);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scopeId.get()).getProtocolMappers().getMappersPerProtocol("nosuch"),
                Resource.CLIENT, false, true);
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getProtocolMappers().getMapperById("nosuch"),
                Resource.CLIENT, false, true);
        // Write operations (create/update/delete) via ProtocolMappersResource are covered by clientScopeProtocolMappersResource()

        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getScopeMappings().getAll(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getScopeMappings().realmLevel().listAll(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getScopeMappings().realmLevel().listAvailable(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getScopeMappings().realmLevel().listEffective(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getScopeMappings().realmLevel().add(List.of()),
                Resource.CLIENT, true);
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getScopeMappings().realmLevel().remove(List.of()),
                Resource.CLIENT, true);
        ClientRepresentation realmAccessClient = adminClient.realms().realm(REALM_NAME) .clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        invoke((RealmResource realm) -> realm.clientScopes().get(scopeId.get()).getScopeMappings().clientLevel(realmAccessClient.getId()).listAll(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scopeId.get()).getScopeMappings().clientLevel(realmAccessClient.getId()).listAvailable(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scopeId.get()).getScopeMappings().clientLevel(realmAccessClient.getId()).listEffective(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scopeId.get()).getScopeMappings().clientLevel(realmAccessClient.getId()).add(List.of()),
                Resource.CLIENT, true);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scopeId.get()).getScopeMappings().clientLevel(realmAccessClient.getId()).remove(List.of()),
                Resource.CLIENT, true);

        // this should throw forbidden as "query-users" role isn't enough
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clientScopes().findAll(),
                clients.get(AdminRoles.QUERY_USERS), false);
    }

    /**
     * Tests create/update permissions for client-scope representations that include protocolMappers.
     *
     * <p>manage-realm users are always permitted. manage-clients users are permitted only when every
     * mapper type in the representation that is new or changed is listed in
     * {@link Constants#ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS}.
     * Unchanged mappers already persisted on the scope are not checked.
     */
    @Test
    public void clientScopeProtocolMappers() {
        ProtocolMapperRepresentation allowedScopeMapper = new ProtocolMapperRepresentation();
        allowedScopeMapper.setName("test-scope-mapper");
        allowedScopeMapper.setProtocol("openid-connect");
        allowedScopeMapper.setProtocolMapper("oidc-usermodel-property-mapper");
        allowedScopeMapper.setConfig(Map.of("user.attribute", "username", "claim.name", "preferred_username"));

        // --- Create: baseline (no allowlist) → manage-clients always denied for mapper writes ---
        InvocationWithResponse createScopeWithMappers = (realm, response) -> {
            ClientScopeRepresentation rep = new ClientScopeRepresentation();
            rep.setName(UUID.randomUUID().toString());
            rep.setProtocol("openid-connect");
            rep.setProtocolMappers(List.of(allowedScopeMapper));
            response.set(realm.clientScopes().create(rep));
            if (response.get().getStatus() == Response.Status.CREATED.getStatusCode()) {
                String createdId = ApiUtil.getCreatedId(response.get());
                realm.clientScopes().get(createdId).remove();
            }
        };
        invoke(createScopeWithMappers, clients.get("none"), false);
        invoke(createScopeWithMappers, clients.get("master-none"), false);
        invoke(createScopeWithMappers, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(createScopeWithMappers, clients.get("master-admin"), true);
        // "multi" has manage-realm and view-clients, but lacks manage-clients
        invoke(createScopeWithMappers, clients.get("multi"), false);
        // manage-clients only: passes requireManageClientScopes() but mapper type not in (empty) allowlist → 403
        invoke(createScopeWithMappers, clients.get(AdminRoles.MANAGE_CLIENTS), false);
        // manage-realm only: fails requireManageClientScopes() before reaching the mapper check
        invoke(createScopeWithMappers, clients.get(AdminRoles.MANAGE_REALM), false);

        // --- Create: allowlist configured → manage-clients allowed for listed types ---
        managedRealm1.updateWithCleanup(r -> r.attribute(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS,
                "oidc-usermodel-property-mapper"));
        invoke(createScopeWithMappers, clients.get(AdminRoles.MANAGE_CLIENTS), true);
        invoke(createScopeWithMappers, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(createScopeWithMappers, clients.get("master-admin"), true);

        ProtocolMapperRepresentation disallowedScopeMapper = new ProtocolMapperRepresentation();
        disallowedScopeMapper.setName("disallowed-scope-mapper");
        disallowedScopeMapper.setProtocol("openid-connect");
        disallowedScopeMapper.setProtocolMapper("oidc-acr-mapper");
        InvocationWithResponse createScopeWithDisallowedMapper = (realm, response) -> {
            ClientScopeRepresentation rep = new ClientScopeRepresentation();
            rep.setName(UUID.randomUUID().toString());
            rep.setProtocol("openid-connect");
            rep.setProtocolMappers(List.of(disallowedScopeMapper));
            response.set(realm.clientScopes().create(rep));
            if (response.get().getStatus() == Response.Status.CREATED.getStatusCode()) {
                String createdId = ApiUtil.getCreatedId(response.get());
                realm.clientScopes().get(createdId).remove();
            }
        };
        // type NOT in allowlist → manage-clients denied
        invoke(createScopeWithDisallowedMapper, clients.get(AdminRoles.MANAGE_CLIENTS), false);
        invoke(createScopeWithDisallowedMapper, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(createScopeWithDisallowedMapper, clients.get("master-admin"), true);

        // Clear the allowlist so the Update-baseline block below runs without it
        RealmRepresentation scopeRealmRep = managedRealm1.admin().toRepresentation();
        scopeRealmRep.getAttributes().remove(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS);
        managedRealm1.admin().update(scopeRealmRep);

        // Create a fresh empty scope (no pre-existing protocol mappers) for the update tests.
        ClientScopeRepresentation freshScope = new ClientScopeRepresentation();
        freshScope.setName("pm-update-test-scope");
        freshScope.setProtocol("openid-connect");
        Response scopeCreateResponse = managedRealm1.admin().clientScopes().create(freshScope);
        String scopeId = ApiUtil.getCreatedId(scopeCreateResponse);
        managedRealm1.cleanup().add(r -> r.clientScopes().get(scopeId).remove());

        // --- Update: baseline (no allowlist) → manage-clients always denied when mapper is new ---
        ClientScopeRepresentation scopeWithNewMapper = managedRealm1.admin().clientScopes().get(scopeId).toRepresentation();
        scopeWithNewMapper.setProtocolMappers(List.of(allowedScopeMapper));
        Invocation updateScopeWithNewMapper = realm -> realm.clientScopes().get(scopeId).update(scopeWithNewMapper);

        invoke(updateScopeWithNewMapper, clients.get("none"), false);
        invoke(updateScopeWithNewMapper, clients.get("master-none"), false);
        invoke(updateScopeWithNewMapper, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(updateScopeWithNewMapper, clients.get("master-admin"), true);
        // "multi" has manage-realm and view-clients, but lacks manage-clients
        invoke(updateScopeWithNewMapper, clients.get("multi"), false);
        // manage-clients only: passes requireManageClientScopes() but mapper type not in (empty) allowlist → 403
        invoke(updateScopeWithNewMapper, clients.get(AdminRoles.MANAGE_CLIENTS), false);
        // manage-realm only: fails requireManageClientScopes() before reaching the mapper check
        invoke(updateScopeWithNewMapper, clients.get(AdminRoles.MANAGE_REALM), false);

        // --- Update: allowlist configured → manage-clients allowed/denied based on what actually changed ---
        managedRealm1.updateWithCleanup(r -> r.attribute(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS,
                "oidc-usermodel-property-mapper"));

        // New mapper whose type is in the allowlist → manage-clients allowed
        invoke(updateScopeWithNewMapper, clients.get(AdminRoles.MANAGE_CLIENTS), true);
        invoke(updateScopeWithNewMapper, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(updateScopeWithNewMapper, clients.get("master-admin"), true);

        // New mapper whose type is NOT in the allowlist → manage-clients denied
        ClientScopeRepresentation scopeWithDisallowedMapper = managedRealm1.admin().clientScopes().get(scopeId).toRepresentation();
        ProtocolMapperRepresentation disallowedScopeMapper2 = new ProtocolMapperRepresentation();
        disallowedScopeMapper2.setName("disallowed-scope-mapper");
        disallowedScopeMapper2.setProtocol("openid-connect");
        disallowedScopeMapper2.setProtocolMapper("oidc-acr-mapper");
        scopeWithDisallowedMapper.setProtocolMappers(List.of(disallowedScopeMapper2));
        Invocation updateScopeWithNewDisallowedMapper = realm -> realm.clientScopes().get(scopeId).update(scopeWithDisallowedMapper);
        invoke(updateScopeWithNewDisallowedMapper, clients.get(AdminRoles.MANAGE_CLIENTS), false);
        invoke(updateScopeWithNewDisallowedMapper, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(updateScopeWithNewDisallowedMapper, clients.get("master-admin"), true);

        // Pre-persist a disallowed-type mapper on the scope via admin.
        ProtocolMapperRepresentation persistedDisallowedScopeMapper = new ProtocolMapperRepresentation();
        persistedDisallowedScopeMapper.setName("persisted-disallowed-scope-mapper");
        persistedDisallowedScopeMapper.setProtocol("openid-connect");
        persistedDisallowedScopeMapper.setProtocolMapper("oidc-acr-mapper");
        managedRealm1.admin().clientScopes().get(scopeId).getProtocolMappers()
                .createMapper(persistedDisallowedScopeMapper);

        // Fetch scope with its now-persisted mapper so the representation matches the DB exactly.
        ClientScopeRepresentation scopeWithPersistedMapper = managedRealm1.admin().clientScopes().get(scopeId).toRepresentation();

        // Unchanged disallowed-type mapper: no field differs → no type check fires → manage-clients allowed
        Invocation updateScopeWithUnchangedDisallowedMapper = realm ->
                realm.clientScopes().get(scopeId).update(scopeWithPersistedMapper);
        invoke(updateScopeWithUnchangedDisallowedMapper, clients.get(AdminRoles.MANAGE_CLIENTS), true);

        // Changed disallowed-type mapper: config modified → type check fires → manage-clients denied
        ClientScopeRepresentation scopeWithChangedDisallowedMapper = managedRealm1.admin().clientScopes().get(scopeId).toRepresentation();
        scopeWithChangedDisallowedMapper.getProtocolMappers().stream()
                .filter(m -> "persisted-disallowed-scope-mapper".equals(m.getName()))
                .forEach(m -> m.setConfig(Map.of("some.config", "changed")));
        Invocation updateScopeWithChangedDisallowedMapper = realm ->
                realm.clientScopes().get(scopeId).update(scopeWithChangedDisallowedMapper);
        invoke(updateScopeWithChangedDisallowedMapper, clients.get(AdminRoles.MANAGE_CLIENTS), false);
    }


    /**
     * Tests create/update/delete permissions for protocol mappers on client scopes via ProtocolMappersResource.
     *
     * <p>The realm attribute {@link Constants#ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS}
     * is set to {@code oidc-acr-mapper,oidc-amr-mapper} for the duration of the test.
     * Users with {@code manage-realm} are always permitted regardless of the mapper type.
     * Users with {@code manage-clients} (but not {@code manage-realm}) are permitted only when the
     * mapper type is explicitly in the allowed list; otherwise they are denied.
     * Users with {@code manage-realm} only are additionally blocked by the {@code ClientScopesResource}
     * routing guard ({@code requireListClientScopes}) regardless of the mapper type.
     */
    @Test
    public void clientScopeProtocolMappersResource() {
        // Use a dedicated scope so that mappers created/left behind during the test do not
        // pollute any pre-existing client scope and are automatically removed via cleanup().
        ClientScopeRepresentation dedicatedScope = new ClientScopeRepresentation();
        dedicatedScope.setName("pm-resource-test-scope");
        dedicatedScope.setProtocol("openid-connect");
        Response dedicatedScopeResponse = managedRealm1.admin().clientScopes().create(dedicatedScope);
        String dedicatedScopeId = ApiUtil.getCreatedId(dedicatedScopeResponse);
        managedRealm1.cleanup().add(r -> r.clientScopes().get(dedicatedScopeId).remove());
        ClientScopeRepresentation scope = managedRealm1.admin().clientScopes().get(dedicatedScopeId).toRepresentation();

        // Configure the allowed-types list on the realm.
        managedRealm1.updateWithCleanup(r -> r.attribute(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS,
                "oidc-acr-mapper,oidc-amr-mapper"));
        // --- Allowed mapper type: both realm-admin and manage-clients users are permitted ---
            ProtocolMapperRepresentation allowedMapper = new ProtocolMapperRepresentation();
            allowedMapper.setName("allowed-mapper");
            allowedMapper.setProtocol("openid-connect");
            allowedMapper.setProtocolMapper("oidc-acr-mapper");

            // createMapper (bulk) and update perform the auth check before the existence check,
            // so denied callers receive 403 even when the mapper id does not exist.
            for (Invocation writeOp : List.<Invocation>of(
                    realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(List.of(allowedMapper)),
                    realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().update("nosuch", allowedMapper))) {
                invoke(writeOp, clients.get("none"), false);
                invoke(writeOp, clients.get("master-none"), false);
                invoke(writeOp, clients.get(AdminRoles.REALM_ADMIN), true);
                invoke(writeOp, clients.get("master-admin"), true);
                // "multi" has manage-realm and view-clients, but lacks manage-clients
                invoke(writeOp, clients.get("multi"), false);
                // manage-clients: routing guard passes, managePermission passes, type is in allowlist
                invoke(writeOp, clients.get(AdminRoles.MANAGE_CLIENTS), true);
                // manage-realm only: fails the ClientScopesResource routing guard (requireListClientScopes)
                invoke(writeOp, clients.get(AdminRoles.MANAGE_REALM), false);
            }
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get("none"), false);
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get("master-none"), false);
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get(AdminRoles.REALM_ADMIN), true);
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get("master-admin"), true);
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get("multi"), false);
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get(AdminRoles.MANAGE_CLIENTS), true);
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(allowedMapper)),
                    clients.get(AdminRoles.MANAGE_REALM), false);

            // delete resolves the mapper model before the auth check, so a non-existent id always
            // yields 404. Test delete only with permitted callers; denied callers are covered above.
            invoke(realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().delete("nosuch"),
                    clients.get(AdminRoles.REALM_ADMIN), true);
            invoke(realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().delete("nosuch"),
                    clients.get("master-admin"), true);
            invoke(realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().delete("nosuch"),
                    clients.get(AdminRoles.MANAGE_CLIENTS), true);

            // --- Disallowed mapper type: only realm-admin is permitted; manage-clients is denied ---
            ProtocolMapperRepresentation disallowedMapper = new ProtocolMapperRepresentation();
            disallowedMapper.setName("disallowed-mapper");
            disallowedMapper.setProtocol("openid-connect");
            disallowedMapper.setProtocolMapper("oidc-usermodel-property-mapper");

            // createMapper (bulk) and update perform the auth check before the existence check.
            for (Invocation writeOp : List.<Invocation>of(
                    realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(List.of(disallowedMapper)),
                    realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().update("nosuch", disallowedMapper))) {
                invoke(writeOp, clients.get("none"), false);
                invoke(writeOp, clients.get("master-none"), false);
                invoke(writeOp, clients.get(AdminRoles.REALM_ADMIN), true);
                invoke(writeOp, clients.get("master-admin"), true);
                // "multi" has manage-realm and view-clients, but lacks manage-clients
                invoke(writeOp, clients.get("multi"), false);
                // manage-clients: routing guard passes, but type is not in allowlist
                invoke(writeOp, clients.get(AdminRoles.MANAGE_CLIENTS), false);
                // manage-realm only: fails the ClientScopesResource routing guard
                invoke(writeOp, clients.get(AdminRoles.MANAGE_REALM), false);
            }
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(disallowedMapper)),
                    clients.get(AdminRoles.MANAGE_CLIENTS), false);
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(disallowedMapper)),
                    clients.get(AdminRoles.REALM_ADMIN), true);
            invoke((realm, response) ->
                            response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(disallowedMapper)),
                    clients.get("master-admin"), true);

            // --- Delete authorization: guard is only reached for existing mappers ---
            // manage-clients may delete an allowed-type mapper.
            ProtocolMapperRepresentation storedScopeAllowed = new ProtocolMapperRepresentation();
            storedScopeAllowed.setName("stored-allowed-scope-mapper");
            storedScopeAllowed.setProtocol("openid-connect");
            storedScopeAllowed.setProtocolMapper("oidc-acr-mapper");
            String storedScopeAllowedId = ApiUtil.getCreatedId(
                    managedRealm1.admin().clientScopes().get(scope.getId()).getProtocolMappers().createMapper(storedScopeAllowed));
            invoke(realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().delete(storedScopeAllowedId),
                    clients.get(AdminRoles.MANAGE_CLIENTS), true);

            // manage-clients must not delete a disallowed-type mapper.
            ProtocolMapperRepresentation storedScopeDisallowed = new ProtocolMapperRepresentation();
            storedScopeDisallowed.setName("stored-disallowed-scope-mapper");
            storedScopeDisallowed.setProtocol("openid-connect");
            storedScopeDisallowed.setProtocolMapper("oidc-usermodel-property-mapper");
            storedScopeDisallowed.setConfig(Map.of("user.attribute", "username", "claim.name", "preferred_username"));
            String storedScopeDisallowedId = ApiUtil.getCreatedId(
                    managedRealm1.admin().clientScopes().get(scope.getId()).getProtocolMappers().createMapper(storedScopeDisallowed));
        invoke(realm -> realm.clientScopes().get(scope.getId()).getProtocolMappers().delete(storedScopeDisallowedId),
                clients.get(AdminRoles.MANAGE_CLIENTS), false);
    }

    @Test
    public void clientInitialAccess() {
        invoke(realm -> realm.clientInitialAccess().list(), Resource.CLIENT, false);
        invoke(realm -> realm.clientInitialAccess().create(new ClientInitialAccessCreatePresentation()), Resource.CLIENT, true);
        invoke(realm -> realm.clientInitialAccess().delete("nosuch"), Resource.CLIENT, true);
    }

    @Test
    public void roles() {
        RoleRepresentation newRole = RoleBuilder.create().name("sample-role").build();
        managedRealm1.admin().roles().create(newRole);
        managedRealm1.cleanup().add(r -> r.roles().deleteRole("sample-role"));

        invoke(realm -> realm.roles().list(), Resource.REALM, false, true);

        // this should throw forbidden as "create-client" role isn't enough
        invoke(realm -> clients.get(AdminRoles.CREATE_CLIENT).realm(REALM_NAME).roles().list(),
                clients.get(AdminRoles.CREATE_CLIENT), false);
        for (String role : AdminRoles.ALL_QUERY_ROLES) {
            invoke(realm -> realm.roles().list(), clients.get(role), false);
        }
        invoke(realm -> realm.roles().get("sample-role").toRepresentation(),
                Resource.REALM, false);
        invoke(realm -> realm.roles().get("sample-role").update(newRole),
                Resource.REALM, true);
        invoke(realm -> realm.roles().create(new RoleRepresentation()),
                Resource.REALM, true);
        invoke(realm -> {
            realm.roles().deleteRole("sample-role");
            // need to recreate for other tests
            realm.roles().create(newRole);
        }, Resource.REALM, true);
        invoke(realm -> realm.roles().get("sample-role").getRoleComposites(), Resource.REALM, false);
        invoke(realm -> realm.roles().get("sample-role").addComposites(List.of()), Resource.REALM, true);
        invoke(realm -> realm.roles().get("sample-role").deleteComposites(List.of()), Resource.REALM, true);
        invoke(realm -> realm.roles().get("sample-role").getRoleComposites(), Resource.REALM, false);
        invoke(realm -> realm.roles().get("sample-role").getRealmRoleComposites(), Resource.REALM, false);
        invoke(realm -> realm.roles().get("sample-role").getClientRoleComposites(KeycloakModelUtils.generateId()), Resource.REALM, false);
        invoke(realm -> realm.roles().list(), clients.get(AdminRoles.MANAGE_IDENTITY_PROVIDERS), true);
    }

    @Test
    public void rolesById() {
        RoleRepresentation newRole = RoleBuilder.create().name("role-by-id").build();
        managedRealm1.admin().roles().create(newRole);
        RoleRepresentation role = managedRealm1.admin().roles().get("role-by-id").toRepresentation();
        managedRealm1.cleanup().add(r -> r.roles().deleteRole("role-by-id"));

        invoke(realm -> realm.rolesById().getRole(role.getId()), Resource.REALM, false, true);
        invoke(realm -> realm.rolesById().updateRole(role.getId(), role), Resource.REALM, true);
        invoke(realm -> {
            realm.rolesById().deleteRole(role.getId());
            // need to recreate for other tests
            realm.roles().create(newRole);
            RoleRepresentation temp = realm.roles().get("role-by-id").toRepresentation();
            role.setId(temp.getId());
        }, Resource.REALM, true);
        invoke(realm -> realm.rolesById().getRoleComposites(role.getId()), Resource.REALM, false, true);
        invoke(realm -> realm.rolesById().addComposites(role.getId(), List.of()), Resource.REALM, true);
        invoke(realm -> realm.rolesById().deleteComposites(role.getId(), List.of()), Resource.REALM, true);
        invoke(realm -> realm.rolesById().getRoleComposites(role.getId()), Resource.REALM, false, true);
        invoke(realm -> realm.rolesById().getRealmRoleComposites(role.getId()), Resource.REALM, false, true);
        invoke(realm -> realm.rolesById().getClientRoleComposites(role.getId(), KeycloakModelUtils.generateId()), Resource.REALM, false, true);
    }

    @Test
    public void groups() {
        invoke(realm -> realm.groups().groups(), Resource.USER, false);
        invoke(realm -> realm.groups().count(), Resource.USER, false);
        invoke((realm, response) -> {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("mygroup");
            response.set(realm.groups().add(group));
        }, Resource.USER, true);

        GroupRepresentation group = managedRealm1.admin().getGroupByPath("mygroup");
        ClientRepresentation realmAccessClient = managedRealm1.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);

        // this should throw forbidden as "create-client" role isn't enough
        invoke(realm -> clients.get(AdminRoles.CREATE_CLIENT).realm(REALM_NAME).groups().groups(),
                clients.get(AdminRoles.CREATE_CLIENT), false);

        invoke(realm -> realm.groups().group(group.getId()).toRepresentation(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).update(group), Resource.USER, true);
        invoke(realm -> realm.groups().group(group.getId()).members(0, 100), Resource.USER, false);
        invoke((realm, response) -> {
            GroupRepresentation subgroup = new GroupRepresentation();
            subgroup.setName("sub");
            response.set(realm.groups().group(group.getId()).subGroup(subgroup));
        }, Resource.USER, true);

        invoke(realm -> realm.groups().group(group.getId()).roles().getAll(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().listAll(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().listEffective(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().listAvailable(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().add(List.of()), Resource.USER, true);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().remove(List.of()), Resource.USER, true);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).listAll(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).listEffective(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).listAvailable(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).add(List.of()), Resource.USER, true);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).remove(List.of()), Resource.USER, true);
        invoke(realm -> {
            realm.groups().group(group.getId()).remove();
            group.setId(null);
            realm.groups().add(group);
            GroupRepresentation temp = realm.getGroupByPath("mygroup");
            group.setId(temp.getId());
        }, Resource.USER, true);
    }

    // Permissions for impersonation tested in ImpersonationTest
    @Test
    public void users() {
        invoke((realm, response) ->
                        response.set(realm.users().create(UserBuilder.create().username("testuser").build())),
                Resource.USER, true);
        UserRepresentation user = managedRealm1.admin().users().search("testuser").get(0);
        invoke(realm -> {
            realm.users().get(user.getId()).remove();
            realm.users().create(user);
            UserRepresentation temp = realm.users().search("testuser").get(0);
            user.setId(temp.getId());
        }, Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).toRepresentation(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).getUnmanagedAttributes(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).update(user), Resource.USER, true);
        invoke(realm -> realm.users().count(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).getUserSessions(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).getOfflineSessions(KeycloakModelUtils.generateId()), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).getFederatedIdentity(), Resource.USER, false);
        invoke((realm, response) -> response.set(realm.users().get(user.getId()).addFederatedIdentity("nosuch",
                        FederatedIdentityBuilder.create().identityProvider("nosuch").userId("nosuch").userName("nosuch").build()
                )
        ), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).removeFederatedIdentity("nosuch"), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).getConsents(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).revokeConsent("testclient"), Resource.USER, true);

        invoke(realm -> realm.users().get(user.getId()).verifiableCredentials().getCredentials(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).verifiableCredentials().getIssuedCredentials(), Resource.USER, false);
        UserVerifiableCredentialRepresentation verifCred = new UserVerifiableCredentialRepresentation();
        verifCred.setCredentialScopeName("nosuch");
        invoke(realm -> realm.users().get(user.getId()).verifiableCredentials().createCredential(verifCred), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).verifiableCredentials().updateCredential("nosuch"), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).verifiableCredentials().revokeCredential("nosuch"), Resource.USER, true);

        invoke(realm -> realm.users().get(user.getId()).logout(), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).resetPassword(CredentialBuilder.password("password").build()),
                Resource.USER, true);
        invoke(realm -> {
            CredentialRepresentation totpCredential = realm.users().get(user.getId()).credentials().stream()
                    .filter(c -> OTPCredentialModel.TYPE.equals(c.getType())).findFirst().orElse(null);
            if (totpCredential != null) {
                realm.users().get(user.getId()).removeCredential(totpCredential.getId());
            } else {
                realm.users().get(user.getId()).removeCredential("123");
            }
        }, Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).disableCredentialType(List.of(CredentialRepresentation.PASSWORD)),
                Resource.USER, true);
        invoke(realm -> {
            CredentialRepresentation cred = realm.users().get(user.getId()).credentials().stream().findFirst().orElse(null);
            if (cred != null) {
                realm.users().get(user.getId()).moveCredentialToFirst(cred.getId());
            } else {
                realm.users().get(user.getId()).moveCredentialToFirst("123");
            }
        }, Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).executeActionsEmail(List.of(UserModel.RequiredAction.UPDATE_PASSWORD.name())),
                Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).executeActionsEmail(List.of()), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).sendVerifyEmail(), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).groups(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).leaveGroup("nosuch"), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).joinGroup("nosuch"), Resource.USER, true);

        invoke(realm -> realm.users().get(user.getId()).roles().getAll(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().listAll(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().listAvailable(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().listEffective(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().add(List.of()), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().remove(List.of()), Resource.USER, true);

        ClientRepresentation realmAccessClient = managedRealm1.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).listAll(),
                Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).listAvailable(),
                Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).listEffective(),
                Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).add(List.of()),
                Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).remove(List.of()),
                Resource.USER, true);
        invoke(realm -> realm.users().search("foo", 0, 1), Resource.USER, false);
        // this should throw forbidden as "query-client" role isn't enough
        invoke(realm -> clients.get(AdminRoles.QUERY_CLIENTS).realm(REALM_NAME).users().list(),
                clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke((realm, response) -> response.set(clients.get(AdminRoles.QUERY_CLIENTS).realm(REALM_NAME).users().create(user)),
                clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke(realm -> clients.get(AdminRoles.QUERY_CLIENTS).realm(REALM_NAME).users().search("test"),
                clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke(realm -> realm.users().get(user.getId()).toRepresentation(), clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke(realm -> realm.users().get(user.getId()).remove(), clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke(realm -> realm.users().get(user.getId()).update(user), clients.get(AdminRoles.QUERY_CLIENTS), false);
        // users with query-user role should be able to query required actions so the user detail page can be rendered successfully when fine-grained permissions are enabled.
        invoke(realm -> realm.flows().getRequiredActions(), clients.get(AdminRoles.QUERY_USERS), true);
        invoke(realm -> clients.get(AdminRoles.VIEW_USERS).realm(REALM_NAME).users().get(user.getId()).getConfiguredUserStorageCredentialTypes(),
                clients.get(AdminRoles.VIEW_USERS), true);
        for (String role : Stream.of(AdminRoles.ALL_REALM_ROLES)
                .filter(Predicate.not(AdminRoles.VIEW_REALM::equals)
                .and(Predicate.not(AdminRoles.MANAGE_REALM::equals)
                .and(Predicate.not(AdminRoles.VIEW_USERS::equals)
                .and(Predicate.not(AdminRoles.MANAGE_USERS::equals)
                .and(Predicate.not(AdminRoles.QUERY_USERS::equals)))))).toList()) {
            invoke(realm -> realm.users().userProfile().getConfiguration(), clients.get(role), false);
            invoke(realm -> realm.users().userProfile().getMetadata(), clients.get(role), false);
        }
        for (String role : Stream.of(AdminRoles.VIEW_REALM, AdminRoles.MANAGE_REALM, AdminRoles.VIEW_USERS, AdminRoles.MANAGE_USERS, AdminRoles.QUERY_USERS).toList()) {
            invoke(realm -> realm.users().userProfile().getConfiguration(), clients.get(role), true);
            invoke(realm -> realm.users().userProfile().getMetadata(), clients.get(role), true);
        }
    }

    @Test
    public void realmRoleMappingsVisibleWithManageUsers() {
        String roleName = "role-mapping-visibility";
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        managedRealm1.admin().roles().create(role);
        managedRealm1.cleanup().add(r -> r.roles().deleteRole(roleName));
        RoleRepresentation createdRole = managedRealm1.admin().roles().get(roleName).toRepresentation();

        String userUuid = ApiUtil.getCreatedId(managedRealm1.admin().users()
                .create(UserBuilder.create().username("role-mapping-target").enabled(true).build()));
        managedRealm1.cleanup().add(r -> r.users().delete(userUuid).close());
        managedRealm1.admin().users().get(userUuid).roles().realmLevel().add(List.of(createdRole));

        String groupUuid = ApiUtil.getCreatedId(managedRealm1.admin().groups()
                .add(GroupBuilder.create().name("role-mapping-group").build()));
        managedRealm1.cleanup().add(r -> r.groups().group(groupUuid).remove());
        managedRealm1.admin().groups().group(groupUuid).roles().realmLevel().add(List.of(createdRole));

        String clientUuid = ApiUtil.getCreatedId(managedRealm1.admin().clients()
                .create(ClientBuilder.create().clientId("role-mapping-client").build()));
        managedRealm1.cleanup().add(r -> r.clients().get(clientUuid).remove());
        String clientRoleName = "client-role-mapping-visibility";
        managedRealm1.admin().clients().get(clientUuid).roles().create(RoleBuilder.create().name(clientRoleName).build());
        RoleRepresentation clientRole = managedRealm1.admin().clients().get(clientUuid).roles().get(clientRoleName).toRepresentation();
        managedRealm1.admin().users().get(userUuid).roles().clientLevel(clientUuid).add(List.of(clientRole));
        managedRealm1.admin().groups().group(groupUuid).roles().clientLevel(clientUuid).add(List.of(clientRole));

        RealmResource manageUsers = clients.get(AdminRoles.MANAGE_USERS).realm(REALM_NAME);

        MappingsRepresentation userMappings = manageUsers.users().get(userUuid).roles().getAll();
        assertThat(userMappings.getRealmMappings(), Matchers.notNullValue());
        assertThat(userMappings.getRealmMappings().stream().map(RoleRepresentation::getName).toList(),
                Matchers.hasItem(roleName));

        MappingsRepresentation groupMappings = manageUsers.groups().group(groupUuid).roles().getAll();
        assertThat(groupMappings.getRealmMappings(), Matchers.notNullValue());
        assertThat(groupMappings.getRealmMappings().stream().map(RoleRepresentation::getName).toList(),
                Matchers.hasItem(roleName));

        // client-level mappings without view-clients follow the same rule
        assertThat(roleNames(manageUsers.users().get(userUuid).roles().clientLevel(clientUuid).listAll()), Matchers.hasItem(clientRoleName));
        assertThat(roleNames(manageUsers.users().get(userUuid).roles().clientLevel(clientUuid).listEffective()), Matchers.hasItem(clientRoleName));
        assertThat(roleNames(manageUsers.groups().group(groupUuid).roles().clientLevel(clientUuid).listAll()), Matchers.hasItem(clientRoleName));
        assertThat(roleNames(manageUsers.groups().group(groupUuid).roles().clientLevel(clientUuid).listEffective()), Matchers.hasItem(clientRoleName));

        // the group representation follows the same rule
        GroupRepresentation groupRep = manageUsers.groups().group(groupUuid).toRepresentation();
        assertThat(groupRep.getRealmRoles(), Matchers.hasItem(roleName));
        assertThat(groupRep.getClientRoles(), Matchers.hasKey("role-mapping-client"));
        assertThat(groupRep.getClientRoles().get("role-mapping-client"), Matchers.hasItem(clientRoleName));

        RealmResource viewUsers = clients.get(AdminRoles.VIEW_USERS).realm(REALM_NAME);

        assertThat(realmRoleNames(viewUsers.users().get(userUuid).roles().getAll()),
                Matchers.not(Matchers.hasItem(roleName)));
        assertThat(realmRoleNames(viewUsers.groups().group(groupUuid).roles().getAll()),
                Matchers.not(Matchers.hasItem(roleName)));
        assertThat(viewUsers.users().get(userUuid).roles().clientLevel(clientUuid).listAll(), Matchers.empty());
        assertThat(viewUsers.users().get(userUuid).roles().clientLevel(clientUuid).listEffective(), Matchers.empty());
        assertThat(viewUsers.groups().group(groupUuid).roles().clientLevel(clientUuid).listAll(), Matchers.empty());
        assertThat(viewUsers.groups().group(groupUuid).roles().clientLevel(clientUuid).listEffective(), Matchers.empty());
        GroupRepresentation viewUsersGroupRep = viewUsers.groups().group(groupUuid).toRepresentation();
        assertThat(viewUsersGroupRep.getRealmRoles() == null ? List.<String>of() : viewUsersGroupRep.getRealmRoles(),
                Matchers.not(Matchers.hasItem(roleName)));
        assertThat(viewUsersGroupRep.getClientRoles() == null ? Map.<String, List<String>>of() : viewUsersGroupRep.getClientRoles(),
                Matchers.not(Matchers.hasKey("role-mapping-client")));

        // being able to view (and map) the role does not grant management of the role itself
        String roleId = createdRole.getId();
        assertThat(manageUsers.rolesById().getRole(roleId).getName(), Matchers.is(roleName));
        assertThrows(ForbiddenException.class, () -> manageUsers.rolesById().updateRole(roleId, createdRole));
        assertThrows(ForbiddenException.class, () -> manageUsers.rolesById().addComposites(roleId, List.of()));
        assertThrows(ForbiddenException.class, () -> manageUsers.rolesById().deleteComposites(roleId, List.of()));
        assertThrows(ForbiddenException.class, () -> manageUsers.rolesById().deleteRole(roleId));
        assertThrows(ForbiddenException.class, () -> manageUsers.roles().get(roleName).update(createdRole));
        assertThrows(ForbiddenException.class, () -> manageUsers.roles().get(roleName).addComposites(List.of()));
        assertThrows(ForbiddenException.class, () -> manageUsers.roles().get(roleName).deleteComposites(List.of()));
        assertThrows(ForbiddenException.class, () -> manageUsers.roles().deleteRole(roleName));
    }

    private static List<String> realmRoleNames(MappingsRepresentation mappings) {
        return mappings.getRealmMappings() == null ? List.of()
                : mappings.getRealmMappings().stream().map(RoleRepresentation::getName).toList();
    }

    private static List<String> roleNames(List<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName).toList();
    }

    @Test
    public void identityProviders() {
        invoke(realm -> realm.identityProviders().findAll(), Resource.IDENTITY_PROVIDER, false);
        invoke((realm, response) -> response.set(realm.identityProviders().create(
                                IdentityProviderBuilder.create()
                                        .providerId("oidc")
                                        .displayName("nosuch-foo")
                                        .alias("foo")
                                        .attribute("clientId", "foo")
                                        .attribute("clientSecret", "foo")
                                        .build()
                        )
                ), Resource.IDENTITY_PROVIDER, true);
        invoke(realm -> realm.identityProviders().get("nosuch").toRepresentation(), Resource.IDENTITY_PROVIDER, false);
        invoke(realm -> realm.identityProviders().get("nosuch").update(new IdentityProviderRepresentation()), Resource.IDENTITY_PROVIDER, true);
        invoke((realm, response) -> response.set(realm.identityProviders().get("nosuch").export("saml")), Resource.IDENTITY_PROVIDER, false);
        invoke(realm -> realm.identityProviders().get("nosuch").remove(), Resource.IDENTITY_PROVIDER, true);

        invoke((realm, response) -> response.set(realm.identityProviders().get("nosuch").addMapper(new IdentityProviderMapperRepresentation())), Resource.IDENTITY_PROVIDER, true);
        invoke(realm -> realm.identityProviders().get("nosuch").delete("nosuch"), Resource.IDENTITY_PROVIDER, true);
        invoke(realm -> realm.identityProviders().get("nosuch").getMappers(), Resource.IDENTITY_PROVIDER, false);
        invoke(realm -> realm.identityProviders().get("nosuch").getMapperById("nosuch"), Resource.IDENTITY_PROVIDER, false);
        invoke(realm -> realm.identityProviders().get("nosuch").getMapperTypes(), Resource.IDENTITY_PROVIDER, false);

        invoke(realm -> realm.identityProviders().importFrom(Map.of()), Resource.IDENTITY_PROVIDER, true);
        invoke(realm -> realm.identityProviders().importFrom(null), Resource.IDENTITY_PROVIDER, true);
    }

    @Test
    public void components() {
        invoke(realm -> realm.components().query(), Resource.REALM, false);
        invoke(realm -> realm.components().query("nosuch"), Resource.REALM, false);
        invoke(realm -> realm.clientRegistrationPolicy().getProviders(), Resource.REALM, false);
        invoke((realm, response) -> response.set(realm.components().add(new ComponentRepresentation())), Resource.REALM, true);
        invoke(realm -> realm.components().component("nosuch").toRepresentation(), Resource.REALM, false);
        invoke(realm -> realm.components().component("nosuch").update(new ComponentRepresentation()), Resource.REALM, true);
        invoke(realm -> realm.components().component("nosuch").remove(), Resource.REALM, true);
    }

    @Test
    public void partialExport() {
        invoke(realm -> realm.partialExport(false, false), clients.get("view-realm"), false);
        invoke(realm -> realm.partialExport(false, false), clients.get("manage-realm"), true);
        invoke(realm -> realm.partialExport(true, false), clients.get("manage-realm"), false);
        invoke(realm -> realm.partialExport(false, true), clients.get("manage-realm"), false);
        invoke(realm -> realm.partialExport(true, true), clients.get("multi"), true);
        invoke(realm -> realm.partialExport(false, false), clients.get("none"), false);
    }

    @Test
    public void localizations() {
        verifyAnyAdminRoleReqired(realm -> realm.localization().getRealmSpecificLocales());

        verifyAnyAdminRoleReqired(realm -> realm.localization().getRealmLocalizationText("en", "test"));

        verifyAnyAdminRoleReqired(realm -> realm.localization().getRealmLocalizationTexts("en"));
        verifyAnyAdminRoleReqired(realm -> realm.localization().getRealmLocalizationTexts("en", false));

        invoke(realm -> realm.localization().createOrUpdateRealmLocalizationTexts("en", Map.of()), clients.get("view-realm"), false);
        invoke(realm -> realm.localization().createOrUpdateRealmLocalizationTexts("en", Map.of()), clients.get("manage-realm"), true);
        invoke(realm -> realm.localization().createOrUpdateRealmLocalizationTexts("en", Map.of()), clients.get("master-admin"), true);
        invoke(realm -> realm.localization().createOrUpdateRealmLocalizationTexts("en", Map.of()), clients.get("none"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("REALM2"), false);

        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("view-realm"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("manage-realm"), true);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("master-admin"), true);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("none"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("REALM2"), false);

        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("view-realm"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("manage-realm"), true);
        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("master-admin"), true);
        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("none"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("REALM2"), false);
    }

    private void verifyAnyAdminRoleReqired(Invocation invocation) {
        invoke(invocation, clients.get("view-realm"), true);
        invoke(invocation, clients.get("manage-realm"), true);
        invoke(invocation, clients.get("multi"), true);
        invoke(invocation, clients.get("master-admin"), true);
        invoke(invocation, clients.get("none"), false);
        invoke(invocation, clients.get("REALM2"), false);
    }
}
