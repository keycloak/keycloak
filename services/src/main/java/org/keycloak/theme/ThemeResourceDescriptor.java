/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.theme;

public class ThemeResourceDescriptor {

    private final String path;
    private final String media;
    private final String integrity;
    private final String crossorigin;
    private final String defer;
    private final String async;
    private final String type;
    private final String blocking;
    private final String rel;

    static String normalizeFaviconPath(String path) {
        if (path != null && path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    private ThemeResourceDescriptor(Builder builder, boolean favicon) {
        this.path = favicon ? normalizeFaviconPath(builder.path) : builder.path;
        this.media = builder.media;
        this.integrity = builder.integrity;
        this.crossorigin = builder.crossorigin;
        this.defer = builder.defer;
        this.async = builder.async;
        this.type = builder.type;
        this.blocking = builder.blocking;
        this.rel = builder.rel;
    }

    public static Builder builder(String path) {
        return new Builder(path);
    }

    public String getPath() {
        return path;
    }

    public String getMedia() {
        return media;
    }

    public String getIntegrity() {
        return integrity;
    }

    public String getCrossorigin() {
        return crossorigin;
    }

    public String getDefer() {
        return defer;
    }

    public String getAsync() {
        return async;
    }

    public String getType() {
        return type;
    }

    public String getBlocking() {
        return blocking;
    }

    public String getRel() {
        return rel;
    }

    public boolean hasMedia() {
        return media != null && !media.isEmpty();
    }

    public boolean hasIntegrity() {
        return integrity != null && !integrity.isEmpty();
    }

    public boolean hasCrossorigin() {
        return crossorigin != null && !crossorigin.isEmpty();
    }

    public boolean hasDefer() {
        return defer != null && isTruthy(defer, "defer");
    }

    public boolean hasAsync() {
        return async != null && isTruthy(async, "async");
    }

    public boolean hasType() {
        return type != null && !type.isEmpty();
    }

    public boolean hasBlocking() {
        return blocking != null && !blocking.isEmpty();
    }

    public boolean hasRel() {
        return rel != null && !rel.isEmpty();
    }

    private static boolean isTruthy(String value, String keyword) {
        return "true".equalsIgnoreCase(value) || keyword.equalsIgnoreCase(value);
    }

    static String inferFaviconType(String path) {
        if (path == null) {
            return null;
        }
        String lower = path.toLowerCase();
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }
        return null;
    }

    public static final class Builder {
        private final String path;
        private String media;
        private String integrity;
        private String crossorigin;
        private String defer;
        private String async;
        private String type;
        private String blocking;
        private String rel;

        private Builder(String path) {
            this.path = path;
        }

        public Builder media(String media) {
            this.media = media;
            return this;
        }

        public Builder integrity(String integrity) {
            this.integrity = integrity;
            return this;
        }

        public Builder crossorigin(String crossorigin) {
            this.crossorigin = crossorigin;
            return this;
        }

        public Builder defer(String defer) {
            this.defer = defer;
            return this;
        }

        public Builder async(String async) {
            this.async = async;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder blocking(String blocking) {
            this.blocking = blocking;
            return this;
        }

        public Builder rel(String rel) {
            this.rel = rel;
            return this;
        }

        public Builder attribute(String name, String value) {
            if (value == null) {
                return this;
            }
            switch (name) {
                case "media" -> media = value;
                case "integrity" -> integrity = value;
                case "crossorigin" -> crossorigin = value;
                case "defer" -> defer = value;
                case "async" -> async = value;
                case "type" -> type = value;
                case "blocking" -> blocking = value;
                case "rel" -> rel = value;
                default -> { }
            }
            return this;
        }

        public ThemeResourceDescriptor build() {
            return new ThemeResourceDescriptor(this, false);
        }

        public ThemeResourceDescriptor buildFavicon() {
            String normalizedPath = normalizeFaviconPath(path);
            if (type == null || type.isEmpty()) {
                type = inferFaviconType(normalizedPath);
            }
            if (rel == null || rel.isEmpty()) {
                rel = "icon";
            }
            return new ThemeResourceDescriptor(this, true);
        }
    }
}
