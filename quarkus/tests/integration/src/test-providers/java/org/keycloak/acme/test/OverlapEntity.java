package org.keycloak.acme.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OverlapEntity {
    @Id
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
