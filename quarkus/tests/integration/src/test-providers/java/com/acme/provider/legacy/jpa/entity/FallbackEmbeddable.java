package com.acme.provider.legacy.jpa.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class FallbackEmbeddable {

    private String value;

    public String getValue() {
        return value;
    }
}
