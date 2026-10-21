package com.acme.provider.singlenamed;

import jakarta.persistence.Id;

public class SingleNamedPuEntity {
    @Id
    private String id;
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
