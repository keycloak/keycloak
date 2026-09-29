package com.acme.provider.standalone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class StandaloneEntity2 {
    @Id
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
