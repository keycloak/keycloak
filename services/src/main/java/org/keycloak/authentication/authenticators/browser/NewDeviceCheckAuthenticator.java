package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.keycloak.models.untrustedlogin.UntrustedLoginProvider;
import org.keycloak.models.untrustedlogin.UntrustedLoginRealmConfig;

/**
 * Non-interactive, non-blocking authenticator. Placed as a flow step AFTER credential
 * validation (e.g. after "Username Password Form" in the browser flow). It never
 * challenges the user or fails the flow - it only:
 *   1. Checks whether this device/IP is known for the user (via UntrustedLoginProvider)
 *   2. Stamps the result onto BOTH the auth session note (for any later step in the SAME
 *      flow execution that wants to read it, e.g. a conditional step-up authenticator) AND
 *      the EventBuilder detail map (so it actually travels with the fired LOGIN event -
 *      see note below on why both are needed)
 *   3. Records this login as "known" for next time
 *
 * This intentionally mirrors the "notification only, not a challenge" scoping decision
 * from the issue discussion - MFA-on-new-device is explicitly out of scope for this flow
 * step. A deployment that wants step-up auth on untrusted devices can read the same
 * NEW_DEVICE_DETECTED auth session note from a separate conditional authenticator later
 * in the flow.
 *
 * WHY BOTH authSession.setAuthNote() AND context.getEvent().detail():
 * These are two different data paths with different lifetimes. Auth session notes only
 * live for the duration of the current authentication flow execution and are visible to
 * other authenticators in the SAME flow run - they are not what ends up on the Event
 * object Keycloak fires at the end of a successful login. context.getEvent() returns the
 * EventBuilder that IS accumulating the details for that fired event; calling .detail(...)
 * on it here is what makes NOTE_NEW_DEVICE_DETECTED actually show up in
 * event.getDetails() when UntrustedLoginEventListenerProvider.onEvent() runs. Missing this
 * call was the concrete gap flagged in the original skeleton - the email would never have
 * been sent because the listener was reading a detail that nothing ever set.
 */
public class NewDeviceCheckAuthenticator implements Authenticator {

    public static final String NOTE_NEW_DEVICE_DETECTED = "NEW_DEVICE_DETECTED";
    public static final String NOTE_LOGIN_IP = "NEW_DEVICE_LOGIN_IP";
    public static final String NOTE_LOGIN_UA = "NEW_DEVICE_LOGIN_UA";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();

        if (!UntrustedLoginRealmConfig.isEnabled(realm)) {
            context.success();
            return;
        }

        UserModel user = context.getUser();
        KeycloakSession session = context.getSession();
        String ipAddress = context.getConnection().getRemoteAddr();
        String userAgent = context.getHttpRequest().getHttpHeaders().getHeaderString("User-Agent");

        UntrustedLoginProvider trustProvider = session.getProvider(UntrustedLoginProvider.class);

        UntrustedLoginProvider.TrustResult result = trustProvider.checkTrust(realm, user, userAgent, ipAddress);

        if (result.isUntrusted()) {
            // (1) Auth-session note: readable by later steps in this same flow execution.
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            authSession.setAuthNote(NOTE_NEW_DEVICE_DETECTED, Boolean.TRUE.toString());
            authSession.setAuthNote(NOTE_LOGIN_IP, ipAddress);
            authSession.setAuthNote(NOTE_LOGIN_UA, userAgent);

            // (2) Event detail: this is what actually reaches the fired LOGIN event, and
            // therefore what UntrustedLoginEventListenerProvider.onEvent() can see via
            // event.getDetails(). Without this call the listener's lookup is always empty.
            context.getEvent()
                    .detail(NOTE_NEW_DEVICE_DETECTED, Boolean.TRUE.toString())
                    .detail(NOTE_LOGIN_IP, ipAddress)
                    .detail(NOTE_LOGIN_UA, userAgent);
        }

        // Record regardless of outcome, so a genuinely new device is "known" starting now.
        trustProvider.recordLogin(realm, user, userAgent, ipAddress);

        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // no user-facing action; authenticate() always resolves synchronously
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(org.keycloak.models.KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(org.keycloak.models.KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}