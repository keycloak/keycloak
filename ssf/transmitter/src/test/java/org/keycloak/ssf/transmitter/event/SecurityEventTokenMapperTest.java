package org.keycloak.ssf.transmitter.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.ssf.event.risc.RiscAccountDisabled;
import org.keycloak.ssf.event.risc.RiscAccountEnabled;
import org.keycloak.ssf.event.risc.RiscAccountPurged;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.subject.PurgedUserSnapshot;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the RISC {@code account-disabled} / {@code account-enabled} /
 * {@code account-purged} mapping added to {@link SecurityEventTokenMapper}: the
 * brute-force permanent-lockout {@link Event} path, the admin "enable/disable a
 * user" {@link AdminEvent} path (see
 * {@link SecurityEventTokenMapper#isEnabledStateChangeAdminEvent}), and the two
 * deletion paths (see {@link SecurityEventTokenMapper#isUserPurgeAdminEvent}).
 *
 * <p>The purge tests cover the near misses as well as the hits: Keycloak has
 * several paths that delete a user row without the account ceasing to exist, and
 * they are kept out by the operation type and resource type rather than by
 * anything deliberate, so they are pinned here.
 *
 * <p>Most methods here are session-independent — subject resolution defaults to
 * {@code iss_sub} (no organization/email lookups), so the shared mapper is
 * constructed with a {@code null} session and a stub issuer, mirroring how
 * {@link SsfTransmitterEventListenerTest} constructs the listener with a
 * {@code null} session for predicates that don't touch it.
 *
 * <p>The purge generator is the exception: it refuses to emit without a
 * pre-removal snapshot, since that snapshot is the only evidence the deletion was
 * a real purge rather than a federated local-only removal. Those tests use
 * {@link #purgeMapper()}, which carries a session with a captured snapshot.
 */
class SecurityEventTokenMapperTest {

    private static final String USER_ID = "user-123";

    private static final String REALM_ID = "realm-1";

    @BeforeAll
    static void initProfile() {
        // PurgedUserSnapshot.capture consults Organizations.isEnabled, which reads the profile.
        Profile.configure(new PropertiesProfileConfigResolver(new Properties()));
    }

    private final SecurityEventTokenMapper mapper =
            new SecurityEventTokenMapper(null, null, session -> "https://issuer.example/realms/test");

    /**
     * A mapper whose session carries a snapshot for {@link #USER_ID}, as the real
     * emission path always does — capture runs on {@code UserPreRemovedEvent} before
     * the event this mapper converts is fired.
     */
    private SecurityEventTokenMapper purgeMapper() {
        RealmModel realm = mock(RealmModel.class);
        lenient().when(realm.getId()).thenReturn(REALM_ID);

        KeycloakContext context = mock(KeycloakContext.class);
        lenient().when(context.getRealm()).thenReturn(realm);
        lenient().when(context.getHttpRequest()).thenReturn(mock(HttpRequest.class));

        KeycloakSession session = mock(KeycloakSession.class);
        lenient().when(session.getContext()).thenReturn(context);

        Map<String, Object> attributes = new HashMap<>();
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        lenient().when(session.getAttribute(anyString(), any(Class.class))).thenAnswer(invocation -> {
            Object value = attributes.get(invocation.<String>getArgument(0));
            return value == null ? null : invocation.<Class<?>>getArgument(1).cast(value);
        });

        OrganizationProvider orgProvider = mock(OrganizationProvider.class);
        lenient().when(orgProvider.isEnabled()).thenReturn(false);
        lenient().when(session.getProvider(OrganizationProvider.class)).thenReturn(orgProvider);

        UserModel user = mock(UserModel.class);
        lenient().when(user.getId()).thenReturn(USER_ID);
        lenient().when(user.getUsername()).thenReturn("purged");
        lenient().when(user.getAttributes()).thenReturn(Map.of(UserModel.USERNAME, List.of("purged")));
        // A local user: no federation link, so the removal is a real purge.
        lenient().when(user.getFederationLink()).thenReturn(null);
        lenient().when(user.getGroupsStream()).thenAnswer(invocation -> Stream.empty());
        lenient().when(user.getRoleMappingsStream()).thenAnswer(invocation -> Stream.empty());

        PurgedUserSnapshot.capture(session, realm, user);

        return new SecurityEventTokenMapper(session, null, ignored -> "https://issuer.example/realms/test");
    }

    // ----- brute-force permanent lockout (Event path) -----

    @Test
    void canConvert_permanentLockoutEvent_true() {
        Event event = new Event();
        event.setType(EventType.USER_DISABLED_BY_PERMANENT_LOCKOUT);
        event.setUserId(USER_ID);

        assertTrue(mapper.canConvert(event));
    }

    @Test
    void toSecurityEventToken_permanentLockoutEvent_producesAccountDisabledWithBruteForceReason() {
        Event event = new Event();
        event.setType(EventType.USER_DISABLED_BY_PERMANENT_LOCKOUT);
        event.setUserId(USER_ID);

        SsfSecurityEventToken token = mapper.toSecurityEventToken(event, streamConfig());

        assertNotNull(token);
        Object payload = token.getEvents().get(RiscAccountDisabled.TYPE);
        assertTrue(payload instanceof RiscAccountDisabled);
        RiscAccountDisabled accountDisabled = (RiscAccountDisabled) payload;
        assertEquals(RiscAccountDisabled.REASON_BRUTE_FORCE, accountDisabled.getReason());
        // No AdminEvent on this path, but brute-force lockout is Keycloak's own
        // policy engine acting, not the user's choice — must not be USER.
        assertEquals(InitiatingEntity.POLICY, accountDisabled.getInitiatingEntity());
    }

    // ----- admin-initiated enable/disable (AdminEvent path) -----

    @Test
    void canConvert_adminEnabledStateChange_true() {
        AdminEvent adminEvent = adminUserUpdateEvent("true", "false");

        assertTrue(mapper.canConvert(adminEvent));
    }

    @Test
    void canConvert_adminUpdateWithoutEnabledChange_false() {
        // Generic profile update (e.g. name/email) — no previous/updated
        // enabled details attached, since the value didn't change.
        AdminEvent adminEvent = adminUserUpdateEvent(null, null);

        assertFalse(mapper.canConvert(adminEvent));
    }

    @Test
    void canConvert_adminUpdateWithUnchangedEnabledDetail_false() {
        // Defensive: if both details were ever attached with the same value,
        // that is not a transition and must not be reported as one.
        AdminEvent adminEvent = adminUserUpdateEvent("true", "true");

        assertFalse(mapper.canConvert(adminEvent));
    }

    @Test
    void isEnabledStateChangeAdminEvent_nonUpdateOperation_false() {
        // The enabled-state gate requires UPDATE: the same bare users/{id} path with
        // a different verb is never an enable/disable transition, whatever details it
        // happens to carry. DELETE on this path is a purge, which canConvert()
        // accepts through a different gate — hence the assertion on the specific
        // predicate rather than on canConvert().
        AdminEvent adminEvent = adminUserUpdateEvent("true", "false");
        adminEvent.setOperationType(OperationType.DELETE);

        assertFalse(mapper.isEnabledStateChangeAdminEvent(adminEvent));
        assertTrue(mapper.isUserPurgeAdminEvent(adminEvent));
    }

    @Test
    void canConvert_createOperationOnUserPath_false() {
        AdminEvent adminEvent = adminUserUpdateEvent("true", "false");
        adminEvent.setOperationType(OperationType.CREATE);

        assertFalse(mapper.canConvert(adminEvent));
    }

    @Test
    void canConvert_subResourcePath_notPickedUpByEnabledStateGate() {
        // Bare "users/{id}" is required — a sub-path must not be picked up
        // by the enabled-state-change gate itself. Using ".../groups" here
        // (rather than ".../logout") keeps this test isolated from the
        // pre-existing "log out all sessions" pattern, which legitimately
        // makes canConvert() return true for a ".../logout" path regardless
        // of any enabled-state details.
        AdminEvent adminEvent = adminUserUpdateEvent("true", "false");
        adminEvent.setResourcePath("users/" + USER_ID + "/groups");

        assertFalse(mapper.isEnabledStateChangeAdminEvent(adminEvent));
        assertFalse(mapper.canConvert(adminEvent));
    }

    @Test
    void toSecurityEventToken_adminDisablesUser_producesAccountDisabledWithAdminReason() {
        AdminEvent adminEvent = adminUserUpdateEvent("true", "false");

        SsfSecurityEventToken token = mapper.toSecurityEventToken(adminEvent, streamConfig());

        assertNotNull(token);
        Object payload = token.getEvents().get(RiscAccountDisabled.TYPE);
        assertTrue(payload instanceof RiscAccountDisabled);
        RiscAccountDisabled accountDisabled = (RiscAccountDisabled) payload;
        assertEquals(RiscAccountDisabled.REASON_ADMIN, accountDisabled.getReason());
        assertEquals(InitiatingEntity.ADMIN, accountDisabled.getInitiatingEntity());
    }

    @Test
    void toSecurityEventToken_adminEnablesUser_producesAccountEnabled() {
        AdminEvent adminEvent = adminUserUpdateEvent("false", "true");

        SsfSecurityEventToken token = mapper.toSecurityEventToken(adminEvent, streamConfig());

        assertNotNull(token);
        Object payload = token.getEvents().get(RiscAccountEnabled.TYPE);
        assertTrue(payload instanceof RiscAccountEnabled);
        assertEquals(InitiatingEntity.ADMIN, ((RiscAccountEnabled) payload).getInitiatingEntity());
    }

    @Test
    void logoutWithoutDetails_isNotIgnored() {
        Event event = new Event();
        event.setType(EventType.LOGOUT);
        // details intentionally left null, which is the RP-initiated logout path
        // without post_logout_redirect_uri never calls detail()

        assertFalse(mapper.shouldIgnoreLogout(event),
                "a real user logout without details must be propagated, not dropped or NPE");
        assertTrue(mapper.canConvert(event),
                "canConvert must survive a LOGOUT event with null details");
    }

    @Test
    void logoutWithExpiredSessionReason_isIgnored() {
        Event event = new Event();
        event.setType(EventType.LOGOUT);
        event.setDetails(Map.of(Details.REASON, Details.USER_SESSION_EXPIRED_REASON));

        assertTrue(mapper.shouldIgnoreLogout(event),
                "expired session cleanup is not a real logout and must not emit a SET");
    }

    @Test
    void logoutWithUnrelatedDetails_isNotIgnored() {
        Event event = new Event();
        event.setType(EventType.LOGOUT);
        event.setDetails(Map.of(Details.REDIRECT_URI, "https://rp.example.com/logged-out"));

        assertFalse(mapper.shouldIgnoreLogout(event),
                "details without a REASON entry must be treated like a real logout");
    }

    // ----- account purge (admin DELETE + self-service DELETE_ACCOUNT) -----

    @Test
    void canConvert_adminUserDelete_true() {
        assertTrue(mapper.canConvert(adminUserDeleteEvent("users/" + USER_ID)));
    }

    @Test
    void toSecurityEventToken_withoutSnapshot_doesNotEmit() {
        // The snapshot is the only evidence the deletion was a real purge. The shared
        // mapper has no session and therefore no snapshot, which stands in for the
        // production case where capture failed and swallowed its own exception.
        assertNull(mapper.toSecurityEventToken(deleteAccountEvent(), streamConfig()));
        assertNull(mapper.toSecurityEventToken(
                adminUserDeleteEvent("users/" + USER_ID), streamConfig()));
    }

    @Test
    void toSecurityEventToken_adminDeletesUser_producesAccountPurgedWithAdminEntity() {
        SsfSecurityEventToken token = purgeMapper().toSecurityEventToken(
                adminUserDeleteEvent("users/" + USER_ID), streamConfig());

        assertNotNull(token);
        Object payload = token.getEvents().get(RiscAccountPurged.TYPE);
        assertTrue(payload instanceof RiscAccountPurged);
        assertEquals(InitiatingEntity.ADMIN, ((RiscAccountPurged) payload).getInitiatingEntity());
    }

    @Test
    void canConvert_deleteAccountEvent_true() {
        assertTrue(mapper.canConvert(deleteAccountEvent()));
    }

    @Test
    void toSecurityEventToken_deleteAccountEvent_producesAccountPurgedWithUserEntity() {
        SsfSecurityEventToken token = purgeMapper().toSecurityEventToken(deleteAccountEvent(), streamConfig());

        assertNotNull(token);
        Object payload = token.getEvents().get(RiscAccountPurged.TYPE);
        assertTrue(payload instanceof RiscAccountPurged);
        // Self-service deletion has no AdminEvent, and unlike the disable path
        // "not admin-initiated" here genuinely does mean the user did it.
        assertEquals(InitiatingEntity.USER, ((RiscAccountPurged) payload).getInitiatingEntity());
    }

    @Test
    void canConvert_userSubResourceDelete_notPurge() {
        // Only the bare users/{id} path is a purge. A DELETE deeper in the user
        // resource removes part of the user, not the account.
        AdminEvent adminEvent = adminUserDeleteEvent("users/" + USER_ID + "/groups");

        assertFalse(mapper.isUserPurgeAdminEvent(adminEvent));
        assertFalse(mapper.canConvert(adminEvent));
    }

    // ----- near misses: paths that delete a user without purging an account -----

    @Test
    void canConvert_partialImportOverwrite_false() {
        // A partial import under the OVERWRITE policy really does delete the user
        // row — then recreates it in the same request. It reports that as UPDATE on
        // users/{id}, one enum value away from the purge gate. The account never
        // ceased to exist, so no purge SET may be emitted.
        AdminEvent adminEvent = adminUserUpdateEvent(null, null);

        assertFalse(mapper.isUserPurgeAdminEvent(adminEvent));
        assertFalse(mapper.canConvert(adminEvent));
    }

    @Test
    void canConvert_clientDeleteRemovingServiceAccount_false() {
        // Deleting a client removes its service-account user, but reports the
        // deletion as ResourceType.CLIENT at clients/{id}. Service accounts are not
        // human subjects and must never produce a purge SET.
        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setResourceType(ResourceType.CLIENT);
        adminEvent.setOperationType(OperationType.DELETE);
        adminEvent.setResourcePath("clients/client-123");
        adminEvent.setDetails(new HashMap<>());

        assertFalse(mapper.canConvert(adminEvent));
    }

    private StreamConfig streamConfig() {
        return new StreamConfig();
    }

    /**
     * Builds the {@link AdminEvent} shape {@code UserResource.deleteUser} produces
     * for {@code DELETE /admin/realms/{realm}/users/{id}} — same bare resource path
     * as the update event, distinguished only by {@link OperationType#DELETE}.
     */
    private AdminEvent adminUserDeleteEvent(String resourcePath) {
        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setResourceType(ResourceType.USER);
        adminEvent.setOperationType(OperationType.DELETE);
        adminEvent.setResourcePath(resourcePath);
        adminEvent.setDetails(new HashMap<>());

        return adminEvent;
    }

    /**
     * Builds the {@code DELETE_ACCOUNT} {@link Event} the account console's
     * delete-account required action fires after removing the user.
     */
    private Event deleteAccountEvent() {
        Event event = new Event();
        event.setType(EventType.DELETE_ACCOUNT);
        event.setUserId(USER_ID);
        event.setDetails(new HashMap<>());

        return event;
    }

    /**
     * Builds the {@link AdminEvent} shape {@code UserResource.updateUser}
     * produces for {@code PUT /admin/realms/{realm}/users/{id}}: bare
     * {@code users/{id}} resource path, {@code ResourceType.USER},
     * {@code OperationType.UPDATE}, and — only when the enabled flag
     * actually changed — the {@link Details#PREVIOUS_ENABLED} /
     * {@link Details#UPDATED_ENABLED} detail pair.
     */
    private AdminEvent adminUserUpdateEvent(String previousEnabled, String updatedEnabled) {
        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setResourceType(ResourceType.USER);
        adminEvent.setOperationType(OperationType.UPDATE);
        adminEvent.setResourcePath("users/" + USER_ID);

        Map<String, String> details = new HashMap<>();
        if (previousEnabled != null) {
            details.put(Details.PREVIOUS_ENABLED, previousEnabled);
        }
        if (updatedEnabled != null) {
            details.put(Details.UPDATED_ENABLED, updatedEnabled);
        }
        adminEvent.setDetails(details);

        return adminEvent;
    }
}
