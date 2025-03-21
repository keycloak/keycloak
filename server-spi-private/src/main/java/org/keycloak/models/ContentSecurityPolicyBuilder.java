/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

import java.util.LinkedHashMap;
import java.util.Map;

public class ContentSecurityPolicyBuilder {

    // constants for directive names used in the class
    public static final String DIRECTIVE_NAME_FRAME_SRC = "frame-src";
    public static final String DIRECTIVE_NAME_FRAME_ANCESTORS = "frame-ancestors";
    public static final String DIRECTIVE_NAME_OBJECT_SRC = "object-src";

    // constants for specific directive value keywords
    public static final String DIRECTIVE_VALUE_SELF = "'self'";
    public static final String DIRECTIVE_VALUE_NONE = "'none'";

    private final Map<String, String> directives = new LinkedHashMap<>();

    public static ContentSecurityPolicyBuilder create() {
        return new ContentSecurityPolicyBuilder()
                .add(DIRECTIVE_NAME_FRAME_SRC, DIRECTIVE_VALUE_SELF)
                .add(DIRECTIVE_NAME_FRAME_ANCESTORS, DIRECTIVE_VALUE_SELF)
                .add(DIRECTIVE_NAME_OBJECT_SRC, DIRECTIVE_VALUE_NONE);
    }

    public static ContentSecurityPolicyBuilder create(String directives) {
        return new ContentSecurityPolicyBuilder().parse(directives);
    }

    public ContentSecurityPolicyBuilder frameSrc(String frameSrc) {
        if (frameSrc == null) {
            directives.remove(DIRECTIVE_NAME_FRAME_SRC);
        } else {
            put(DIRECTIVE_NAME_FRAME_SRC, frameSrc);
        }
        return this;
    }

    public ContentSecurityPolicyBuilder addFrameSrc(String frameSrc) {
        return add(DIRECTIVE_NAME_FRAME_SRC, frameSrc);
    }

    public boolean isDefaultFrameAncestors() {
        return DIRECTIVE_VALUE_SELF.equals(directives.get(DIRECTIVE_NAME_FRAME_ANCESTORS));
    }

    public ContentSecurityPolicyBuilder frameAncestors(String frameancestors) {
        if (frameancestors == null) {
            directives.remove(DIRECTIVE_NAME_FRAME_ANCESTORS);
        } else {
            put(DIRECTIVE_NAME_FRAME_ANCESTORS, frameancestors);
        }
        return this;
    }

    public ContentSecurityPolicyBuilder addFrameAncestors(String frameancestors) {
        return add(DIRECTIVE_NAME_FRAME_ANCESTORS, frameancestors);
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        if (!directives.isEmpty()) {
            for (Map.Entry<String, String> entry : directives.entrySet()) {
                sb.append(entry.getKey());
                if (!entry.getValue().isEmpty()) {
                    sb.append(" ").append(entry.getValue());
                }
                sb.append("; ");
            }
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private ContentSecurityPolicyBuilder put(String name, String value) {
        if (name != null && value != null) {
            directives.put(name, value);
        }
        return this;
    }

    private ContentSecurityPolicyBuilder add(String name, String value) {
        if (name != null && value != null) {
            String current = directives.get(name);
            if (current != null && !current.isEmpty()) {
                value = current + " " + value;
            }
            directives.put(name, value);
        }
        return this;
    }

    // W3C Working Draft: https://www.w3.org/TR/CSP/
    // Only managing spaces not the other whitespaces defined in the spec
    private ContentSecurityPolicyBuilder parse(String value) {
        if (value == null) {
            return this;
        }
        String[] values = value.split(";");
        if (values != null) {
            for (String directive : values) {
                directive = directive.trim();
                int idx = directive.indexOf(' ');
                if (idx > 0) {
                    add(directive.substring(0, idx), directive.substring(idx + 1, directive.length()).trim());
                } else if (!directive.isEmpty()) {
                    add(directive, "");
                }
            }
        }
        return this;
    }
}
