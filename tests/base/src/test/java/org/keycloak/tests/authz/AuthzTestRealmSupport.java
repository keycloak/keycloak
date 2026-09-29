package org.keycloak.tests.authz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Realm import helpers for migrated authz tests without inheriting the legacy Arquillian runner.
 */
public abstract class AuthzTestRealmSupport {

    protected final Logger log = Logger.getLogger(getClass());

    protected Keycloak adminClient;
    protected List<RealmRepresentation> testRealmReps;

    public abstract void addTestRealms(List<RealmRepresentation> testRealms);

    protected boolean isImportAfterEachMethod() {
        return false;
    }

    protected boolean removeVerifyProfileAtImport() {
        return true;
    }

    public Keycloak getAdminClient() {
        return adminClient;
    }

    public RealmsResource realmsResouce() {
        return adminClient.realms();
    }

    public void importTestRealms() {
        addTestRealmsInternal();
        log.info("importing test realms");
        for (RealmRepresentation testRealm : testRealmReps) {
            importRealm(testRealm);
        }
        afterImportTestRealms();
    }

    protected void afterImportTestRealms() {
    }

    private void addTestRealmsInternal() {
        if (testRealmReps == null) {
            testRealmReps = new ArrayList<>();
        }
        if (testRealmReps.isEmpty()) {
            addTestRealms(testRealmReps);
        }
    }

    public void importRealm(RealmRepresentation realm) {
        try {
            adminClient.realms().realm(realm.getRealm()).remove();
        } catch (NotFoundException ignore) {
        }
        adminClient.realms().create(realm);
        configureRealmForTestEvents(realm.getRealm());

        if (removeVerifyProfileAtImport()) {
            try {
                RequiredActionProviderRepresentation vpModel = adminClient.realm(realm.getRealm()).flows()
                        .getRequiredAction(UserModel.RequiredAction.VERIFY_PROFILE.name());
                vpModel.setEnabled(false);
                vpModel.setDefaultAction(false);
                adminClient.realm(realm.getRealm()).flows().updateRequiredAction(
                        UserModel.RequiredAction.VERIFY_PROFILE.name(), vpModel);
            } catch (NotFoundException ignore) {
            }
        }
    }

    private void configureRealmForTestEvents(String realmName) {
        RealmRepresentation realmRepresentation = adminClient.realm(realmName).toRepresentation();
        realmRepresentation.setEventsEnabled(true);
        adminClient.realm(realmName).update(realmRepresentation);
    }

    public void removeRealm(String realmName) {
        log.info("removing realm: " + realmName);
        try {
            adminClient.realms().realm(realmName).remove();
        } catch (NotFoundException ignore) {
        }
    }

    public String createUser(String realm, String username, String password, String... requiredActions) {
        UserRepresentation user = createUserRepresentation(username, password);
        user.setRequiredActions(Arrays.asList(requiredActions));
        return AdminApiUtil.createUserWithAdminClient(adminClient.realm(realm), user);
    }

    public String createUser(String realm, String username, String password, String firstName, String lastName, String email, Consumer<UserRepresentation> customizer) {
        UserRepresentation user = createUserRepresentation(username, email, firstName, lastName, true, password);
        customizer.accept(user);
        return AdminApiUtil.createUserWithAdminClient(adminClient.realm(realm), user);
    }

    public String createUser(String realm, String username, String password, String firstName, String lastName, String email) {
        UserRepresentation user = createUserRepresentation(username, email, firstName, lastName, true, password);
        return AdminApiUtil.createUserWithAdminClient(adminClient.realm(realm), user);
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName, String lastName, boolean enabled) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(enabled);
        return user;
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName, String lastName, boolean enabled, String password) {
        UserRepresentation user = createUserRepresentation(username, email, firstName, lastName, enabled);
        setPasswordFor(user, password);
        return user;
    }

    public static UserRepresentation createUserRepresentation(String username, String password) {
        return createUserRepresentation(username, null, null, null, true, password);
    }

    public static void setPasswordFor(UserRepresentation user, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));
    }

    protected void assertResponseSuccessful(Response response) {
        try {
            assertEquals(Response.Status.Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        } catch (AssertionError ex) {
            throw new AssertionError("unexpected response code " + response.getStatus() + ", body is:\n" + response.readEntity(String.class), ex);
        }
    }
}
