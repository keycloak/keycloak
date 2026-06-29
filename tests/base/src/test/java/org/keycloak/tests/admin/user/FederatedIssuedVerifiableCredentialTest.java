package org.keycloak.tests.admin.user;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.common.util.Time;
import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.UserVerifiableCredentialModel;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
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
public class FederatedIssuedVerifiableCredentialTest extends AbstractUserTest {

    @InjectRealm(config = FederatedIssuedCredentialTestRealmConfig.class)
    protected ManagedRealm testRealm;

    private static final String CLIENT_SCOPE_NAME_1 = "driving-license-cert";
    private static final String CLIENT_SCOPE_NAME_2 = "education-cert";

    @Test
    @DatabaseTest
    public void testAddIssuedCredentialForFederatedUser() {
        String federatedUserId = createFederatedUser("fed-user-issued");
        String clientId = createTestClient("wallet-client");
        String scopeId = resolveScopeId(CLIENT_SCOPE_NAME_1);


        String verifiableCredentialId = runOnServer.fetchString(session -> {
            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(federatedUserId, new UserVerifiableCredentialModel("vc_01", scopeId));
            return added.getId();
        });

        // Add IssuedVerifiableCredential
        runOnServer.run(session -> {
            IssuedVerifiableCredentialModel issuedVc = new IssuedVerifiableCredentialModel(federatedUserId, verifiableCredentialId, clientId);
            issuedVc.setRevision("rev-001");

            IssuedVerifiableCredentialModel added = session.users().addIssuedVerifiableCredential(issuedVc);

            assertNotNull(added);
            assertNotNull(added.getId());
            assertNotNull(added.getIssuedAt());
            assertEquals(federatedUserId, added.getUserId());
        });

        runOnServer.run(session -> {
            List<IssuedVerifiableCredentialModel> issued = session.users()
                    .getIssuedVerifiableCredentialsStreamByUser(federatedUserId)
                    .collect(Collectors.toList());

            assertEquals(1, issued.size());
            assertEquals(verifiableCredentialId, issued.get(0).getVerifiableCredentialId());
        });
    }


    @Test
    @DatabaseTest
    public void testRemoveIssuedCredentialForFederatedUser() {
        String federatedUserId = createFederatedUser("fed-user-issued-remove");
        String clientId = createTestClient("wallet-client");
        String scopeId = resolveScopeId(CLIENT_SCOPE_NAME_1);

        // Add VC and Issued VC, get Issued VC Id
        String issuedVcId = runOnServer.fetchString(session -> {
            UserVerifiableCredentialModel addedVC = session.users().addVerifiableCredential(federatedUserId, new UserVerifiableCredentialModel("vc_01", scopeId));
            IssuedVerifiableCredentialModel issuedVc = new IssuedVerifiableCredentialModel(federatedUserId, addedVC.getId(), clientId);
            issuedVc.setRevision("rev-001");
            IssuedVerifiableCredentialModel added = session.users().addIssuedVerifiableCredential(issuedVc);
            return added.getId();
        });

        // Verify exists
        runOnServer.run(session -> {
            List<IssuedVerifiableCredentialModel> list = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).toList();
            assertEquals(1, list.size());

            boolean removed = session.users().removeIssuedVerifiableCredential(issuedVcId);
            assertTrue(removed);
        });

        // Verify removed
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(0, count);
        });
    }

    @Test
    @DatabaseTest
    public void testIssuedVCDeletedWhenUserVCDeletedForFederatedUser() {
        String federatedUserId = createFederatedUser("fed-user-cascade");
        String clientId = createTestClient("wallet-cascade");
        String scopeId = resolveScopeId(CLIENT_SCOPE_NAME_1);

        // Add UserVC and IssuedVC
        runOnServer.run(session -> {
            UserVerifiableCredentialModel addedVc = session.users().addVerifiableCredential(federatedUserId, new UserVerifiableCredentialModel("vc-001", scopeId));

            IssuedVerifiableCredentialModel issuedVc = new IssuedVerifiableCredentialModel(federatedUserId, addedVc.getId(), clientId);
            issuedVc.setRevision("rev-001");
            session.users().addIssuedVerifiableCredential(issuedVc);
        });

        // Verify IssuedVC exists
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(1, count);
        });

        // Delete UserVerifiableCredential
        runOnServer.run(session -> {
            boolean vCRemoved = session.users().removeVerifiableCredential(federatedUserId, scopeId);
            assertTrue(vCRemoved);
        });

        // Verify IssuedVC is also deleted (cascade)
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(0, count);
        });
    }

    @Test
    @DatabaseTest
    public void testRemoveExpiredIssuedVerifiableCredentials() {
        String federatedUserId = createFederatedUser("fed-user-expiry");
        String clientId = createTestClient("wallet-client");
        String scopeId = resolveScopeId(CLIENT_SCOPE_NAME_1);

        long now = Time.currentTimeMillis();
        long oneHourInMs = 3600000L;

        // Create one verifiable credential and two issued credentials with different expiration times
        runOnServer.run(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel(null, scopeId);
            vcModel.setRevision("rev-001");
            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(federatedUserId, vcModel);

            // First issued VC - expires in 1 hour
            IssuedVerifiableCredentialModel model1 = new IssuedVerifiableCredentialModel(federatedUserId, added.getId(), clientId);
            model1.setRevision("rev-001");
            model1.setIssuedAt(now);
            model1.setExpiresAt(now + oneHourInMs);
            session.users().addIssuedVerifiableCredential(model1);

            // Second issued VC - expires in 2 hours
            IssuedVerifiableCredentialModel model2 = new IssuedVerifiableCredentialModel(federatedUserId, added.getId(), clientId);
            model2.setRevision("rev-002");
            model2.setIssuedAt(now);
            model2.setExpiresAt(now + oneHourInMs * 2);
            session.users().addIssuedVerifiableCredential(model2);
        });

        // Verify both exist
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(2, count);
        });

        // Call cleanup - no expirations yet
        runOnServer.run(session -> session.users().removeExpiredIssuedVerifiableCredentials());
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(2, count);
        });

        // Advance time 1 hour - first credential expires
        timeOffSetWithCaches.set((int) (oneHourInMs / 1000));
        runOnServer.run(session -> session.users().removeExpiredIssuedVerifiableCredentials());
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(1, count);
        });

        // Advance time 2 hours - both credentials expire
        timeOffSetWithCaches.set((int) (2 * oneHourInMs / 1000));
        runOnServer.run(session -> session.users().removeExpiredIssuedVerifiableCredentials());
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(0, count);
        });
    }

    @Test
    @DatabaseTest
    public void testIssuedVCDeletedWhenClientDeletedForFederatedUser() {
        String federatedUserId = createFederatedUser("fed-user-client-delete");
        String clientId = createTestClient("wallet-to-delete");
        String scopeId1 = resolveScopeId(CLIENT_SCOPE_NAME_1);
        String scopeId2 = resolveScopeId(CLIENT_SCOPE_NAME_2);

        // Add credentials
        runOnServer.run(session -> {
            UserVerifiableCredentialModel addedVc1 = session.users().addVerifiableCredential(federatedUserId, new UserVerifiableCredentialModel("vc-001", scopeId1));
            UserVerifiableCredentialModel addedVc2 = session.users().addVerifiableCredential(federatedUserId, new UserVerifiableCredentialModel("vc-002", scopeId2));

            IssuedVerifiableCredentialModel vc1 = new IssuedVerifiableCredentialModel(federatedUserId, addedVc1.getId(), clientId);
            vc1.setRevision("rev-001");
            session.users().addIssuedVerifiableCredential(vc1);

            IssuedVerifiableCredentialModel vc2 = new IssuedVerifiableCredentialModel(federatedUserId, addedVc2.getId(), clientId);
            vc2.setRevision("rev-002");
            session.users().addIssuedVerifiableCredential(vc2);
        });

        // Verify both issued credentials exists
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(2, count);
        });

        // Delete client
        managedRealm.admin().clients().get(clientId).remove();

        // Verify all IssuedVCs from this client are deleted
        runOnServer.run(session -> {
            long count = session.users().getIssuedVerifiableCredentialsStreamByUser(federatedUserId).count();
            assertEquals(0, count);
        });
    }

    private String createTestClient(String clientName) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientName);
        clientRep.setEnabled(true);

        jakarta.ws.rs.core.Response resp = managedRealm.admin().clients().create(clientRep);
        String clientId = ApiUtil.getCreatedId(resp);
        resp.close();
        adminEvents.clear();
        return clientId;
    }

    public static class FederatedIssuedCredentialTestRealmConfig implements RealmConfig {
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

}
