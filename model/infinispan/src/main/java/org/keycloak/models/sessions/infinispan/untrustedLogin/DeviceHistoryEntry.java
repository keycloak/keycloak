package org.keycloak.models.sessions.infinispan.untrustedlogin;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.Objects;

/**
 * A single "known device" fact for a user: a device fingerprint paired with the
 * IP (or IP subnet) it was first seen from, plus timestamps used for trust-window expiry.
 *
 * Marshalled via Infinispan ProtoStream (not plain Java serialization) - see
 * UntrustedLoginProtoSchema for the schema registration. Requires Keycloak >= 26.2
 * (see README: "Why >= 26.2 is a hard requirement").
 *
 * @ProtoFactory marks the constructor Protostream uses to rebuild instances; every
 * @ProtoField-annotated getter must have a matching constructor parameter. Fields stay
 * `implements Serializable` too, purely as a harmless fallback marker - ProtoStream does
 * NOT use Java serialization, this does not add a second marshalling path.
 */
public class DeviceHistoryEntry implements Serializable {

    private final String deviceHash;   // sha-256 of normalized User-Agent (+ optional device cookie)
    private final String ipAddress;    // exact IP as seen at login time
    private final String ipSubnet;     // /24 (v4) or /48 (v6) generalization, used for "same network" matching
    private long firstSeen;            // epoch millis
    private long lastSeen;             // epoch millis

    @ProtoFactory
    public DeviceHistoryEntry(String deviceHash, String ipAddress, String ipSubnet, long firstSeen, long lastSeen) {
        this.deviceHash = deviceHash;
        this.ipAddress = ipAddress;
        this.ipSubnet = ipSubnet;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }

    @ProtoField(number = 1)
    public String getDeviceHash() {
        return deviceHash;
    }

    @ProtoField(number = 2)
    public String getIpAddress() {
        return ipAddress;
    }

    @ProtoField(number = 3)
    public String getIpSubnet() {
        return ipSubnet;
    }

    @ProtoField(number = 4, defaultValue = "0")
    public long getFirstSeen() {
        return firstSeen;
    }

    @ProtoField(number = 5, defaultValue = "0")
    public long getLastSeen() {
        return lastSeen;
    }

    /**
     * Returns a copy with lastSeen bumped to `now`. DeviceHistoryEntry stays effectively
     * immutable at the Protostream boundary - InfinispanUntrustedLoginProvider rebuilds
     * the containing UserDeviceHistory rather than mutating entries in place, which is the
     * safer pattern for replicated/distributed Infinispan caches (see provider comments).
     */
    public DeviceHistoryEntry withLastSeen(long now) {
        return new DeviceHistoryEntry(deviceHash, ipAddress, ipSubnet, firstSeen, now);
    }

    public boolean isExpired(long now, long trustWindowMillis) {
        return (now - lastSeen) > trustWindowMillis;
    }

    /** Matches if same device fingerprint, regardless of IP (device roamed to a new network). */
    public boolean matchesDevice(String otherDeviceHash) {
        return Objects.equals(this.deviceHash, otherDeviceHash);
    }

    /** Matches if same IP subnet, regardless of device (new browser, same trusted network). */
    public boolean matchesSubnet(String otherSubnet) {
        return otherSubnet != null && Objects.equals(this.ipSubnet, otherSubnet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceHistoryEntry)) return false;
        DeviceHistoryEntry that = (DeviceHistoryEntry) o;
        return Objects.equals(deviceHash, that.deviceHash) && Objects.equals(ipAddress, that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceHash, ipAddress);
    }
}