package com.acme.provider.multitenancy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "MT_DISCRIMINATOR_ENTITY")
public class DiscriminatorTenantEntity {

    @Id
    private String id;

    @TenantId
    private String tenant;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenant() {
        return tenant;
    }
}
