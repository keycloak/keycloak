package org.keycloak.services;

import java.util.Optional;

import jakarta.ws.rs.core.MediaType;

public enum PatchType {
    JSON_MERGE(PatchTypeNames.JSON_MERGE);

    private final MediaType mediaType;

    PatchType(String mediaType) {
        this.mediaType = MediaType.valueOf(mediaType);
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public static Optional<PatchType> getByMediaType(String mediaType) {
        try {
            return getByMediaType(MediaType.valueOf(mediaType));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<PatchType> getByMediaType(MediaType mediaType) {
        for (var type : PatchType.values()) {
            if (type.getMediaType().isCompatible(mediaType)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
