package org.keycloak.audit;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Event {

    private long time;

    private String event;

    private String realmId;

    private String clientId;

    private String userId;

    private String ipAddress;

    private String error;

    private Map<String, String> details;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Event clone() {
        Event clone = new Event();
        clone.time = time;
        clone.event = event;
        clone.realmId = realmId;
        clone.clientId = clientId;
        clone.userId = userId;
        clone.ipAddress = ipAddress;
        clone.error = error;
        clone.details = details != null ? new HashMap<String, String>(details) : null;
        return clone;
    }

}
