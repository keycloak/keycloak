package org.keycloak.models.untrustedlogin;

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;

/**
 * Typed accessor for this feature's per-realm settings, backed by RealmModel attributes.
 *
 * IMPORTANT - what this is and isn't:
 * This is NOT the same thing as adding first-class fields to Keycloak's own RealmModel /
 * RealmRepresentation (that would mean editing classes that live in Keycloak core -
 * services/src/main/java/org/keycloak/models/RealmModel.java, the JPA RealmEntity, a
 * Liquibase changelog if a dedicated column were used, RealmRepresentation.java, AND the
 * Admin Console React UI under js/apps/admin-ui - all of which are out of reach for a
 * standalone extension module like this one; that level of integration only happens if
 * this feature is merged directly into Keycloak core).
 *
 * What THIS class does, achievable entirely from an extension module: gives realm
 * attributes a single, typed, validated entry point instead of scattered
 * `Boolean.parseBoolean(realm.getAttribute("someKey"))` calls with no validation and no
 * documented defaults. Paired with UntrustedLoginAdminResourceProvider, this is also
 * exposed over the Admin REST API as a proper typed resource (GET/PUT with a real
 * representation class) rather than requiring admins to poke at attributes directly via
 * the generic realm PUT endpoint.
 */
public final class UntrustedLoginRealmConfig {

    private static final Logger log = Logger.getLogger(UntrustedLoginRealmConfig.class);

    public static final String ATTR_ENABLED = "untrustedLoginNotificationsEnabled";
    public static final String ATTR_TRUST_WINDOW_DAYS = "untrustedLoginTrustWindowDays";

    public static final boolean DEFAULT_ENABLED = false;
    public static final int DEFAULT_TRUST_WINDOW_DAYS = 90;
    public static final int MIN_TRUST_WINDOW_DAYS = 1;
    public static final int MAX_TRUST_WINDOW_DAYS = 3650; // 10 years - sanity ceiling

    private UntrustedLoginRealmConfig() {
    }

    public static boolean isEnabled(RealmModel realm) {
        String raw = realm.getAttribute(ATTR_ENABLED);
        if (raw == null) {
            return DEFAULT_ENABLED;
        }
        return Boolean.parseBoolean(raw);
    }

    public static void setEnabled(RealmModel realm, boolean enabled) {
        realm.setAttribute(ATTR_ENABLED, Boolean.toString(enabled));
    }

    public static int getTrustWindowDays(RealmModel realm) {
        String raw = realm.getAttribute(ATTR_TRUST_WINDOW_DAYS);
        if (raw == null) {
            return DEFAULT_TRUST_WINDOW_DAYS;
        }
        try {
            int days = Integer.parseInt(raw);
            if (days < MIN_TRUST_WINDOW_DAYS || days > MAX_TRUST_WINDOW_DAYS) {
                log.warnf("Realm %s has %s=%d out of allowed range [%d, %d] - falling back to default %d",
                        realm.getName(), ATTR_TRUST_WINDOW_DAYS, days,
                        MIN_TRUST_WINDOW_DAYS, MAX_TRUST_WINDOW_DAYS, DEFAULT_TRUST_WINDOW_DAYS);
                return DEFAULT_TRUST_WINDOW_DAYS;
            }
            return days;
        } catch (NumberFormatException e) {
            log.warnf("Realm %s has non-numeric %s='%s' - falling back to default %d",
                    realm.getName(), ATTR_TRUST_WINDOW_DAYS, raw, DEFAULT_TRUST_WINDOW_DAYS);
            return DEFAULT_TRUST_WINDOW_DAYS;
        }
    }

    /**
     * @throws IllegalArgumentException if days is outside [MIN_TRUST_WINDOW_DAYS, MAX_TRUST_WINDOW_DAYS] -
     *         thrown (not silently clamped) here because this is the write path,
     *         reachable from the admin REST endpoint, where a bad value should be
     *         rejected with a 400 rather than silently coerced.
     */
    public static void setTrustWindowDays(RealmModel realm, int days) {
        if (days < MIN_TRUST_WINDOW_DAYS || days > MAX_TRUST_WINDOW_DAYS) {
            throw new IllegalArgumentException(
                    "trustWindowDays must be between " + MIN_TRUST_WINDOW_DAYS + " and " + MAX_TRUST_WINDOW_DAYS);
        }
        realm.setAttribute(ATTR_TRUST_WINDOW_DAYS, Integer.toString(days));
    }

    public static long getTrustWindowMillis(RealmModel realm) {
        return getTrustWindowDays(realm) * 24L * 60 * 60 * 1000;
    }
}