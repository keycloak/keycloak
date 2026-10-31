package com.acme.provider.legacy.jpa.entity;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Mapped superclass of {@link FallbackEntity}: it must follow the entity into the default persistence unit.
 */
@MappedSuperclass
public abstract class FallbackBase {

    @Id
    private String id;

    public String getId() {
        return id;
    }
}
