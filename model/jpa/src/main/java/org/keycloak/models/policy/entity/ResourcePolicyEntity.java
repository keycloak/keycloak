package org.keycloak.models.policy.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RLM_POLICY")
public class ResourcePolicyEntity {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "REALM_ID")
    private String realmId;

    @Column(name = "PROVIDER_ID")
    private String providerId;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("priority ASC")
    private List<ResourceActionEntity> actions = new ArrayList<>();

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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public List<ResourceActionEntity> getActions() {
        return actions;
    }

    public void setActions(List<ResourceActionEntity> actions) {
        this.actions = actions;
    }
}
