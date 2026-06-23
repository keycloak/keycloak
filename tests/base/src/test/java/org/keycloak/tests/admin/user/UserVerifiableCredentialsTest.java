package org.keycloak.tests.admin.user;

import java.util.List;
import java.util.Set;

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
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.jwtTypeNaturalPersonScopeName;
import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.sdJwtTypeNaturalPersonScopeName;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class UserVerifiableCredentialsTest extends AbstractUserTest {

    private static final String SCOPE_1_NAME = jwtTypeNaturalPersonScopeName;
    private static final String SCOPE_1_CONFIG_ID = jwtTypeNaturalPersonScopeName;
    private static final String SCOPE_2_NAME = sdJwtTypeNaturalPersonScopeName;
    private static final String SCOPE_2_CONFIG_ID = sdJwtTypeNaturalPersonScopeName;

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
        createVerifiableCredential(user, userId ,SCOPE_1_NAME, SCOPE_1_CONFIG_ID);
        assertVerifiableCredentials(user.getCredentials(), SCOPE_1_NAME);

        // Create second credential and assert both are present
        createVerifiableCredential(user, userId, SCOPE_2_NAME, SCOPE_2_CONFIG_ID);
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

        createVerifiableCredential(user, userId, SCOPE_1_NAME, SCOPE_1_CONFIG_ID);
        try {
            createVerifiableCredential(user, userId, SCOPE_1_NAME, SCOPE_1_CONFIG_ID);
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

        createVerifiableCredential(user, userId ,"new-scope", "new-scope");
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
            createVerifiableCredential(user, userId, SCOPE_1_NAME, SCOPE_1_CONFIG_ID);
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
            createVerifiableCredential(user, userId, "non-existent", null);
            Assertions.fail("Not expected to successfully create verifiable credential referencing unknown client scope");
        } catch (BadRequestException cee) {
            ErrorRepresentation error = cee.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Client scope does not exists", error.getErrorMessage());
        }

        try {
            createVerifiableCredential(user, userId, OAuth2Constants.SCOPE_ADDRESS, null);
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
        long createdDate = Time.currentTimeMillis();
        try {
            UserVerifiableCredentialRepresentation verifCred = new UserVerifiableCredentialRepresentation();
            verifCred.setCredentialScopeName(SCOPE_1_NAME);
            verifCred.setCreatedDate(createdDate);
            verifCred.setUpdatedDate(createdDate);
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

    @Test
    @DatabaseTest
    public void verifyUpdateCredentialRefreshesAttributes() {
        String userId = createUser();
        UserResource userResource = managedRealm.admin().users().get(userId);
        UserVerifiableCredentialResource credResource = userResource.verifiableCredentials();

        UserRepresentation user = userResource.toRepresentation();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        userResource.update(user);
        adminEvents.clear();

        UserVerifiableCredentialRepresentation created = createVerifiableCredential(credResource, userId, SCOPE_1_NAME, SCOPE_1_CONFIG_ID);

        String originalRevision = created.getRevision();
        assertNotNull(created.getUserAttributes(), "Initial snapshot should have attributes");
        assertEquals("John", created.getUserAttributes().get("firstName").get(0), "Initial firstName");
        assertEquals("Doe", created.getUserAttributes().get("lastName").get(0), "Initial lastName");
        assertEquals("john.doe@example.com", created.getUserAttributes().get("email").get(0), "Initial email");


        user = userResource.toRepresentation();
        user.setFirstName("Jane");
        user.setEmail("jane.doe@example.com");
        userResource.update(user);

        UserVerifiableCredentialRepresentation updated = credResource.updateCredential(SCOPE_1_NAME);

        assertNotNull(updated.getUserAttributes(), "Credential should have user attributes snapshot");

        assertAll("Credential snapshot should reflect current user attributes",
                () -> assertEquals("Jane", updated.getUserAttributes().get("firstName").get(0),
                                  "firstName should be updated to Jane in snapshot"),
                () -> assertEquals("Doe", updated.getUserAttributes().get("lastName").get(0),
                                  "lastName should remain Doe in snapshot"),
                () -> assertEquals("jane.doe@example.com", updated.getUserAttributes().get("email").get(0),
                                  "email should be updated in snapshot"),
                () -> assertNotEquals(originalRevision, updated.getRevision(), "Revision should be updated"),
                () -> assertNotEquals(updated.getCreatedDate(), updated.getUpdatedDate(), "Update Date should be different that of created date"),
                () -> assertTrue(updated.getUpdatedDate() > updated.getCreatedDate(), "Update Date should be greater that of created date"),
                () -> assertEquals(SCOPE_1_NAME, updated.getCredentialConfigurationId(),
                        "Credential configuration ID should be set and equal to " + SCOPE_1_NAME)
        );

        List<UserVerifiableCredentialRepresentation> all = credResource.getCredentials();
        UserVerifiableCredentialRepresentation retrieved = all.stream()
                .filter(c -> SCOPE_1_NAME.equals(c.getCredentialScopeName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Credential not found"));

        assertEquals(updated.getRevision(), retrieved.getRevision(), "Retrieved revision should match");
        assertEquals("Jane", retrieved.getUserAttributes().get("firstName").get(0), "Retrieved snapshot should have updated firstName");
        assertEquals("jane.doe@example.com", retrieved.getUserAttributes().get("email").get(0), "Retrieved snapshot should have updated email");
        assertEquals(updated.getCredentialConfigurationId(), retrieved.getCredentialConfigurationId(), "Retrieved credential configuration id should match");
    }

    @Test
    @DatabaseTest
    public void verifySnapshotContainsOnlyAllowedAttributes() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        managedRealm.cleanup().add(r -> r.users().userProfile().update(upConfig));

        upConfig.addOrReplaceAttribute(new UPAttribute("adminOnlyAttr", new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_ADMIN))));
        managedRealm.admin().users().userProfile().update(upConfig);
        adminEvents.clear();

        String userId = createUser();
        UserResource userResource = managedRealm.admin().users().get(userId);
        UserVerifiableCredentialResource credResource = userResource.verifiableCredentials();

        // Set attributes with different visibility
        UserRepresentation user = userResource.toRepresentation();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.singleAttribute("adminOnlyAttr", "adminValue");
        userResource.update(user);
        adminEvents.clear();

        // Create verifiable credential
        UserVerifiableCredentialRepresentation credential = createVerifiableCredential(credResource, userId, SCOPE_1_NAME, SCOPE_1_CONFIG_ID);

        // Verify snapshot contains admin-visible attributes
        assertNotNull(credential.getUserAttributes(), "Snapshot should have attributes");
        assertEquals("adminValue", credential.getUserAttributes().get("adminOnlyAttr").get(0), "adminOnlyAttr should be in snapshot");
        
        upConfig.addOrReplaceAttribute(new UPAttribute("adminOnlyAttr", new UPAttributePermissions(Set.of(ROLE_USER), Set.of(ROLE_USER))));
        managedRealm.admin().users().userProfile().update(upConfig);
        adminEvents.clear();

        //attribute still in the snapshot
        credential = credResource.getCredentials().stream().filter(cred-> cred.getCredentialScopeName().equals(SCOPE_1_NAME)).findFirst().get();
        assertEquals("adminValue", credential.getUserAttributes().get("adminOnlyAttr").get(0), "adminOnlyAttr should be in snapshot");

        credResource.updateCredential(SCOPE_1_NAME);

        credential = credResource.getCredentials().stream().filter(cred-> cred.getCredentialScopeName().equals(SCOPE_1_NAME)).findFirst().get();
        assertNull(credential.getUserAttributes().get("adminOnlyAttr"), "adminOnlyAttr should NOT be in snapshot");
    }

    private UserVerifiableCredentialRepresentation createVerifiableCredential(UserVerifiableCredentialResource user, String userId, String clientScopeName, String credentialConfigId) {
        UserVerifiableCredentialRepresentation verifCred = new UserVerifiableCredentialRepresentation();
        verifCred.setCredentialScopeName(clientScopeName);
        UserVerifiableCredentialRepresentation createdRep = user.createCredential(verifCred);

        assertEquals(clientScopeName, createdRep.getCredentialScopeName());
        assertEquals(credentialConfigId, createdRep.getCredentialConfigurationId());
        assertNotNull(createdRep.getCreatedDate());
        assertNotNull(createdRep.getUpdatedDate());
        assertNotNull(createdRep.getRevision());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userVerifiableCredentialsPath(userId), createdRep, ResourceType.USER);
        return createdRep;
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
