package org.keycloak.models.sessions.infinispan.untrustedlogin;

import org.infinispan.Cache;

import org.keycloak.models.untrustedlogin.UntrustedLoginProvider;
import org.keycloak.models.untrustedlogin.UntrustedLoginRealmConfig;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Default UntrustedLoginProvider, backed by a dedicated Infinispan cache
 * ("knownUserDevices") rather than a new JPA entity/table.
 *
 * Rationale (see issue discussion): Keycloak's UserSessionProvider already tracks
 * per-login IP + User-Agent, but only for the lifetime of the session (online sessions
 * are evicted on logout/expiry; offline sessions only exist for offline_access clients).
 * There is no existing durable "login history" to reuse. Rather than introduce a second,
 * disconnected persistence mechanism (a new JPA table + Liquibase changelog), this follows
 * the same distributed-cache approach already used for `sessions` / `offlineSessions`
 * (see InfinispanUserSessionProvider), which gives cluster-awareness for free via
 * Infinispan's existing replication/distribution config.
 *
 * Marshalling: values are UserDeviceHistory, a Protostream-annotated wrapper (see
 * UntrustedLoginProtoSchema). Reads/writes always rebuild a new UserDeviceHistory /
 * DeviceHistoryEntry list rather than mutating an existing instance in place - this
 * matters for a distributed Infinispan cache, since compute() must return a value whose
 * *identity or content change* Infinispan can detect and replicate; mutating fields on an
 * object already stored in the cache does not reliably propagate to other owners.
 *
 * Cache name and config are defined in cache-config/knownUserDevices-cache.xml - that file
 * needs to be merged into the deployment's cache-ispn.xml (see this module's README).
 */
public class InfinispanUntrustedLoginProvider implements UntrustedLoginProvider {

    private static final Logger log = Logger.getLogger(InfinispanUntrustedLoginProvider.class);

    public static final String CACHE_NAME = "knownUserDevices";

    private final KeycloakSession session;

    public InfinispanUntrustedLoginProvider(KeycloakSession session) {
        this.session = session;
    }

    private Cache<String, UserDeviceHistory> getCache() {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        return connections.getCache(CACHE_NAME);
    }

    private static String cacheKey(RealmModel realm, UserModel user) {
        // Namespaced by realm since Infinispan caches here are shared across realms,
        // same convention InfinispanUserSessionProvider follows for its keys.
        return realm.getId() + "::" + user.getId();
    }

    @Override
    public TrustResult checkTrust(RealmModel realm, UserModel user, String userAgent, String ipAddress) {
        Cache<String, UserDeviceHistory> cache = getCache();
        String key = cacheKey(realm, user);
        UserDeviceHistory history = cache.get(key);

        if (history == null || history.getEntries().isEmpty()) {
            log.debugf("No device history for user %s in realm %s - treating as untrusted login", user.getUsername(), realm.getName());
            return new TrustResult(false, false);
        }

        String deviceHash = FingerprintUtil.hashDevice(userAgent);
        String subnet = FingerprintUtil.toSubnet(ipAddress);
        long now = System.currentTimeMillis();
        long trustWindowMillis = UntrustedLoginRealmConfig.getTrustWindowMillis(realm);

        boolean knownDevice = false;
        boolean knownNetwork = false;

        for (DeviceHistoryEntry entry : history.getEntries()) {
            if (entry.isExpired(now, trustWindowMillis)) {
                continue;
            }
            if (entry.matchesDevice(deviceHash)) {
                knownDevice = true;
            }
            if (entry.matchesSubnet(subnet)) {
                knownNetwork = true;
            }
        }

        return new TrustResult(knownDevice, knownNetwork);
    }

    @Override
    public void recordLogin(RealmModel realm, UserModel user, String userAgent, String ipAddress) {
        Cache<String, UserDeviceHistory> cache = getCache();
        String key = cacheKey(realm, user);
        String deviceHash = FingerprintUtil.hashDevice(userAgent);
        String subnet = FingerprintUtil.toSubnet(ipAddress);
        long now = System.currentTimeMillis();
        long trustWindowMillis = UntrustedLoginRealmConfig.getTrustWindowMillis(realm);

        // compute() gives us atomicity for the read-modify-write under the cache's own
        // locking; we always return a NEW UserDeviceHistory instance (see class javadoc
        // on why in-place mutation is unsafe for a distributed cache).
        cache.compute(key, (k, existing) -> {
            List<DeviceHistoryEntry> current = existing != null
                    ? new ArrayList<>(existing.getEntries())
                    : new ArrayList<>();

            boolean updated = false;
            for (int i = 0; i < current.size(); i++) {
                DeviceHistoryEntry entry = current.get(i);
                if (entry.matchesDevice(deviceHash) && entry.getIpAddress().equals(ipAddress)) {
                    current.set(i, entry.withLastSeen(now));
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                current.add(new DeviceHistoryEntry(deviceHash, ipAddress, subnet, now, now));
            }

            // Opportunistically drop fully expired entries so the list doesn't grow
            // unbounded for users who log in from many short-lived locations over time.
            current.removeIf(e -> e.isExpired(now, trustWindowMillis));

            return new UserDeviceHistory(current);
        });
    }

    @Override
    public void forgetAllDevices(RealmModel realm, UserModel user) {
        getCache().remove(cacheKey(realm, user));
    }

    @Override
    public void close() {
        // no-op: cache handle is not owned per-request, nothing to release here
    }
}