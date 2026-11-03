package com.acme.provider.tenantunit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity of a multi-tenant persistence unit defined through {@code db-jpa-packages-<datasource>} and
 * {@code db-orm-multitenant-<datasource>}.
 */
@Entity
@Table(name = "TENANT_UNIT_ENTITY")
public class TenantUnitEntity {

    @Id
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
