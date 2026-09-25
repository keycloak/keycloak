package org.keycloak.events.untrustedlogin;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.keycloak.authentication.authenticators.browser.NewDeviceCheckAuthenticator;
import org.keycloak.geoip.GeoIpProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends a "new device/location" email when a login is flagged untrusted by
 * NewDeviceCheckAuthenticator. Reuses Keycloak's existing EmailTemplateProvider /
 * EmailSenderProvider pipeline (the same one used for e.g. "email-verification" and
 * "password change" notifications) rather than a new notification channel - this is
 * what satisfies "email is sufficient for v1, additional channels via custom SPI" from
 * the scoping discussion: anyone wanting push/SMS can implement their own
 * EventListenerProvider the same way this one does, listening for the same event.
 */
public class UntrustedLoginEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(UntrustedLoginEventListenerProvider.class);

    private final KeycloakSession session;

    public UntrustedLoginEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.LOGIN) {
            return;
        }

        RealmModel realm = session.realms().getRealm(event.getRealmId());
        if (realm == null) {
            return;
        }

        // NewDeviceCheckAuthenticator.authenticate() writes these same keys onto the
        // EventBuilder via context.getEvent().detail(...) at authenticate() time (in
        // addition to the auth session note it also sets, which is only relevant within
        // the flow execution itself) - that's what makes them show up here, on the actual
        // fired Event object, once the login completes.
        Map<String, String> details = event.getDetails();
        boolean isNewDevice = details != null
                && Boolean.parseBoolean(details.get(NewDeviceCheckAuthenticator.NOTE_NEW_DEVICE_DETECTED));

        if (!isNewDevice) {
            return;
        }

        UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null || user.getEmail() == null) {
            log.debugf("Skipping new-device email - no user/email for userId %s", event.getUserId());
            return;
        }

        String ipAddress = details.get(NewDeviceCheckAuthenticator.NOTE_LOGIN_IP);
        String userAgent = details.get(NewDeviceCheckAuthenticator.NOTE_LOGIN_UA);

        GeoIpProvider geoIp = session.getProvider(GeoIpProvider.class);
        String location = geoIp != null ? geoIp.resolveLocation(ipAddress) : null;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("ipAddress", ipAddress);
        attributes.put("userAgent", userAgent);
        attributes.put("approximateLocation", location != null ? location : "Unknown location");
        attributes.put("loginTime", event.getTime());

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .send("untrustedLoginSubject", "untrusted-login.ftl", attributes);
        } catch (EmailException e) {
            log.warn("Failed to send new-device login notification email for user " + user.getUsername(), e);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // not relevant to this feature
    }

    @Override
    public void close() {
        // no-op
    }
}