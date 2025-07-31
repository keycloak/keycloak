package org.keycloak.models.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "RLM_ACTION")
public class ResourceActionEntity {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "PROVIDER_ID")
    private String providerId;

    @Column(name = "PRIORITY")
    private int priority;

    @Column(name = "AFTER_MS")
    private long afterMs; // Duration in milliseconds

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POLICY_ID")
    private ResourcePolicyEntity policy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getAfterMs() {
        return afterMs;
    }

    public void setAfterMs(long afterMs) {
        this.afterMs = afterMs;
    }

    public ResourcePolicyEntity getPolicy() {
        return policy;
    }

    public void setPolicy(ResourcePolicyEntity policy) {
        this.policy = policy;
    }
}
