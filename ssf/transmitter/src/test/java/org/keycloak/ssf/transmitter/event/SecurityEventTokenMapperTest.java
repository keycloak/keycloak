package org.keycloak.ssf.transmitter.event;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.ssf.event.risc.RiscAccountDisabled;
import org.keycloak.ssf.event.risc.RiscAccountEnabled;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the RISC {@code account-disabled} / {@code account-enabled}
 * mapping added to {@link SecurityEventTokenMapper}: the brute-force
 * permanent-lockout {@link Event} path and the admin "enable/disable a user"
 * {@link AdminEvent} path (see {@link SecurityEventTokenMapper#isEnabledStateChangeAdminEvent}).
 *
 * <p>Session-independent methods only — subject resolution defaults to
 * {@code iss_sub} (no organization/email lookups), so the mapper is
 * constructed with a {@code null} session and a stub issuer, mirroring how
 * {@link SsfTransmitterEventListenerTest} constructs the listener with a
 * {@code null} session for predicates that don't touch it.
 */
class SecurityEventTokenMapperTest {

    private static final String USER_ID = "user-123";

    private final SecurityEventTokenMapper mapper =
            new SecurityEventTokenMapper(null, null, session -> "https://issuer.example/realms/test");

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
    void canConvert_nonUpdateOperation_false() {
        AdminEvent adminEvent = adminUserUpdateEvent("true", "false");
        adminEvent.setOperationType(OperationType.DELETE);

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

    private StreamConfig streamConfig() {
        return new StreamConfig();
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
