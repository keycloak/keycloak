package org.keycloak.admin.ui.rest.model;

import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public final class ClientRole {
    @Schema(required = true)
    private final String id;
    @Schema(required = true)
    private final String role;
    @Schema(required = true)
    private String client;
    @Schema(required = true)
    private String clientId;
    private String description;

    public String getId() {
        return this.id;
    }

    public String getRole() {
        return this.role;
    }

    public String getClient() {
        return this.client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ClientRole(String id, String role, String description) {
        this.id = id;
        this.role = role;
        this.description = description;
    }

    public ClientRole(String id, String role, String client, String clientId, String description) {
        this.id = id;
        this.role = role;
        this.client = client;
        this.clientId = clientId;
        this.description = description;
    }

    public ClientRole copy(String id, String role, String client, String clientId, String description) {
        return new ClientRole(id, role, client, clientId, description);
    }

    @Override public String toString() {
        return "ClientRole{" + "id='" + id + '\'' + ", role='" + role + '\'' + ", client='" + client + '\'' + ", clientId='" + clientId + '\'' + ", description='" + description + '\'' + '}';
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClientRole that = (ClientRole) o;
        return id.equals(that.id) && role.equals(that.role) && client.equals(that.client) && clientId.equals(that.clientId);
    }

    @Override public int hashCode() {
        return Objects.hash(id, role, client, clientId);
    }
}
