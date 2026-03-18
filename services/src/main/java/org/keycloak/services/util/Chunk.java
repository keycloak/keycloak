package org.keycloak.services.util;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a chunk from the Vite build manifest (see {@link ViteManifest}).
 */
public record Chunk (
    @JsonProperty(required = true)
    String file,

    @JsonProperty
    Optional<String> src,

    @JsonProperty
    Optional<String> name,

    @JsonProperty
    Optional<Boolean> isEntry,

    @JsonProperty
    Optional<Boolean> isDynamicEntry,

    @JsonProperty
    Optional<String[]> imports,

    @JsonProperty
    Optional<String[]> dynamicImports,

    @JsonProperty
    Optional<String[]> assets,

    @JsonProperty Optional<String[]> css
){}
