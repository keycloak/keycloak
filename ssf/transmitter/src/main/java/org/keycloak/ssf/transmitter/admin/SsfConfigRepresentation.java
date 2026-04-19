package org.keycloak.ssf.transmitter.admin;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representation of the SSF configuration exposed via the admin endpoint
 * {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/config}.
 *
 * <p>Currently this only carries the set of SSF event types supported by the
 * transmitter by default. Additional realm/transmitter-level SSF settings can
 * be added here as the SSF feature evolves without having to introduce
 * separate endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SsfConfigRepresentation {

    private Set<String> defaultSupportedEvents;

    private Set<String> availableSupportedEvents;

    /**
     * Subset of {@link #availableSupportedEvents} that the transmitter
     * fires natively from Keycloak event listeners. Used by the admin
     * UI as a "natively emitted" badge — events outside this set are
     * still selectable but only fire when an external system uses the
     * synthetic emit endpoint or a custom mapper is shipped.
     */
    private Set<String> nativelyEmittedEvents;

    private Integer defaultPushEndpointConnectTimeoutMillis;

    private Integer defaultPushEndpointSocketTimeoutMillis;

    private String defaultUserSubjectFormat;

    public Set<String> getDefaultSupportedEvents() {
        return defaultSupportedEvents;
    }

    public void setDefaultSupportedEvents(Set<String> defaultSupportedEvents) {
        this.defaultSupportedEvents = defaultSupportedEvents;
    }

    public Set<String> getAvailableSupportedEvents() {
        return availableSupportedEvents;
    }

    public void setAvailableSupportedEvents(Set<String> availableSupportedEvents) {
        this.availableSupportedEvents = availableSupportedEvents;
    }

    public Set<String> getNativelyEmittedEvents() {
        return nativelyEmittedEvents;
    }

    public void setNativelyEmittedEvents(Set<String> nativelyEmittedEvents) {
        this.nativelyEmittedEvents = nativelyEmittedEvents;
    }

    public Integer getDefaultPushEndpointConnectTimeoutMillis() {
        return defaultPushEndpointConnectTimeoutMillis;
    }

    public void setDefaultPushEndpointConnectTimeoutMillis(Integer defaultPushEndpointConnectTimeoutMillis) {
        this.defaultPushEndpointConnectTimeoutMillis = defaultPushEndpointConnectTimeoutMillis;
    }

    public Integer getDefaultPushEndpointSocketTimeoutMillis() {
        return defaultPushEndpointSocketTimeoutMillis;
    }

    public void setDefaultPushEndpointSocketTimeoutMillis(Integer defaultPushEndpointSocketTimeoutMillis) {
        this.defaultPushEndpointSocketTimeoutMillis = defaultPushEndpointSocketTimeoutMillis;
    }

    public String getDefaultUserSubjectFormat() {
        return defaultUserSubjectFormat;
    }

    public void setDefaultUserSubjectFormat(String defaultUserSubjectFormat) {
        this.defaultUserSubjectFormat = defaultUserSubjectFormat;
    }
}
