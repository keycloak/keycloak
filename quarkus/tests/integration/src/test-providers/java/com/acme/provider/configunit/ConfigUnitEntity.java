package com.acme.provider.configunit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.keycloak.provider.Provider;

/**
 * Entity of a persistence unit defined through {@code db-jpa-packages-<datasource>} instead of a persistence.xml.
 * Implements an interface of keycloak-server-spi to assert that the class hierarchy is assigned to the unit as well.
 */
@Entity
@Table(name = "CONFIG_UNIT_ENTITY")
public class ConfigUnitEntity implements Provider {

    @Id
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void close() {
    }
}
