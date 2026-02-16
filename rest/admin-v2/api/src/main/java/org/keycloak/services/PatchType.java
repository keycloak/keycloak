package org.keycloak.services;

import java.util.Optional;

import jakarta.ws.rs.core.MediaType;

public enum PatchType {
    JSON_MERGE(MediaType.valueOf(MediaName.JSON_MERGE));

    private final MediaType mediaType;

    PatchType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public static class MediaName {
        public static final String JSON_MERGE = "application/merge-patch+json";
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
