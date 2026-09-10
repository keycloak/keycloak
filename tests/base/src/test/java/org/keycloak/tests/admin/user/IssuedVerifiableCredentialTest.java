package org.keycloak.tests.admin.user;

import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.UserVerifiableCredentialModel;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.jwtTypeNaturalPersonScopeName;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;



@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class IssuedVerifiableCredentialTest extends AbstractUserTest {

    private static final String CREDENTIAL_TYPE_1 = jwtTypeNaturalPersonScopeName;
    private static final String CREDENTIAL_TYPE_2 = "education-cert";
    private static final String CERT_1 = "cert-1";
    private static final String CERT_2 = "cert-2";
    private static final String CERT_3 = "cert-3";

    @InjectRealm(config = IssuedVcTestRealmConfig.class)
    protected ManagedRealm testRealm;

    @InjectTimeOffSet
    protected TimeOffSet timeOffSet;


    @Test
    @DatabaseTest
    public void testGetIssuedCredentials_EmptyList() {
        String userId = createUser();

        UserResource userResource = managedRealm.admin().users().get(userId);
        List<IssuedVerifiableCredentialRepresentation> issuedCreds = userResource.verifiableCredentials().getIssuedCredentials();

        assertNotNull(issuedCreds, "Should return empty list, not null");
        assertThat(issuedCreds, empty());
    }

    @Test
    @DatabaseTest
    public void testGetIssuedCredentials_WithData() {
        String userId = createUser();

        createIssuedVcViaModelLayer(userId, CREDENTIAL_TYPE_1, "wallet-123", "rev-001");
        createIssuedVcViaModelLayer(userId, CREDENTIAL_TYPE_2, "wallet-456", "rev-002");

        // Retrieve via REST API
        UserResource userResource = managedRealm.admin().users().get(userId);
        List<IssuedVerifiableCredentialRepresentation> issuedCreds = userResource.verifiableCredentials().getIssuedCredentials();

        assertThat(issuedCreds, hasSize(2));

        // Verify field population
        IssuedVerifiableCredentialRepresentation firstCred = issuedCreds.get(0);
        assertNotNull(firstCred.getId());
        assertEquals(userId, firstCred.getUserId());
        assertThat(firstCred.getCredentialType(), is(oneOf(CREDENTIAL_TYPE_1, CREDENTIAL_TYPE_2)));
        assertNotNull(firstCred.getClientId());
        assertNotNull(firstCred.getRevision());
        assertNotNull(firstCred.getIssuedAt());
    }

    @Test
    @DatabaseTest
    public void testGetIssuedCredentials_SortedByIssuedAtDesc() {
        String userId = createUser();
        long baseTime = Time.currentTimeMillis();

        createIssuedVcViaModelLayer(userId, CERT_1, "wallet-1", "rev-1", baseTime);
        createIssuedVcViaModelLayer(userId, CERT_2, "wallet-2", "rev-2", baseTime + 1000);
        createIssuedVcViaModelLayer(userId, CERT_3, "wallet-3", "rev-3", baseTime + 2000);

        UserResource userResource = managedRealm.admin().users().get(userId);
        List<IssuedVerifiableCredentialRepresentation> issuedCreds = userResource.verifiableCredentials().getIssuedCredentials();

        assertThat(issuedCreds, hasSize(3));

        assertEquals(CERT_3, issuedCreds.get(0).getCredentialType());
        assertEquals(CERT_2, issuedCreds.get(1).getCredentialType());
        assertEquals(CERT_1, issuedCreds.get(2).getCredentialType());
    }

    @Test
    @DatabaseTest
    public void testGetIssuedCredentials_UserIsolation() {
        String user1Id = createUser("user1", "user1@test.com");
        String user2Id = createUser("user2", "user2@test.com");

        createIssuedVcViaModelLayer(user1Id, CREDENTIAL_TYPE_1, "wallet1", "rev1");
        createIssuedVcViaModelLayer(user2Id, CREDENTIAL_TYPE_2, "wallet2", "rev2");

        // User 1 should only see their VC
        List<IssuedVerifiableCredentialRepresentation> user1Creds =
                managedRealm.admin().users().get(user1Id).verifiableCredentials().getIssuedCredentials();

        assertThat(user1Creds, hasSize(1));
        assertEquals(CREDENTIAL_TYPE_1, user1Creds.get(0).getCredentialType());

        // User 2 should only see their VC
        List<IssuedVerifiableCredentialRepresentation> user2Creds =
                managedRealm.admin().users().get(user2Id).verifiableCredentials().getIssuedCredentials();

        assertThat(user2Creds, hasSize(1));
        assertEquals(CREDENTIAL_TYPE_2, user2Creds.get(0).getCredentialType());
    }

    @Test
    @DatabaseTest
    public void testGetIssuedCredentials_WithExpiresAt() {
        String userId = createUser();
        long issuedAt = Time.currentTimeMillis();
        long expiresAt = issuedAt + 86400000L; // 24 hours later

        // Resolve scope ID
        String clientScopeId = managedRealm.admin().clientScopes().findAll().stream()
                .filter(s -> CREDENTIAL_TYPE_1.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client scope not found: " + CREDENTIAL_TYPE_1))
                .getId();

        runOnServer.run(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel(null, clientScopeId);
            vcModel.setRevision("rev-001");
            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(userId, vcModel);

            IssuedVerifiableCredentialModel model = new IssuedVerifiableCredentialModel(userId, added.getId(), "wallet-123");
            model.setRevision("rev-001");
            model.setIssuedAt(issuedAt);
            model.setExpiresAt(expiresAt);
            session.users().addIssuedVerifiableCredential(model);
        });

        UserResource userResource = managedRealm.admin().users().get(userId);
        List<IssuedVerifiableCredentialRepresentation> issuedCreds = userResource.verifiableCredentials().getIssuedCredentials();

        assertThat(issuedCreds, hasSize(1));
        IssuedVerifiableCredentialRepresentation cred = issuedCreds.get(0);
        assertEquals(expiresAt, cred.getExpiresAt());
    }

    @Test
    public void testGetIssuedCredentials_FeatureDisabled() {
        managedRealm.updateWithCleanup((realm) -> realm.verifiableCredentialsEnabled(false));
        adminEvents.clear();

        String userId = createUser();
        UserResource userResource = managedRealm.admin().users().get(userId);

        try {
            userResource.verifiableCredentials().getIssuedCredentials();
            Assertions.fail("Expected BadRequestException when feature is disabled");
        } catch (BadRequestException e) {
            assertThat(e.getResponse().getStatus(), is(400));
        }
    }

    @Test
    @DatabaseTest
    public void testGetIssuedCredentials_NonExistentUser() {
        try {
            managedRealm.admin().users().get("non-existent-user-id").verifiableCredentials().getIssuedCredentials();
            Assertions.fail("Expected NotFoundException");
        } catch (NotFoundException e) {
            assertThat(e.getResponse().getStatus(), is(404));
        }
    }

    @Test
    @DatabaseTest
    public void testRevokeIssuedCredential() {
        String userId = createUser();
        createIssuedVcViaModelLayer(userId, CREDENTIAL_TYPE_1, "wallet-123", "rev-001");

        UserResource userResource = managedRealm.admin().users().get(userId);
        List<IssuedVerifiableCredentialRepresentation> issuedCreds = userResource.verifiableCredentials().getIssuedCredentials();
        assertThat(issuedCreds, hasSize(1));

        String credentialId = issuedCreds.get(0).getId();

        // Revoke the credential
        userResource.verifiableCredentials().revokeIssuedCredential(credentialId);

        // Verify
        List<IssuedVerifiableCredentialRepresentation> afterRevoke = userResource.verifiableCredentials().getIssuedCredentials();
        assertThat(afterRevoke, empty());
    }

    @Test
    @DatabaseTest
    public void testIssuedVCDeletedWhenUserVCDeleted() {
        String userId = createUser();
        String clientId = createTestClient("wallet-client");

        // Create UserVC and IssuedVC
        createIssuedVcViaModelLayer(userId, CREDENTIAL_TYPE_1, clientId, "rev-001");

        // Verify IssuedVC exists
        UserResource userResource = managedRealm.admin().users().get(userId);
        List<IssuedVerifiableCredentialRepresentation> issuedCreds = userResource.verifiableCredentials().getIssuedCredentials();
        assertThat(issuedCreds, hasSize(1));
        assertEquals(CREDENTIAL_TYPE_1, issuedCreds.get(0).getCredentialType());

        // Delete UserVerifiableCredential
        userResource.verifiableCredentials().revokeCredential(CREDENTIAL_TYPE_1);

        // Verify IssuedVC is also deleted
        List<IssuedVerifiableCredentialRepresentation> afterDelete = userResource.verifiableCredentials().getIssuedCredentials();
        assertThat(afterDelete, empty());
    }

    @Test
    @DatabaseTest
    public void testIssuedVCDeletedWhenClientDeleted() {
        String userId = createUser();
        String walletClientId = createTestClient("test-wallet");

        // Issue 2 credentials from this wallet
        createIssuedVcViaModelLayer(userId, CREDENTIAL_TYPE_1, walletClientId, "rev-001");
        createIssuedVcViaModelLayer(userId, CREDENTIAL_TYPE_2, walletClientId, "rev-002");

        // Verify issuedVCs exist
        UserResource userResource = managedRealm.admin().users().get(userId);
        assertThat(userResource.verifiableCredentials().getIssuedCredentials(), hasSize(2));

        // Delete the wallet client via REST API
        managedRealm.admin().clients().get(walletClientId).remove();

        // Verify all IssuedVCs from this wallet are deleted
        List<IssuedVerifiableCredentialRepresentation> afterDelete = userResource.verifiableCredentials().getIssuedCredentials();
        assertThat(afterDelete, empty());
    }

    @Test
    @DatabaseTest
    public void testIssuedVCDeletedWhenUserDeleted() {
        String userId = createUser();
        String clientId = createTestClient("wallet-client");

        // Create 2 VCs
        createIssuedVcViaModelLayer(userId, CREDENTIAL_TYPE_1, clientId, "rev-001");
        createIssuedVcViaModelLayer(userId, CREDENTIAL_TYPE_2, clientId, "rev-002");

        // Verify both exist
        UserResource userResource = managedRealm.admin().users().get(userId);
        assertThat(userResource.verifiableCredentials().getIssuedCredentials(), hasSize(2));
        assertThat(userResource.verifiableCredentials().getCredentials(), hasSize(2));

        // Delete user
        managedRealm.runCleanup();

        // Verify user and all VCs are deleted
        runOnServer.run(session -> {
            assertNull(session.users().getUserById(session.getContext().getRealm(), userId));
            assertEquals(0,  session.users().getIssuedVerifiableCredentialsStreamByUser(userId).count());
        });
    }

    @Test
    @DatabaseTest
    public void testIssuedVCDeletedWhenClientScopeDeleted() {
        String userId = createUser();
        String clientId = createTestClient("wallet-client");

        // Create client scope
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("test-scope");
        scopeRep.setProtocol("oid4vc");
        Response resp = managedRealm.admin().clientScopes().create(scopeRep);
        String scopeId = ApiUtil.getCreatedId(resp);
        resp.close();
        adminEvents.clear();

        // Create IssuedVC
        createIssuedVcViaModelLayer(userId, "test-scope", clientId, "rev-001");

        UserResource userResource = managedRealm.admin().users().get(userId);
        assertThat(userResource.verifiableCredentials().getIssuedCredentials(), hasSize(1));

        // Delete client scope
        managedRealm.admin().clientScopes().get(scopeId).remove();

        // Verify IssuedVC deleted
        assertThat(userResource.verifiableCredentials().getIssuedCredentials(), empty());
    }

    @Test
    @DatabaseTest
    public void testRemoveExpiredIssuedVerifiableCredentials() {
        String userId = createUser();
        String clientId = createTestClient("wallet-client");

        long now = Time.currentTimeMillis();
        long oneHourInMs = 3600000L;

        // Resolve scope ID
        String clientScopeId = managedRealm.admin().clientScopes().findAll().stream()
                .filter(s -> CREDENTIAL_TYPE_1.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client scope not found: " + CREDENTIAL_TYPE_1))
                .getId();

        // Create one verifiable credential and two issued credentials with different expiration times
        runOnServer.run(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel(null, clientScopeId);
            vcModel.setRevision("rev-001");
            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(userId, vcModel);

            IssuedVerifiableCredentialModel model1 = new IssuedVerifiableCredentialModel(userId, added.getId(), clientId);
            model1.setRevision("rev-001");
            model1.setIssuedAt(now);
            model1.setExpiresAt(now + oneHourInMs); // Expires in 1 hour
            session.users().addIssuedVerifiableCredential(model1);

            IssuedVerifiableCredentialModel model2 = new IssuedVerifiableCredentialModel(userId, added.getId(), clientId);
            model2.setRevision("rev-002");
            model2.setIssuedAt(now);
            model2.setExpiresAt(now + oneHourInMs * 2); // Expires in 2 hour
            session.users().addIssuedVerifiableCredential(model2);
        });

        UserResource userResource = managedRealm.admin().users().get(userId);
        assertThat(userResource.verifiableCredentials().getIssuedCredentials(), hasSize(2));

        // no expirations
        runOnServer.run(session -> session.users().removeExpiredIssuedVerifiableCredentials());
        List<IssuedVerifiableCredentialRepresentation> issuedCredentials = userResource.verifiableCredentials().getIssuedCredentials();
        assertThat(issuedCredentials, hasSize(2));

        //first expires
        timeOffSet.set((int) (oneHourInMs / 1000));
        runOnServer.run(session -> session.users().removeExpiredIssuedVerifiableCredentials());
        issuedCredentials = userResource.verifiableCredentials().getIssuedCredentials();
        assertThat(issuedCredentials, hasSize(1));

        //all expired
        timeOffSet.set((int) (2 * oneHourInMs / 1000));
        runOnServer.run(session -> session.users().removeExpiredIssuedVerifiableCredentials());
        issuedCredentials = userResource.verifiableCredentials().getIssuedCredentials();
        assertThat(issuedCredentials, hasSize(0));
    }

    // Helper methods

    protected void createIssuedVcViaModelLayer(String userId, String credentialType,
                                               String clientId, String revision) {
        createIssuedVcViaModelLayer(userId, credentialType, clientId, revision, null);
    }

    protected void createIssuedVcViaModelLayer(String userId, String credentialType,
                                               String clientId, String revision, Long issuedAt) {
        // Resolve scope ID
        String clientScopeId = testRealm.admin().clientScopes().findAll().stream()
                .filter(s -> credentialType.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client scope not found: " + credentialType))
                .getId();

        runOnServer.run(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel(null, clientScopeId);
            vcModel.setRevision(revision);
            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(userId, vcModel);

            IssuedVerifiableCredentialModel model = new IssuedVerifiableCredentialModel(userId, added.getId(), clientId);
            model.setRevision(revision);
            if (issuedAt != null) {
                model.setIssuedAt(issuedAt);
            }
            session.users().addIssuedVerifiableCredential(model);
        });
    }

    private String createTestClient(String clientName) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientName);
        clientRep.setEnabled(true);

        Response resp = managedRealm.admin().clients().create(clientRep);
        String clientId = ApiUtil.getCreatedId(resp);
        resp.close();
        adminEvents.clear();
        return clientId;
    }

    public static class IssuedVcTestRealmConfig implements RealmConfig {
        public static final String TEST_REALM_NAME = "test";

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .name(TEST_REALM_NAME)
                    .eventsEnabled(true)
                    .eventsListeners("jboss-logging")
                    .verifiableCredentialsEnabled(true)
                    .clientScopes(
                            createCredentialScope(CREDENTIAL_TYPE_1),
                            createCredentialScope(CREDENTIAL_TYPE_2),
                            createCredentialScope(CERT_1),
                            createCredentialScope(CERT_2),
                            createCredentialScope(CERT_3)
                    );
        }
    }

    private static CredentialScopeRepresentation createCredentialScope(String scopeName) {
        return new CredentialScopeRepresentation(scopeName)
                .setIncludeInTokenScope(true)
                .setCredentialConfigurationId(scopeName);
    }
}
