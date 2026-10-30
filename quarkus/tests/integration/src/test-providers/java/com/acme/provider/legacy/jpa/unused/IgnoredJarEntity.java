package com.acme.provider.legacy.jpa.unused;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class IgnoredJarEntity {
    @Id
    private String id;
}
