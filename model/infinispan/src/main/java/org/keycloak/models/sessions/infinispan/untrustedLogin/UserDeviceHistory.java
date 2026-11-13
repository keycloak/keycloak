package org.keycloak.models.sessions.infinispan.untrustedlogin;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cache VALUE type for the "knownUserDevices" Infinispan cache - one instance per
 * (realm, user) cache key, holding all remembered devices/IPs for that user.
 *
 * This exists as a separate class (rather than storing Set<DeviceHistoryEntry> directly)
 * because Protostream marshals concrete message types, not raw Java collections - a
 * @ProtoField of List<T> works (Protostream treats it as a repeated field), but the
 * collection itself needs to be a field of some annotated class.
 */
public class UserDeviceHistory implements Serializable {

    private final List<DeviceHistoryEntry> entries;

    @ProtoFactory
    public UserDeviceHistory(List<DeviceHistoryEntry> entries) {
        this.entries = entries != null ? entries : Collections.emptyList();
    }

    @ProtoField(number = 1)
    public List<DeviceHistoryEntry> getEntries() {
        return entries;
    }

    public static UserDeviceHistory empty() {
        return new UserDeviceHistory(new ArrayList<>());
    }
}