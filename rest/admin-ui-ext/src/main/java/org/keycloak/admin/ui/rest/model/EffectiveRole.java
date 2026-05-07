package org.keycloak.admin.ui.rest.model;

import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public final class EffectiveRole {
    @Schema(required = true)
    private final String id;
    @Schema(required = true)
    private final String name;
    private final String description;
    @Schema(required = true)
    private final boolean clientRole;
    private String client;
    private String clientId;

    public EffectiveRole(String id, String name, String description, boolean clientRole) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.clientRole = clientRole;
    }

    public EffectiveRole(String id, String name, String description, boolean clientRole, String client, String clientId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.clientRole = clientRole;
        this.client = client;
        this.clientId = clientId;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isClientRole() {
        return this.clientRole;
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

    @Override
    public String toString() {
        return "EffectiveRole{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", clientRole=" + clientRole +
                ", client='" + client + '\'' +
                ", clientId='" + clientId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EffectiveRole that = (EffectiveRole) o;
        return clientRole == that.clientRole &&
                id.equals(that.id) &&
                name.equals(that.name) &&
                Objects.equals(client, that.client) &&
                Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, clientRole, client, clientId);
    }
}
