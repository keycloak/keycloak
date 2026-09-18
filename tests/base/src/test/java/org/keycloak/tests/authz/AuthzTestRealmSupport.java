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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.events.TestEventsListenerProviderFactory;

import org.jboss.logging.Logger;

import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_HOST;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Realm import helpers for migrated authz tests without inheriting the legacy Arquillian runner.
 */
public abstract class AuthzTestRealmSupport {

    protected final Logger log = Logger.getLogger(getClass());

    protected Keycloak adminClient;
    protected KeycloakTestingClient testingClient;
    protected KeycloakTestingClient.Server runOnServerMaster;
    protected KeycloakTestingClient.Server runOnServer;
    protected List<RealmRepresentation> testRealmReps;

    public abstract void addTestRealms(List<RealmRepresentation> testRealms);

    protected boolean isImportAfterEachMethod() {
        return false;
    }

    protected boolean modifyRealmForSSL() {
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
        if (modifyRealmForSSL()) {
            if (AUTH_SERVER_SSL_REQUIRED) {
                for (ClientRepresentation cr : realm.getClients()) {
                    modifyMainUrls(cr);
                    modifyRedirectUrls(cr);
                }
            }
        }

        if (!AUTH_SERVER_HOST.equals("localhost")) {
            if (!AUTH_SERVER_SSL_REQUIRED) {
                realm.setSslRequired("none");
            }
            if (realm.getClients() != null) {
                for (ClientRepresentation cr : realm.getClients()) {
                    fixAuthServerHostAndPortForClientRepresentation(cr);
                }
            }

            if (realm.getApplications() != null) {
                for (ClientRepresentation cr : realm.getApplications()) {
                    fixAuthServerHostAndPortForClientRepresentation(cr);
                }
            }
        }

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
                testingClient.testing().pollAdminEvent();
            } catch (NotFoundException ignore) {
            }
        }
    }

    private void configureRealmForTestEvents(String realmName) {
        RealmRepresentation realmRepresentation = adminClient.realm(realmName).toRepresentation();
        realmRepresentation.setEventsEnabled(true);
        List<String> listeners = realmRepresentation.getEventsListeners();
        if (listeners == null) {
            listeners = new ArrayList<>();
            realmRepresentation.setEventsListeners(listeners);
        }
        if (!listeners.contains(TestEventsListenerProviderFactory.PROVIDER_ID)) {
            listeners.add(TestEventsListenerProviderFactory.PROVIDER_ID);
        }
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

    protected void assertResponseSuccessful(Response response) {
        try {
            assertEquals(Response.Status.Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        } catch (AssertionError ex) {
            throw new AssertionError("unexpected response code " + response.getStatus() + ", body is:\n" + response.readEntity(String.class), ex);
        }
    }

    public void fixAuthServerHostAndPortForClientRepresentation(ClientRepresentation cr) {
        cr.setBaseUrl(removeDefaultPorts(replaceAuthHostWithRealHost(cr.getBaseUrl())));
        cr.setAdminUrl(removeDefaultPorts(replaceAuthHostWithRealHost(cr.getAdminUrl())));

        if (cr.getRedirectUris() != null && !cr.getRedirectUris().isEmpty()) {
            List<String> fixedUrls = new ArrayList<>(cr.getRedirectUris().size());
            for (String url : cr.getRedirectUris()) {
                fixedUrls.add(removeDefaultPorts(replaceAuthHostWithRealHost(url)));
            }
            cr.setRedirectUris(fixedUrls);
        }
    }

    private String replaceAuthHostWithRealHost(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("localhost", AUTH_SERVER_HOST);
    }

    private void modifyRedirectUrls(ClientRepresentation cr) {
        if (cr.getRedirectUris() != null && !cr.getRedirectUris().isEmpty()) {
            List<String> fixedRedirectUrls = new ArrayList<>(cr.getRedirectUris().size());
            for (String url : cr.getRedirectUris()) {
                fixedRedirectUrls.add(replaceHttpValuesWithHttps(url));
            }
            cr.setRedirectUris(fixedRedirectUrls);
        }
    }

    private void modifyMainUrls(ClientRepresentation cr) {
        cr.setBaseUrl(replaceHttpValuesWithHttps(cr.getBaseUrl()));
        cr.setAdminUrl(replaceHttpValuesWithHttps(cr.getAdminUrl()));
    }

    private String replaceHttpValuesWithHttps(String input) {
        if (input == null) {
            return null;
        }
        if ("".equals(input)) {
            return "";
        }
        return input
                .replace("http", "https")
                .replace("8080", "8543")
                .replace("8180", "8543");
    }
}
