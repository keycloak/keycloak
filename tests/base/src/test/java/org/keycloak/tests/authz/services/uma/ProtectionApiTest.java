package org.keycloak.tests.authz.services.uma;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class ProtectionApiTest {

    private static final String RESOURCE_SERVER_ID = "resource-server";

    @InjectRealm(config = TestRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(config = TestResourceServerConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient resourceServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    private AuthzClient authzClient;

    @BeforeEach
    public void enableRemoteResourceManagement() {
        RoleRepresentation umaProtectionRole = resourceServer.admin().roles().get("uma_protection").toRepresentation();
        for (String username : List.of("alice", "marta")) {
            String userId = realm.admin().users().search(username).get(0).getId();
            realm.admin().users().get(userId).roles().clientLevel(resourceServer.getId()).add(List.of(umaProtectionRole));
        }

        ResourceServerRepresentation settings = resourceServer.admin().authorization().getSettings();
        settings.setAllowRemoteResourceManagement(true);
        resourceServer.admin().authorization().update(settings);

        authzClient = AuthzClient.create(new Configuration(keycloakUrls.getBase(), realm.getName(), resourceServer.getClientId(),
                Map.of("secret", resourceServer.getSecret()), httpClient));
    }

    @Test
    public void testRejectsClientSuppliedIdWithoutChangingExistingResource() {
        ProtectionResource alice = authzClient.protection("alice", "password");
        ResourceRepresentation existing = alice.resource().create(new ResourceRepresentation("alice-resource", Set.of()));

        ResourceRepresentation registration = new ResourceRepresentation("overwritten-resource", Set.of());
        registration.setId(existing.getId());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authzClient.protection("marta", "password").resource().create(registration));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), ((HttpResponseException) exception.getCause()).getStatusCode());
        assertEquals("alice-resource", alice.resource().findById(existing.getId()).getName());

        ResourceRepresentation created = authzClient.protection("marta", "password").resource()
                .create(new ResourceRepresentation("marta-resource", Set.of()));
        assertNotNull(created.getId());
    }

    // findById(), delete() and update() resolved the target policy by resource ownership alone, with no
    // check on the policy's own type or owner. The delegate backing these endpoints is constructed with a
    // null auth, so every administrative check in PolicyResourceService is skipped, leaving resource
    // ownership as the only gate. An administrator-authored permission governing a user-owned resource -
    // whether a non-uma policy type, or a uma policy an admin created without setting an owner - was
    // therefore readable, rewritable and deletable through the self-service API
    @Test
    public void testByIdEndpointsRejectAdministratorAuthoredPolicyOnOwnedResource() {
        ResourceRepresentation aliceResource = authzClient.protection("alice", "password").resource()
                .create(ownerManagedResource("alice-admin-policy-resource"));

        String noOwnerPolicyId = createAdminAuthoredPolicy(aliceResource.getId(), "uma", null);
        String wrongTypePolicyId = createAdminAuthoredPolicy(aliceResource.getId(), "role", "alice");

        for (String policyId : List.of(noOwnerPolicyId, wrongTypePolicyId)) {
            assertThrows(RuntimeException.class,
                    () -> authzClient.protection("alice", "password").policy(aliceResource.getId()).findById(policyId));

            UmaPermissionRepresentation permission = new UmaPermissionRepresentation();
            permission.setId(policyId);
            assertThrows(RuntimeException.class,
                    () -> authzClient.protection("alice", "password").policy(aliceResource.getId()).update(permission));

            assertThrows(RuntimeException.class,
                    () -> authzClient.protection("alice", "password").policy(aliceResource.getId()).delete(policyId));

            // untouched by the rejected operations, checked through the admin API
            assertNotNull(resourceServer.admin().authorization().policies().policy(policyId).toRepresentation());
        }
    }

    // a policy associated with no resource at all (an admin-managed policy, or one bound to a resource
    // type) has no resource owner to check the caller against, so it must be rejected rather than accepted
    @Test
    public void testDeleteAndFindByIdRejectPolicyWithoutResources() {
        String policyId = KeycloakModelUtils.generateId();
        String realmName = realm.getName();

        runOnServer.run(session -> createPolicyWithoutResources(session, realmName, policyId));

        for (String user : List.of("alice", "marta")) {
            RuntimeException deleteException = assertThrows(RuntimeException.class,
                    () -> authzClient.protection(user, "password").policy(policyId).delete(policyId));
            assertEquals(Status.NOT_FOUND.getStatusCode(), ((HttpResponseException) deleteException.getCause()).getStatusCode());

            RuntimeException findByIdException = assertThrows(RuntimeException.class,
                    () -> authzClient.protection(user, "password").policy(policyId).findById(policyId));
            assertEquals(Status.NOT_FOUND.getStatusCode(), ((HttpResponseException) findByIdException.getCause()).getStatusCode());
        }

        assertNotNull(resourceServer.admin().authorization().policies().policy(policyId).toRepresentation());
    }

    // a uma policy spanning more than one resource is rejected outright, even when the caller owns the
    // policy and one of its resources; checkRequest() only ever validates the first resource returned by
    // iteration order, so without this guard the owner could reach a policy that also governs a resource
    // owned by someone else
    @Test
    public void testByIdEndpointsRejectUmaPolicyWithMultipleResources() {
        ResourceRepresentation aliceResource = authzClient.protection("alice", "password").resource()
                .create(ownerManagedResource("alice-multi-resource-policy-resource"));
        ResourceRepresentation martaResource = authzClient.protection("marta", "password").resource()
                .create(ownerManagedResource("marta-multi-resource-policy-resource"));

        String policyId = createMultiResourceUmaPolicy(List.of(aliceResource.getId(), martaResource.getId()), "alice");

        assertThrows(RuntimeException.class,
                () -> authzClient.protection("alice", "password").policy(aliceResource.getId()).findById(policyId));

        assertThrows(RuntimeException.class,
                () -> authzClient.protection("alice", "password").policy(aliceResource.getId()).delete(policyId));

        UmaPermissionRepresentation permission = new UmaPermissionRepresentation();
        permission.setId(policyId);
        assertThrows(RuntimeException.class,
                () -> authzClient.protection("alice", "password").policy(aliceResource.getId()).update(permission));

        // untouched by the rejected operations, checked through the admin API
        assertNotNull(resourceServer.admin().authorization().policies().policy(policyId).toRepresentation());
    }

    // a user-managed permission is only ever created through the Protection API with a single resource
    // attached; multiple resources on a uma-type policy are only reachable by an administrator wiring one up
    // directly through the full admin API, which is not the expected shape for this policy type
    @Test
    public void testAdminApiRejectsUmaPolicyWithMultipleResources() {
        ResourceRepresentation resourceA = createAdminManagedResource("admin-resource-a");
        ResourceRepresentation resourceB = createAdminManagedResource("admin-resource-b");

        PolicyRepresentation representation = new PolicyRepresentation();
        representation.setName("admin-authored-uma-policy");
        representation.setType("uma");
        representation.setResources(Set.of(resourceA.getId(), resourceB.getId()));

        try (Response response = resourceServer.admin().authorization().policies().create(representation)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        assertNull(resourceServer.admin().authorization().policies().findByName("admin-authored-uma-policy"));
    }

    @Test
    public void testAdminApiRejectsAddingAnotherResourceToExistingUmaPolicy() {
        ResourceRepresentation resourceA = createAdminManagedResource("admin-resource-c");
        ResourceRepresentation resourceB = createAdminManagedResource("admin-resource-d");

        PolicyRepresentation representation = new PolicyRepresentation();
        representation.setName("single-resource-uma-policy");
        representation.setType("uma");
        representation.setResources(Set.of(resourceA.getId()));

        try (Response response = resourceServer.admin().authorization().policies().create(representation)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        PolicyRepresentation created = resourceServer.admin().authorization().policies().findByName("single-resource-uma-policy");
        created.setResources(Set.of(resourceA.getId(), resourceB.getId()));

        assertThrows(WebApplicationException.class,
                () -> resourceServer.admin().authorization().policies().policy(created.getId()).update(created));
    }

    // the stored policy type cannot be changed through the update representation, so the single-resource
    // validation must be enforced against the persisted policy type rather than the submitted one; otherwise
    // omitting the type on the update payload would bypass the check entirely
    @Test
    public void testAdminApiRejectsAddingAnotherResourceWhenTypeIsOmittedFromUpdate() {
        ResourceRepresentation resourceA = createAdminManagedResource("admin-resource-e");
        ResourceRepresentation resourceB = createAdminManagedResource("admin-resource-f");

        PolicyRepresentation representation = new PolicyRepresentation();
        representation.setName("single-resource-uma-policy-omitted-type");
        representation.setType("uma");
        representation.setResources(Set.of(resourceA.getId()));

        try (Response response = resourceServer.admin().authorization().policies().create(representation)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        PolicyRepresentation created = resourceServer.admin().authorization().policies().findByName("single-resource-uma-policy-omitted-type");

        PolicyRepresentation update = new PolicyRepresentation();
        update.setId(created.getId());
        update.setName(created.getName());
        update.setResources(Set.of(resourceA.getId(), resourceB.getId()));

        assertThrows(WebApplicationException.class,
                () -> resourceServer.admin().authorization().policies().policy(created.getId()).update(update));

        PolicyRepresentation stored = resourceServer.admin().authorization().policies().policy(created.getId()).toRepresentation("*");
        assertEquals(1, stored.getResources().size());
    }

    private ResourceRepresentation createAdminManagedResource(String name) {
        try (Response response = resourceServer.admin().authorization().resources().create(new ResourceRepresentation(name, Set.of()))) {
            return response.readEntity(ResourceRepresentation.class);
        }
    }

    private ResourceRepresentation ownerManagedResource(String name) {
        ResourceRepresentation resource = new ResourceRepresentation(name, Set.of());
        resource.setOwnerManagedAccess(true);
        return resource;
    }

    /**
     * Creates a policy of the given type, associated with the given resource, directly through the
     * authorization model rather than through the self-service protection API. This simulates a policy
     * created by an administrator through the Admin API, optionally with no owner set.
     */
    private String createAdminAuthoredPolicy(String resourceId, String type, String owner) {
        String policyId = KeycloakModelUtils.generateId();
        String realmName = realm.getName();

        runOnServer.run(session -> createAdminAuthoredPolicy(session, realmName, resourceId, type, owner, policyId));

        return policyId;
    }

    private static void createAdminAuthoredPolicy(KeycloakSession session, String realmName, String resourceId, String type, String owner, String policyId) {
        AuthorizationProvider provider = getAuthorizationProvider(session, realmName);
        ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(getResourceServerClient(session));
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        PolicyRepresentation representation = new PolicyRepresentation();
        representation.setId(policyId);
        representation.setName(policyId);
        representation.setType(type);

        Policy policy = provider.getStoreFactory().getPolicyStore().create(resourceServer, representation);

        if (owner != null) {
            policy.setOwner(session.users().getUserByUsername(session.getContext().getRealm(), owner).getId());
        }

        policy.addResource(resourceStore.findById(resourceServer, resourceId));
    }

    /**
     * Creates a uma-type policy associated with multiple resources directly through the authorization model,
     * simulating a policy an administrator wired up through the full admin API.
     */
    private String createMultiResourceUmaPolicy(List<String> resourceIds, String owner) {
        String policyId = KeycloakModelUtils.generateId();
        String realmName = realm.getName();

        runOnServer.run(session -> createMultiResourceUmaPolicy(session, realmName, resourceIds, owner, policyId));

        return policyId;
    }

    private static void createMultiResourceUmaPolicy(KeycloakSession session, String realmName, List<String> resourceIds, String owner, String policyId) {
        AuthorizationProvider provider = getAuthorizationProvider(session, realmName);
        ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(getResourceServerClient(session));
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        PolicyRepresentation representation = new PolicyRepresentation();
        representation.setId(policyId);
        representation.setName(policyId);
        representation.setType("uma");

        Policy policy = provider.getStoreFactory().getPolicyStore().create(resourceServer, representation);
        policy.setOwner(session.users().getUserByUsername(session.getContext().getRealm(), owner).getId());

        for (String resourceId : resourceIds) {
            policy.addResource(resourceStore.findById(resourceServer, resourceId));
        }
    }

    private static void createPolicyWithoutResources(KeycloakSession session, String realmName, String policyId) {
        AuthorizationProvider provider = getAuthorizationProvider(session, realmName);

        Policy policy = provider.getStoreFactory().getPolicyStore().create(
                provider.getStoreFactory().getResourceServerStore().findByClient(getResourceServerClient(session)),
                umaPolicyRepresentation(policyId));
        policy.setOwner(session.users().getUserByUsername(session.getContext().getRealm(), "alice").getId());
    }

    private static AuthorizationProvider getAuthorizationProvider(KeycloakSession session, String realmName) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        session.getContext().setRealm(realm);
        return session.getProvider(AuthorizationProvider.class);
    }

    private static ClientModel getResourceServerClient(KeycloakSession session) {
        return session.getContext().getRealm().getClientByClientId(RESOURCE_SERVER_ID);
    }

    private static UmaPermissionRepresentation umaPolicyRepresentation(String policyId) {
        UmaPermissionRepresentation representation = new UmaPermissionRepresentation();
        representation.setId(policyId);
        representation.setName(policyId);
        return representation;
    }

    public static class TestRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .users(UserBuilder.create("alice").name("Alice", "Test").email("alice@example.org").emailVerified(true).password("password"))
                    .users(UserBuilder.create("marta").name("Marta", "Test").email("marta@example.org").emailVerified(true).password("password"));
        }
    }

    public static class TestResourceServerConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(RESOURCE_SERVER_ID)
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .directAccessGrantsEnabled();
        }
    }
}
