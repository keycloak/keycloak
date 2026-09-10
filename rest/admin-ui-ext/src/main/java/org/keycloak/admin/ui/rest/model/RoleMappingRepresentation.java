package org.keycloak.admin.ui.rest.model;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public final class RoleMappingRepresentation {
    @Schema(description = "Realm role mappings")
    private List<RoleRepresentation> realmMappings;

    @Schema(description = "Client role mappings keyed by client ID")
    private Map<String, ClientMappingRepresentation> clientMappings;

    public RoleMappingRepresentation() {
    }

    public RoleMappingRepresentation(List<RoleRepresentation> realmMappings, Map<String, ClientMappingRepresentation> clientMappings) {
        this.realmMappings = realmMappings;
        this.clientMappings = clientMappings;
    }

    public List<RoleRepresentation> getRealmMappings() {
        return realmMappings;
    }

    public void setRealmMappings(List<RoleRepresentation> realmMappings) {
        this.realmMappings = realmMappings;
    }

    public Map<String, ClientMappingRepresentation> getClientMappings() {
        return clientMappings;
    }

    public void setClientMappings(Map<String, ClientMappingRepresentation> clientMappings) {
        this.clientMappings = clientMappings;
    }

    public static class RoleRepresentation {
        private String id;
        private String name;
        private String description;
        private boolean composite;
        private boolean clientRole;
        private String containerId;

        public RoleRepresentation() {
        }

        public RoleRepresentation(String id, String name, String description, boolean composite, boolean clientRole, String containerId) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.composite = composite;
            this.clientRole = clientRole;
            this.containerId = containerId;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isComposite() {
            return composite;
        }

        public void setComposite(boolean composite) {
            this.composite = composite;
        }

        public boolean isClientRole() {
            return clientRole;
        }

        public void setClientRole(boolean clientRole) {
            this.clientRole = clientRole;
        }

        public String getContainerId() {
            return containerId;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }
    }

    public static class ClientMappingRepresentation {
        private String id;
        private String client;
        private List<RoleRepresentation> mappings;

        public ClientMappingRepresentation() {
        }

        public ClientMappingRepresentation(String id, String client, List<RoleRepresentation> mappings) {
            this.id = id;
            this.client = client;
            this.mappings = mappings;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClient() {
            return client;
        }

        public void setClient(String client) {
            this.client = client;
        }

        public List<RoleRepresentation> getMappings() {
            return mappings;
        }

        public void setMappings(List<RoleRepresentation> mappings) {
            this.mappings = mappings;
        }
    }
}
