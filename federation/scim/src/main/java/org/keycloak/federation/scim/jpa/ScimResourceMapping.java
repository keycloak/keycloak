package org.keycloak.federation.scim.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.keycloak.federation.scim.core.service.EntityOnRemoteScimId;
import org.keycloak.federation.scim.core.service.KeycloakId;

@Entity
@IdClass(ScimResourceId.class)
@Table(name = "SCIM_RESOURCE_MAPPING")
@NamedQueries({
        @NamedQuery(name = "findById", query = "from ScimResourceMapping where realmId = :realmId and componentId = :componentId and type = :type and id = :id"),
        @NamedQuery(name = "findByExternalId", query = "from ScimResourceMapping where realmId = :realmId and componentId = :componentId and type = :type and externalId = :id") })
public class ScimResourceMapping {

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @Id
    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Id
    @Column(name = "COMPONENT_ID", nullable = false)
    private String componentId;

    @Id
    @Column(name = "TYPE", nullable = false)
    private String type;

    @Id
    @Column(name = "EXTERNAL_ID", nullable = false)
    private String externalId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public KeycloakId getIdAsKeycloakId() {
        return new KeycloakId(id);
    }

    public EntityOnRemoteScimId getExternalIdAsEntityOnRemoteScimId() {
        return new EntityOnRemoteScimId(externalId);
    }
}
