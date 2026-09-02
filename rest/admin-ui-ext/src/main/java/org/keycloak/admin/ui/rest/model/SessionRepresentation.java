package org.keycloak.admin.ui.rest.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.keycloak.admin.ui.rest.model.ClientIdSessionType.SessionType;

public class SessionRepresentation {
    private String id;
    private String username;
    private String userId;
    private String ipAddress;
    private long start;
    private long lastAccess;
    private boolean transientUser;

    private SessionType type;
    private Map<String, String> clients = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public SessionType getType() {
        return type;
    }

    public void setType(SessionType type) {
        this.type = type;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }

    public Map<String, String> getClients() {
        return clients;
    }

    public void setClients(Map<String, String> clients) {
        this.clients = clients;
    }

    public boolean isTransientUser() {
        return transientUser;
    }

    public void setTransientUser(boolean transientUser) {
        this.transientUser = transientUser;
    }

    /**
     * Equality is determined by user session ID and the offline flag, which are also the primary key on this entity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionRepresentation)) return false;
        SessionRepresentation that = (SessionRepresentation) o;
        return Objects.equals(id, that.id) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
