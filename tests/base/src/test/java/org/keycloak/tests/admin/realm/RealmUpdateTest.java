package org.keycloak.tests.admin.realm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.log.JBossLoggingEventListenerProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.userprofile.UserProfileProvider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmUpdateTest extends AbstractRealmTest {

    /**
     * KEYCLOAK-1990 1991
     */
    @Test
    public void renameRealmTest() {
        RealmRepresentation realm1 = new RealmRepresentation();
        realm1.setRealm("test-immutable");
        adminClient.realms().create(realm1);
        realm1 = adminClient.realms().realm("test-immutable").toRepresentation();
        realm1.setRealm("test-immutable-old");
        adminClient.realms().realm("test-immutable").update(realm1);
        assertThat(adminClient.realms().realm("test-immutable-old").toRepresentation(), notNullValue());

        RealmRepresentation realm2 = new RealmRepresentation();
        realm2.setRealm("test-immutable");
        adminClient.realms().create(realm2);
        assertThat(adminClient.realms().realm("test-immutable").toRepresentation(), notNullValue());

        adminClient.realms().realm("test-immutable").remove();
        adminClient.realms().realm("test-immutable-old").remove();
    }

    @Test
    public void renameRealm() {
        String OLD = "old";
        String NEW = "new";

        RealmRepresentation rep = new RealmRepresentation();
        rep.setId(OLD);
        rep.setRealm(OLD);

        adminClient.realms().create(rep);

        Map<String, String> newBaseUrls = new HashMap<>();
        Map<String, List<String>> newRedirectUris = new HashMap<>();

        // memorize all existing clients with their soon-to-be URIs
        adminClient.realm(OLD).clients().findAll().forEach(client -> {
            if (client.getBaseUrl() != null && client.getBaseUrl().contains("/" + OLD + "/")) {
                newBaseUrls.put(client.getClientId(), client.getBaseUrl().replace("/" + OLD + "/", "/" + NEW + "/"));
            }
            if (client.getRedirectUris() != null) {
                newRedirectUris.put(
                        client.getClientId(),
                        client.getRedirectUris()
                                .stream()
                                .map(redirectUri -> redirectUri.replace("/" + OLD + "/", "/" + NEW + "/"))
                                .collect(Collectors.toList())
                );
            }
        });

        // at least those three default clients should be in the list of things to be tested
        assertThat(newBaseUrls.keySet(), hasItems(Constants.ADMIN_CONSOLE_CLIENT_ID, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, Constants.ACCOUNT_CONSOLE_CLIENT_ID));
        assertThat(newRedirectUris.keySet(), hasItems(Constants.ADMIN_CONSOLE_CLIENT_ID, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, Constants.ACCOUNT_CONSOLE_CLIENT_ID));

        rep.setRealm(NEW);
        adminClient.realm(OLD).update(rep);

        // Check client in master realm renamed
        Assertions.assertEquals(0, adminClient.realm("master").clients().findByClientId("old-realm").size());
        Assertions.assertEquals(1, adminClient.realm("master").clients().findByClientId("new-realm").size());

        ClientRepresentation adminConsoleClient = adminClient.realm(NEW).clients().findByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID).get(0);
        assertEquals(Constants.AUTH_ADMIN_URL_PROP, adminConsoleClient.getRootUrl());

        ClientRepresentation accountClient = adminClient.realm(NEW).clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
        assertEquals(Constants.AUTH_BASE_URL_PROP, accountClient.getRootUrl());

        ClientRepresentation accountConsoleClient = adminClient.realm(NEW).clients().findByClientId(Constants.ACCOUNT_CONSOLE_CLIENT_ID).get(0);
        assertEquals(Constants.AUTH_BASE_URL_PROP, accountConsoleClient.getRootUrl());

        newBaseUrls.forEach((clientId, baseUrl) -> {
            assertEquals(baseUrl, adminClient.realm(NEW).clients().findByClientId(clientId).get(0).getBaseUrl());
        });
        newRedirectUris.forEach((clientId, redirectUris) -> {
            assertEquals(redirectUris, adminClient.realm(NEW).clients().findByClientId(clientId).get(0).getRedirectUris());
        });

        adminClient.realms().realm(NEW).remove();
    }

    @Test
    public void updateRealmEventsConfig() {
        RealmEventsConfigRepresentation rep = managedRealm.admin().getRealmEventsConfig();
        RealmEventsConfigRepresentation repOrig = copyRealmEventsConfigRepresentation(rep);

        // the "jboss-logging" listener should be enabled by default
        assertTrue(rep.getEventsListeners().contains(JBossLoggingEventListenerProviderFactory.ID), "jboss-logging should be enabled initially");

        // first modification => remove "event-queue", should be sent to the queue
        rep.setEnabledEventTypes(List.of(EventType.LOGIN.name(), EventType.LOGIN_ERROR.name()));
        rep.setEventsListeners(List.of(JBossLoggingEventListenerProviderFactory.ID));
        rep.setEventsExpiration(36000L);
        rep.setEventsEnabled(false);
        rep.setAdminEventsEnabled(false);
        rep.setAdminEventsDetailsEnabled(true);
        managedRealm.admin().updateRealmEventsConfig(rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, "events/config", rep, ResourceType.REALM);
        RealmEventsConfigRepresentation actual = managedRealm.admin().getRealmEventsConfig();
        checkRealmEventsConfigRepresentation(rep, actual);

        // second modification => should not be sent cos event-queue was removed in the first mod
        rep.setEnabledEventTypes(Arrays.asList(EventType.LOGIN.name(),
                EventType.LOGIN_ERROR.name(), EventType.CLIENT_LOGIN.name()));
        managedRealm.admin().updateRealmEventsConfig(rep);
        Assertions.assertNull(adminEvents.poll());
        actual = managedRealm.admin().getRealmEventsConfig();
        checkRealmEventsConfigRepresentation(rep, actual);

        // third modification => restore queue => should be sent and recovered
        managedRealm.admin().updateRealmEventsConfig(repOrig);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, "events/config", repOrig, ResourceType.REALM);
        actual = managedRealm.admin().getRealmEventsConfig();
        checkRealmEventsConfigRepresentation(repOrig, actual);
    }

    @Test
    public void updateRealmWithReservedCharInNameOrEmptyName() {
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        rep.setRealm("fo#o");
        assertThrows(BadRequestException.class, () -> managedRealm.admin().update(rep));
        rep.setRealm("");
        assertThrows(BadRequestException.class, () -> managedRealm.admin().update(rep));
    }

    @Test
    public void updateRealm() {
        // first change
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        rep.setSsoSessionIdleTimeout(123);
        rep.setSsoSessionMaxLifespan(12);
        rep.setSsoSessionIdleTimeoutRememberMe(33);
        rep.setSsoSessionMaxLifespanRememberMe(34);
        rep.setAccessCodeLifespanLogin(1234);
        rep.setActionTokenGeneratedByAdminLifespan(2345);
        rep.setActionTokenGeneratedByUserLifespan(3456);
        rep.setRegistrationAllowed(true);
        rep.setRegistrationEmailAsUsername(true);
        rep.setEditUsernameAllowed(true);
        rep.setUserManagedAccessAllowed(true);

        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).representation(rep).resourceType(ResourceType.REALM);

        rep = managedRealm.admin().toRepresentation();

        assertEquals(123, rep.getSsoSessionIdleTimeout().intValue());
        assertEquals(12, rep.getSsoSessionMaxLifespan().intValue());
        assertEquals(33, rep.getSsoSessionIdleTimeoutRememberMe().intValue());
        assertEquals(34, rep.getSsoSessionMaxLifespanRememberMe().intValue());
        assertEquals(1234, rep.getAccessCodeLifespanLogin().intValue());
        assertEquals(2345, rep.getActionTokenGeneratedByAdminLifespan().intValue());
        assertEquals(3456, rep.getActionTokenGeneratedByUserLifespan().intValue());
        assertEquals(Boolean.TRUE, rep.isRegistrationAllowed());
        assertEquals(Boolean.TRUE, rep.isRegistrationEmailAsUsername());
        assertEquals(Boolean.TRUE, rep.isEditUsernameAllowed());
        assertEquals(Boolean.TRUE, rep.isUserManagedAccessAllowed());

        // second change
        rep.setRegistrationAllowed(false);
        rep.setRegistrationEmailAsUsername(false);
        rep.setEditUsernameAllowed(false);
        rep.setUserManagedAccessAllowed(false);

        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).resourceType(ResourceType.REALM);

        rep = managedRealm.admin().toRepresentation();
        assertEquals(Boolean.FALSE, rep.isRegistrationAllowed());
        assertEquals(Boolean.FALSE, rep.isRegistrationEmailAsUsername());
        assertEquals(Boolean.FALSE, rep.isEditUsernameAllowed());
        assertEquals(Boolean.FALSE, rep.isUserManagedAccessAllowed());

        rep.setAccessCodeLifespanLogin(0);
        rep.setAccessCodeLifespanUserAction(0);
        try {
            managedRealm.admin().update(rep);
            Assertions.fail("Not expected to successfully update the realm");
        } catch (Exception expected) {
            // Expected exception
            assertEquals("HTTP 400 Bad Request", expected.getMessage());
        }
    }

    @Test
    public void updateRealmWithNewRepresentation() {
        // first change
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEditUsernameAllowed(true);
        rep.setSupportedLocales(new HashSet<>(Arrays.asList("en", "de")));

        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).resourceType(ResourceType.REALM);

        rep = managedRealm.admin().toRepresentation();

        assertEquals(Boolean.TRUE, rep.isEditUsernameAllowed());
        assertEquals(2, rep.getSupportedLocales().size());

        // second change
        rep = new RealmRepresentation();
        rep.setEditUsernameAllowed(false);

        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).resourceType(ResourceType.REALM);

        rep = managedRealm.admin().toRepresentation();
        assertEquals(Boolean.FALSE, rep.isEditUsernameAllowed());
        assertEquals(2, rep.getSupportedLocales().size());
    }

    @Test
    public void testNoUserProfileProviderComponentUponRealmChange() {
        String realmName = "new-realm";
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(realmName);

        adminClient.realms().create(rep);

        assertThat(adminClient.realm(realmName).components().query(null, UserProfileProvider.class.getName()), empty());

        rep.setDisplayName("displayName");
        adminClient.realm(realmName).update(rep);

        // this used to return non-empty collection
        assertThat(adminClient.realm(realmName).components().query(null, UserProfileProvider.class.getName()), empty());

        adminClient.realms().realm(realmName).remove();
    }

    private RealmEventsConfigRepresentation copyRealmEventsConfigRepresentation(RealmEventsConfigRepresentation rep) {
        RealmEventsConfigRepresentation recr = new RealmEventsConfigRepresentation();
        recr.setEnabledEventTypes(rep.getEnabledEventTypes());
        recr.setEventsListeners(rep.getEventsListeners());
        recr.setEventsExpiration(rep.getEventsExpiration());
        recr.setEventsEnabled(rep.isEventsEnabled());
        recr.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        recr.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());
        return recr;
    }

    private void checkRealmEventsConfigRepresentation(RealmEventsConfigRepresentation expected,
                                                      RealmEventsConfigRepresentation actual) {
        assertEquals(expected.getEnabledEventTypes().size(), actual.getEnabledEventTypes().size());
        assertTrue(actual.getEnabledEventTypes().containsAll(expected.getEnabledEventTypes()));
        assertEquals(expected.getEventsListeners().size(), actual.getEventsListeners().size());
        assertTrue(actual.getEventsListeners().containsAll(expected.getEventsListeners()));
        assertEquals(expected.getEventsExpiration(), actual.getEventsExpiration());
        assertEquals(expected.isEventsEnabled(), actual.isEventsEnabled());
        assertEquals(expected.isAdminEventsEnabled(), actual.isAdminEventsEnabled());
        assertEquals(expected.isAdminEventsDetailsEnabled(), actual.isAdminEventsDetailsEnabled());
    }
}
