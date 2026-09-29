package com.acme.provider.legacy.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class UnlistedEntity {
    @Id
    private String id;
}
