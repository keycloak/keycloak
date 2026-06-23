package org.keycloak.tests.admin.user;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.UserVerifiableCredentialModel;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.jpa.entity.FederatedUser;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
@DatabaseTest
public class FederatedUserVerifiableCredentialTest extends AbstractUserTest {

    private static final String CLIENT_SCOPE_NAME_1 = "driving-license-cert";
    private static final String CLIENT_SCOPE_NAME_2 = "education-cert";

    @InjectRealm(config = FederatedVcTestRealmConfig.class)
    protected ManagedRealm testRealm;

    @Test
    public void testAddVerifiableCredentialForFederatedUser() {
        String federatedUserId = createFederatedUser("fed-user-1");
        String expectedScopeId = resolveScopeId(CLIENT_SCOPE_NAME_1);

        runOnServer.run(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel("vc-1", expectedScopeId);
            vcModel.setRevision("rev-001");

            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(federatedUserId, vcModel);
            assertNotNull(added);
            assertNotNull(added.getRevision());
            assertNotNull(added.getCreatedDate());
            assertEquals(expectedScopeId, added.getClientScopeId());
        });

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> vcs = session.users()
                    .getVerifiableCredentialsByUser(federatedUserId)
                    .toList();

            assertEquals(1, vcs.size());
            assertEquals(expectedScopeId, vcs.get(0).getClientScopeId());
        });
    }

    @Test
    public void testGetVerifiableCredentialsForFederatedUser_EmptyList() {
        String federatedUserId = createFederatedUser("fed-user-empty");

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> vcs = session.users()
                    .getVerifiableCredentialsByUser(federatedUserId)
                    .toList();

            assertNotNull(vcs);
            assertTrue(vcs.isEmpty());
        });
    }

    @Test
    public void testRemoveVerifiableCredentialForFederatedUser() {
        String federatedUserId = createFederatedUser("fed-user-remove");

        String scopeId1 = resolveScopeId(CLIENT_SCOPE_NAME_1);
        String scopeId2 = resolveScopeId(CLIENT_SCOPE_NAME_2);

        runOnServer.run(session -> {
            session.users().addVerifiableCredential(federatedUserId,
                new UserVerifiableCredentialModel("vc-1", scopeId1));
            session.users().addVerifiableCredential(federatedUserId,
                new UserVerifiableCredentialModel("vc-2", scopeId2));
        });

        runOnServer.run(session -> {
            long count = session.users().getVerifiableCredentialsByUser(federatedUserId).count();
            assertEquals(2, count);
        });

        runOnServer.run(session -> {
            boolean removed = session.users().removeVerifiableCredential(federatedUserId, scopeId1);
            assertTrue(removed);
        });

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> remaining = session.users()
                    .getVerifiableCredentialsByUser(federatedUserId)
                    .toList();

            assertEquals(1, remaining.size());
            assertEquals(scopeId2, remaining.get(0).getClientScopeId());
        });
    }

    @Test
    public void testFederatedAndLocalUsersAreIsolated() {
        String localUserId = createUser("local-user", "local@test.com");
        String federatedUserId = createFederatedUser("fed-user-isolated");

        // Use existing test scopes - resolve both before adding credentials
        String localScopeId = resolveScopeId(CLIENT_SCOPE_NAME_1);
        String fedScopeId = resolveScopeId(CLIENT_SCOPE_NAME_2);

        runOnServer.run(session -> {
            session.users().addVerifiableCredential(localUserId,
                new UserVerifiableCredentialModel("local-vc-01", localScopeId));
            session.users().addVerifiableCredential(federatedUserId,
                new UserVerifiableCredentialModel("fed-vc-02", fedScopeId));
        });

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> localVcs = session.users()
                    .getVerifiableCredentialsByUser(localUserId)
                    .toList();

            assertEquals(1, localVcs.size());
            assertEquals(localScopeId, localVcs.get(0).getClientScopeId());
        });

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> fedVcs = session.users()
                    .getVerifiableCredentialsByUser(federatedUserId)
                    .toList();

            assertEquals(1, fedVcs.size());
            assertEquals(fedScopeId, fedVcs.get(0).getClientScopeId());
        });
    }

    @Test
    public void testCredentialsDeletedWhenClientScopeDeleted() {
        String federatedUserId = createFederatedUser("fed-user-scope-delete");

        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("test-scope-to-delete");
        scopeRep.setProtocol("oid4vc");
        String scopeId;
        try(Response resp = managedRealm.admin().clientScopes().create(scopeRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
            scopeId = ApiUtil.getCreatedId(resp);
        }
        adminEvents.clear();

        runOnServer.run(session -> session.users().addVerifiableCredential(federatedUserId,
                new UserVerifiableCredentialModel("vc_1", scopeId)));

        runOnServer.run(session -> {
            long count = session.users().getVerifiableCredentialsByUser(federatedUserId).count();
            assertEquals(1, count);
        });

        managedRealm.admin().clientScopes().get(scopeId).remove();

        runOnServer.run(session -> {
            long count = session.users().getVerifiableCredentialsByUser(federatedUserId).count();
            assertEquals(0, count);
        });
    }

    @Test
    public void testGetVerifiableCredentialByClientScope() {
        String federatedUserId = createFederatedUser("fed-user-get-verify-fields");
        String scopeId = resolveScopeId(CLIENT_SCOPE_NAME_1);

        runOnServer.run(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel("vc-with-fields", scopeId);
            vcModel.setRevision("rev-123");
            vcModel.setUserAttributes(Map.of(
                    "firstName", List.of("John"),
                    "lastName", List.of("Doe")
            ));

            session.users().addVerifiableCredential(federatedUserId, vcModel);
        });

        runOnServer.run(session -> {
            UserVerifiableCredentialModel retrieved = session.users().getVerifiableCredentialByClientScope(federatedUserId, scopeId);

            assertNotNull(retrieved);
            assertNotNull(retrieved.getId());
            assertEquals(scopeId, retrieved.getClientScopeId());
            assertEquals("rev-123", retrieved.getRevision());
            assertNotNull(retrieved.getCreatedDate());
            assertNotNull(retrieved.getUserAttributes());
            assertEquals(List.of("John"), retrieved.getUserAttributes().get("firstName"));
            assertEquals(List.of("Doe"), retrieved.getUserAttributes().get("lastName"));
        });
    }

    @Test
    public void testGetVerifiableCredentialById() {
        String federatedUserId = createFederatedUser("fed-user-get-verify-fields-by-id");
        String scopeId = resolveScopeId(CLIENT_SCOPE_NAME_1);

        String verCredId = runOnServer.fetchString(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel("vc-with-fields", scopeId);
            vcModel.setRevision("rev-456");
            vcModel.setUserAttributes(Map.of(
                    "firstName", List.of("Max"),
                    "lastName", List.of("Mustermann")
            ));

            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(federatedUserId, vcModel);
            return added.getId();
        });

        runOnServer.run(session -> {
            UserVerifiableCredentialModel retrieved = session.users().getVerifiableCredentialById(verCredId);

            assertNotNull(retrieved);
            assertNotNull(retrieved.getId());
            assertEquals(scopeId, retrieved.getClientScopeId());
            assertEquals("rev-456", retrieved.getRevision());
            assertNotNull(retrieved.getCreatedDate());
            assertNotNull(retrieved.getUserAttributes());
            assertEquals(List.of("Max"), retrieved.getUserAttributes().get("firstName"));
            assertEquals(List.of("Mustermann"), retrieved.getUserAttributes().get("lastName"));
        });
    }

    public static class FederatedVcTestRealmConfig implements RealmConfig {
        public static final String TEST_REALM_NAME = "test";

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .name(TEST_REALM_NAME)
                    .eventsEnabled(true)
                    .eventsListeners("jboss-logging")
                    .verifiableCredentialsEnabled(true)
                    .clientScopes(
                            createCredentialScope(CLIENT_SCOPE_NAME_1),
                            createCredentialScope(CLIENT_SCOPE_NAME_2)
                    );
        }


        private static CredentialScopeRepresentation createCredentialScope(String scopeName) {
            return new CredentialScopeRepresentation(scopeName)
                    .setIncludeInTokenScope(true)
                    .setCredentialConfigurationId(scopeName);
        }

    }

    private String resolveScopeId(String scopeName) {
        return runOnServer.fetchString(session ->
            session.clientScopes()
                    .getClientScopesStream(session.getContext().getRealm())
                    .filter(cs -> scopeName.equals(cs.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Client scope not found: " + scopeName))
                    .getId()
        );
    }

    private String createFederatedUser(String username) {
        return createFederatedUser(username, "John", "Doe", username + "@example.com");
    }

    private String createFederatedUser(String username, String firstName, String lastName, String email) {
        return runOnServer.fetchString(session -> {
            String providerId = "00000000-0000-0000-0000-000000000001";
            // Create federated user ID: f:<providerId>:<externalId>
            String federatedUserId = "f:" + providerId + ":" + username;

            FederatedUser fedUser = new FederatedUser();
            fedUser.setId(federatedUserId);
            fedUser.setRealmId(session.getContext().getRealm().getId());
            fedUser.setStorageProviderId(providerId);
            session.getProvider(JpaConnectionProvider.class).getEntityManager().persist(fedUser);

            UserFederatedStorageProvider federatedStorage = UserStorageUtil.userFederatedStorage(session);
            federatedStorage.setSingleAttribute(session.getContext().getRealm(), federatedUserId, "username", username);
            federatedStorage.setSingleAttribute(session.getContext().getRealm(), federatedUserId, "firstName", firstName);
            federatedStorage.setSingleAttribute(session.getContext().getRealm(), federatedUserId, "lastName", lastName);
            federatedStorage.setSingleAttribute(session.getContext().getRealm(), federatedUserId, "email", email);
            return federatedUserId;
        });
    }
}
