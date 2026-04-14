package org.keycloak.ssf.endpoint.admin;

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
