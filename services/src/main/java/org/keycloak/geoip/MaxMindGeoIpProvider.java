package org.keycloak.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Default out-of-the-box GeoIpProvider, backed by MaxMind's GeoLite2-City database via the
 * official `com.maxmind.geoip2:geoip2` reader. Satisfies the maintainers' guidance to
 * "provide any out-of-the-box implementation using a standard service" — deployments that
 * need something else (a paid MaxMind web service, an internal geo API, ipapi.co, etc.)
 * implement GeoIpProvider/GeoIpProvider.Factory themselves and register it the same way.
 *
 * LICENSING / DISTRIBUTION - deliberately NOT bundled:
 *   GeoLite2 requires a free MaxMind account and its own end-user license agreement; the
 *   .mmdb file itself may not be redistributed by third parties (i.e. this module/Keycloak
 *   cannot ship the database file in its own artifact or container image). Concretely this
 *   means:
 *     1. The admin creates their own MaxMind account and downloads GeoLite2-City.mmdb
 *        themselves: https://dev.maxmind.com/geoip/geolite2-free-geolocation-data
 *     2. The admin mounts/places that file somewhere Keycloak can read, and points this
 *        provider at it via the `geoLite2DatabasePath` config option (see FactoryImpl).
 *     3. Optionally, the admin runs MaxMind's `geoipupdate` tool on a cron schedule to keep
 *        the database current — GeoLite2 databases are republished roughly weekly and stale
 *        databases silently degrade accuracy rather than erroring.
 *   If no path is configured, this provider simply omits location from notification emails
 *   (see FactoryImpl.init) rather than failing - geolocation is cosmetic, never a hard
 *   dependency for the feature to function.
 *
 * The DatabaseReader is thread-safe and expensive to construct (memory-maps the .mmdb), so
 * it is built ONCE in the factory and shared across all provider instances for the process
 * lifetime, following the same "expensive singleton behind a cheap per-request Provider"
 * shape Keycloak uses elsewhere (e.g. connection pools behind JpaConnectionProvider).
 */
public class MaxMindGeoIpProvider implements GeoIpProvider {

    private static final Logger log = Logger.getLogger(MaxMindGeoIpProvider.class);

    private final DatabaseReader reader; // may be null if not configured; null-checked below

    MaxMindGeoIpProvider(DatabaseReader reader) {
        this.reader = reader;
    }

    @Override
    public String resolveLocation(String ipAddress) {
        if (reader == null || ipAddress == null) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            CityResponse response = reader.city(address);

            City city = response.getCity();
            Country country = response.getCountry();

            String cityName = city != null ? city.getName() : null;
            String countryName = country != null ? country.getName() : null;

            if (cityName != null && countryName != null) {
                return cityName + ", " + countryName;
            } else if (countryName != null) {
                return countryName;
            } else {
                return null;
            }
        } catch (AddressNotFoundException e) {
            // Expected for private/reserved ranges (127.0.0.1, 10.x.x.x, etc.) in dev/test
            // environments - not an error condition, just no data available.
            log.debugf("No GeoIP entry for %s (private/reserved range or not in database)", ipAddress);
            return null;
        } catch (GeoIp2Exception | IOException e) {
            log.warnf(e, "GeoIP lookup failed for %s", ipAddress);
            return null;
        }
    }

    @Override
    public void close() {
        // Intentionally a no-op per-instance: the DatabaseReader is a shared singleton
        // owned by FactoryImpl (see class javadoc), not per-provider-instance state.
        // FactoryImpl.close() is what actually closes the reader, at server shutdown.
    }

    public static class FactoryImpl implements GeoIpProvider.Factory {

        public static final String PROVIDER_ID = "maxmind";
        private volatile DatabaseReader sharedReader;

        @Override
        public GeoIpProvider create(KeycloakSession session) {
            return new MaxMindGeoIpProvider(sharedReader);
        }

        @Override
        public void init(Config.Scope config) {
            String databasePath = config.get("geoLite2DatabasePath");
            if (databasePath == null || databasePath.isBlank()) {
                log.info("No GeoLite2 database path configured (geoLite2DatabasePath) - " +
                        "'new login' emails will omit approximate location. See README for " +
                        "how to obtain a GeoLite2-City.mmdb file from MaxMind.");
                return;
            }

            File dbFile = new File(databasePath);
            if (!dbFile.exists() || !dbFile.canRead()) {
                log.warnf("geoLite2DatabasePath is set to '%s' but the file does not exist or " +
                        "is not readable - location will be omitted from emails until this is fixed.", databasePath);
                return;
            }

            try {
                // CHMCache trades ~2MB of heap for significantly faster repeat lookups -
                // reasonable default here since login volume can make repeated lookups of
                // the same ranges common (office networks, mobile carriers, etc.).
                this.sharedReader = new DatabaseReader.Builder(dbFile)
                        .withCache(new com.maxmind.db.CHMCache())
                        .build();
                log.infof("Loaded GeoLite2 database from %s", databasePath);
            } catch (IOException e) {
                log.warnf(e, "Failed to load GeoLite2 database from '%s' - location will be " +
                        "omitted from emails until this is fixed.", databasePath);
            }
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // no-op
        }

        @Override
        public void close() {
            if (sharedReader != null) {
                try {
                    sharedReader.close();
                } catch (IOException e) {
                    log.debug("Error closing GeoLite2 database reader", e);
                }
            }
        }

        @Override
        public String getId() {
            return PROVIDER_ID;
        }
    }
}