package org.keycloak.tests.admin.user;

import java.util.List;
import java.util.stream.Collectors;

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

import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.jwtTypeNaturalPersonScopeName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
@DatabaseTest
public class FederatedUserVerifiableCredentialTest extends AbstractUserTest {

    private static final String CREDENTIAL_TYPE_1 = jwtTypeNaturalPersonScopeName;
    private static final String CREDENTIAL_TYPE_2 = "education-cert";

    @InjectRealm(config = FederatedVcTestRealmConfig.class)
    protected ManagedRealm testRealm;

    @Test
    public void testAddVerifiableCredentialForFederatedUser() {
        String federatedUserId = createFederatedUser("fed-user-1");

        runOnServer.run(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel(CREDENTIAL_TYPE_1);
            vcModel.setRevision("rev-001");

            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(federatedUserId, vcModel);
            assertNotNull(added);
            assertNotNull(added.getRevision());
            assertNotNull(added.getCreatedDate());
            assertNotNull(added.getCredentialScopeName());
        });

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> vcs = session.users()
                    .getVerifiableCredentialsByUser(federatedUserId)
                    .collect(Collectors.toList());

            assertEquals(1, vcs.size());
            assertNotNull(vcs.get(0).getCredentialScopeName());
        });
    }

    @Test
    public void testGetVerifiableCredentialsForFederatedUser_EmptyList() {
        String federatedUserId = createFederatedUser("fed-user-empty");

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> vcs = session.users()
                    .getVerifiableCredentialsByUser(federatedUserId)
                    .collect(Collectors.toList());

            assertNotNull(vcs);
            assertTrue(vcs.isEmpty());
        });
    }

    @Test
    public void testRemoveVerifiableCredentialForFederatedUser() {
        String federatedUserId = createFederatedUser("fed-user-remove");

        runOnServer.run(session -> {
            session.users().addVerifiableCredential(federatedUserId,
                    new UserVerifiableCredentialModel(CREDENTIAL_TYPE_1));
            session.users().addVerifiableCredential(federatedUserId,
                    new UserVerifiableCredentialModel(CREDENTIAL_TYPE_2));
        });

        runOnServer.run(session -> {
            long count = session.users().getVerifiableCredentialsByUser(federatedUserId).count();
            assertEquals(2, count);
        });

        runOnServer.run(session ->  {
                boolean removed = session.users().removeVerifiableCredential(federatedUserId, CREDENTIAL_TYPE_1);
            assertTrue(removed);
        });

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> remaining = session.users()
                    .getVerifiableCredentialsByUser(federatedUserId)
                    .toList();

            assertEquals(1, remaining.size());
            assertEquals(CREDENTIAL_TYPE_2, remaining.get(0).getCredentialScopeName());
        });
    }

    @Test
    public void testFederatedAndLocalUsersAreIsolated() {
        String localUserId = createUser("local-user", "local@test.com");
        String federatedUserId = createFederatedUser("fed-user-isolated");


        runOnServer.run(session -> {
            session.users().addVerifiableCredential(localUserId, new UserVerifiableCredentialModel("local-cert"));
            session.users().addVerifiableCredential(federatedUserId, new UserVerifiableCredentialModel("federated-cert"));
        });

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> localVcs = session.users()
                    .getVerifiableCredentialsByUser(localUserId)
                    .collect(Collectors.toList());

            assertEquals(1, localVcs.size());
            assertEquals("local-cert", localVcs.get(0).getCredentialScopeName());
        });

        runOnServer.run(session -> {
            List<UserVerifiableCredentialModel> fedVcs = session.users()
                    .getVerifiableCredentialsByUser(federatedUserId)
                    .collect(Collectors.toList());

            assertEquals(1, fedVcs.size());
            assertEquals("federated-cert", fedVcs.get(0).getCredentialScopeName());
        });
    }

    @Test
    public void testCredentialsDeletedWhenClientScopeDeleted() {
        String federatedUserId = createFederatedUser("fed-user-scope-delete");

        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("test-scope-to-delete");
        scopeRep.setProtocol("oid4vc");
        Response resp = managedRealm.admin().clientScopes().create(scopeRep);
        assertNotNull(resp);
        String scopeId = ApiUtil.getCreatedId(resp);
        resp.close();
        adminEvents.clear();

        runOnServer.run(session -> session.users().addVerifiableCredential(federatedUserId,
                new UserVerifiableCredentialModel("test-scope-to-delete")));

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
                            createCredentialScope(CREDENTIAL_TYPE_1),
                            createCredentialScope(CREDENTIAL_TYPE_2)
                    );
        }


        private static CredentialScopeRepresentation createCredentialScope(String scopeName) {
            return new CredentialScopeRepresentation(scopeName)
                    .setIncludeInTokenScope(true)
                    .setCredentialConfigurationId(scopeName);
        }

    }

    private String createFederatedUser(String username) {
        return createFederatedUser(username, "John", "Doe", username + "@example.com");
    }

    private String createFederatedUser(String username, String firstName, String lastName, String email) {
        return runOnServer.fetchString(session -> {
            String providerId = "test-provider";
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
