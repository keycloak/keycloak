package org.keycloak.geoip;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * Resolves an IP address to a human-readable approximate location (e.g. "Bengaluru, India")
 * for inclusion in the "new device" notification email.
 *
 * Deliberately decoupled from UntrustedLoginProvider: geolocation is cosmetic (it improves
 * the email's usefulness to the end user) and must never gate the trust decision itself,
 * since geo-IP databases are approximate and third-party dependent.
 *
 * A default implementation (MaxMindGeoIpProvider) ships using GeoLite2 as the out-of-the-box
 * option, per the "provide a default, but keep it swappable" scoping decision. Deployments
 * with stricter licensing constraints or an existing internal geo service implement this
 * interface and register it in META-INF/services to override the default.
 */
public interface GeoIpProvider extends Provider {

    /**
     * @return a short display string like "Bengaluru, India", or null if resolution failed
     *         / no database is configured. Callers must treat null as "omit location from
     *         the email" rather than an error.
     */
    String resolveLocation(String ipAddress);

    interface Factory extends ProviderFactory<GeoIpProvider> {
        @Override
        GeoIpProvider create(KeycloakSession session);
    }
}