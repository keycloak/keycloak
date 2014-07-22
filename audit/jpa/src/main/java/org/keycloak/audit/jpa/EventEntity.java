package org.keycloak.audit.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Entity
@Table(name="EVENT_ENTITY")
public class EventEntity {

    @Id
    @Column(name="ID", length = 36)
    private String id;

    @Column(name="TIME")
    private long time;

    @Column(name="EVENT")
    private String event;

    @Column(name="REALM_ID")
    private String realmId;

    @Column(name="CLIENT_ID")
    private String clientId;

    @Column(name="USER_ID")
    private String userId;

    @Column(name="SESSION_ID")
    private String sessionId;

    @Column(name="IP_ADDRESS")
    private String ipAddress;

    @Column(name="ERROR")
    private String error;

    @Column(name="DETAILS_JSON", length = 2550)
    private String detailsJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

}
