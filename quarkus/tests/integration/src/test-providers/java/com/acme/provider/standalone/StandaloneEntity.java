package com.acme.provider.standalone;

import jakarta.persistence.Id;

public class StandaloneEntity {
    @jakarta.persistence.Id
    private String id;
    private String name;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
