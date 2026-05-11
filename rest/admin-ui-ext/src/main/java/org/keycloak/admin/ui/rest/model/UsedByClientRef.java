package org.keycloak.admin.ui.rest.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

public class UsedByClientRef {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;
    private String clientId;

    public UsedByClientRef() {
    }

    public UsedByClientRef(String id, String clientId) {
        this.id = id;
        this.clientId = clientId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UsedByClientRef that = (UsedByClientRef) o;
        return Objects.equals(id, that.id) && Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clientId);
    }
}
