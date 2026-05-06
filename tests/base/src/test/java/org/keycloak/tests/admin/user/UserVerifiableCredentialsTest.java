package org.keycloak.tests.admin.user;

import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UserVerifiableCredentialResource;
import org.keycloak.common.util.Time;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.jwtTypeNaturalPersonScopeName;
import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.sdJwtTypeNaturalPersonScopeName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class UserVerifiableCredentialsTest extends AbstractUserTest {

    private static final String SCOPE_1_NAME = jwtTypeNaturalPersonScopeName;
    private static final String SCOPE_2_NAME = sdJwtTypeNaturalPersonScopeName;

    @InjectRealm(config = VCTestRealmConfig.class)
    protected ManagedRealm testRealm;

    @Test
    @DatabaseTest
    public void verifiableCredentialsCrud() {
        String userId = createUser();
        UserVerifiableCredentialResource user = managedRealm.admin().users().get(userId).verifiableCredentials();

        // Empty list initially
        assertTrue(user.getCredentials().isEmpty());

        // Create first credential and assert it is present
        createVerifiableCedential(user, userId ,SCOPE_1_NAME);
        assertVerifiableCredentials(user.getCredentials(), SCOPE_1_NAME);

        // Create second credential and assert both are present
        createVerifiableCedential(user, userId, SCOPE_2_NAME);
        assertVerifiableCredentials(user.getCredentials(), SCOPE_1_NAME, SCOPE_2_NAME);

        // Remove one of credentials
        user.revokeCredential(SCOPE_1_NAME);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.userVerifiableCredentialPath(userId, SCOPE_1_NAME), null, ResourceType.USER);
        assertVerifiableCredentials(user.getCredentials(), SCOPE_2_NAME);

        // Remove second one
        user.revokeCredential(SCOPE_2_NAME);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.userVerifiableCredentialPath(userId, SCOPE_2_NAME), null, ResourceType.USER);
        assertTrue(user.getCredentials().isEmpty());
    }

    @Test
    @DatabaseTest
    public void verifiableCredentialsConflict() {
        String userId = createUser();
        UserVerifiableCredentialResource user = managedRealm.admin().users().get(userId).verifiableCredentials();

        createVerifiableCedential(user, userId, SCOPE_1_NAME);
        try {
            createVerifiableCedential(user, userId, SCOPE_1_NAME);
            Assertions.fail("Not expected to successfully create verifiable credential of same name");
        } catch (ClientErrorException cee) {
            ErrorRepresentation error = cee.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Verifiable credential already exists", error.getErrorMessage());
            assertEquals(409, cee.getResponse().getStatus());
        }
    }

    @Test
    @DatabaseTest
    public void verifiableCredentialsClientScopeRemoved() {
        String userId = createUser();
        UserVerifiableCredentialResource user = managedRealm.admin().users().get(userId).verifiableCredentials();

        ClientScopeRepresentation clientScopeRep = ClientScopeBuilder.create().name("new-scope").protocol(OID4VCIConstants.OID4VC_PROTOCOL).build();
        Response resp = managedRealm.admin().clientScopes().create(clientScopeRep);
        resp.close();
        String clientScopeId = ApiUtil.getCreatedId(resp);
        adminEvents.clear();

        createVerifiableCedential(user, userId ,"new-scope");
        assertVerifiableCredentials(user.getCredentials(), "new-scope");

        // Remove client scope. Assert automatically removed from the user as well
        managedRealm.admin().clientScopes().get(clientScopeId).remove();
        assertVerifiableCredentials(user.getCredentials());
    }

    @Test
    @DatabaseTest
    public void verifiableCredentialsRealmRemoved() {
        // Create new realm
        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm("new");
        realmRep.setEnabled(true);
        realmRep.setVerifiableCredentialsEnabled(true);
        adminClient.realms().create(realmRep);

        // Create user
        RealmResource realm = adminClient.realm("new");
        UserRepresentation user = new UserRepresentation();
        user.setUsername("john");
        user.setEmail("john@email.cz");
        user.setEnabled(true);
        Response response = realm.users().create(user);
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        UserResource userRes = realm.users().get(userId);

        // Create verifiable credential
        UserVerifiableCredentialRepresentation verifCred = new UserVerifiableCredentialRepresentation();
        verifCred.setCredentialScopeName(SCOPE_1_NAME);
        userRes.verifiableCredentials().createCredential(verifCred);

        // Remove realm
        realm.remove();
    }

    @Test
    public void verifiableCredentialsDisabled() {
        managedRealm.updateWithCleanup((realm) -> realm.verifiableCredentialsEnabled(false));
        adminEvents.clear();

        String userId = createUser();
        UserVerifiableCredentialResource user = managedRealm.admin().users().get(userId).verifiableCredentials();

        try {
            createVerifiableCedential(user, userId, SCOPE_1_NAME);
            Assertions.fail("Not expected to successfully create verifiable credential when disabled for the realm");
        } catch (BadRequestException cee) {
            ErrorRepresentation error = cee.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Verifiable credentials not enabled for the realm", error.getErrorMessage());
        }
    }

    @Test
    public void verifiableCredentialsClientScopeErrors() {
        String userId = createUser();
        UserVerifiableCredentialResource user = managedRealm.admin().users().get(userId).verifiableCredentials();

        try {
            createVerifiableCedential(user, userId, "non-existent");
            Assertions.fail("Not expected to successfully create verifiable credential referencing unknown client scope");
        } catch (BadRequestException cee) {
            ErrorRepresentation error = cee.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Client scope does not exists", error.getErrorMessage());
        }

        try {
            createVerifiableCedential(user, userId, OAuth2Constants.SCOPE_ADDRESS);
            Assertions.fail("Not expected to successfully create verifiable credential of OIDC protocol");
        } catch (BadRequestException cee) {
            ErrorRepresentation error = cee.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Client scope has incorrect protocol", error.getErrorMessage());
        }
    }

    @Test
    public void verifiableCredentialsCreateErrors() {
        String userId = createUser();
        UserVerifiableCredentialResource user = managedRealm.admin().users().get(userId).verifiableCredentials();

        try {
            UserVerifiableCredentialRepresentation verifCred = new UserVerifiableCredentialRepresentation();
            verifCred.setCredentialScopeName(SCOPE_1_NAME);
            verifCred.setCreatedDate(Time.currentTimeMillis());
            user.createCredential(verifCred);
            Assertions.fail("Not expected to successfully create verifiable credential with filled createdDate");
        } catch (BadRequestException cee) {
            ErrorRepresentation error = cee.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Created date not expected to be specified", error.getErrorMessage());
        }

        try {
            UserVerifiableCredentialRepresentation verifCred = new UserVerifiableCredentialRepresentation();
            verifCred.setCredentialScopeName(SCOPE_1_NAME);
            verifCred.setRevision("some-revision");
            user.createCredential(verifCred);
            Assertions.fail("Not expected to successfully create verifiable credential with filled revision");
        } catch (BadRequestException cee) {
            ErrorRepresentation error = cee.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Revision not expected to be specified", error.getErrorMessage());
        }
    }

    private void createVerifiableCedential(UserVerifiableCredentialResource user, String userId, String clientScopeName) {
        UserVerifiableCredentialRepresentation verifCred = new UserVerifiableCredentialRepresentation();
        verifCred.setCredentialScopeName(clientScopeName);
        UserVerifiableCredentialRepresentation createdRep = user.createCredential(verifCred);

        Assert.assertEquals(clientScopeName, createdRep.getCredentialScopeName());
        Assert.assertNotNull(createdRep.getCreatedDate());
        Assert.assertNotNull(createdRep.getRevision());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userVerifiableCredentialsPath(userId), createdRep, ResourceType.USER);
    }

    private void assertVerifiableCredentials(List<UserVerifiableCredentialRepresentation> creds, String... expectedCredentialNames) {
        List<String> verifCredNames = creds.stream()
                .map(UserVerifiableCredentialRepresentation::getCredentialScopeName)
                .sorted()
                .toList();

        if (expectedCredentialNames == null || expectedCredentialNames.length == 0) {
            assertTrue(verifCredNames.isEmpty(), "Expected empty list of verifiable credentials, but was " + verifCredNames);
        } else {
            assertEquals(expectedCredentialNames.length, verifCredNames.size());
            assertTrue(verifCredNames.containsAll(List.of(expectedCredentialNames)), "Expected verifiable credentials " + List.of(expectedCredentialNames) + ", but was " + verifCredNames);
        }
    }


    private static class VCTestRealmConfig implements RealmConfig {

        public static final String TEST_REALM_NAME = "test";

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(TEST_REALM_NAME)
                    .eventsEnabled(true);

            realm.eventsListeners("jboss-logging");
            realm.verifiableCredentialsEnabled(true);
            return realm;
        }

    }


}
