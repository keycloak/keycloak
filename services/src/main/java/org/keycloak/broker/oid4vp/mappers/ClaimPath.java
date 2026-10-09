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

package org.keycloak.broker.oid4vp.mappers;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A path locating claims in the claims JSON of a presented credential, configured in dot notation
 * with optional array selectors: {@code address.locality} selects a nested claim,
 * {@code nationalities[]} selects all array elements, {@code nationalities[0]} selects the first
 * element, and a literal dot in a claim name is escaped as {@code \.}.
 *
 * <p>Field and all element steps correspond to the object key (string) and all elements (null)
 * kinds of a DCQL claims path pointer as defined by
 * <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.3">
 * OID4VP 1.0, Section 6.3</a>. The pointer's array index kind is not supported beyond {@code [0]}:
 * the verifier removes undisclosed array elements from the presented payload, so positions from
 * the issued credential do not survive selective disclosure, and an index read against the
 * compacted array would silently select a different element than the one the position named at
 * issuance. {@code [0]} therefore means the first element of the presentation, which under
 * selective disclosure is the first disclosed element and not necessarily the first issued one.
 */
public class ClaimPath {

    /**
     * One path step. The field name is set for {@link Kind#FIELD} steps only.
     */
    public record Step(String field, Kind kind) {

        public enum Kind {
            FIELD,
            ALL_ELEMENTS,
            FIRST_ELEMENT
        }
    }

    private final List<Step> steps;

    protected ClaimPath(List<Step> steps) {
        this.steps = List.copyOf(steps);
    }

    /**
     * The parsed steps, for consumers that transform the path rather than resolve it.
     */
    public List<Step> steps() {
        return steps;
    }

    /**
     * Parses the dot notation path, returning {@code null} if it is not well formed.
     */
    public static ClaimPath parse(String path) {
        // The splitting below would silently drop a trailing separator, so a path ending in a
        // dot is always malformed, even an escaped one.
        if (path == null || path.endsWith(".")) {
            return null;
        }
        List<Step> steps = new ArrayList<>();
        for (String segment : JsonUtils.splitClaimPath(path)) {
            int bracket = segment.indexOf('[');
            String field = bracket < 0 ? segment : segment.substring(0, bracket);
            if (field.isEmpty()) {
                return null;
            }
            steps.add(new Step(field, Step.Kind.FIELD));
            String selectors = bracket < 0 ? "" : segment.substring(bracket);
            while (!selectors.isEmpty()) {
                if (selectors.startsWith("[]")) {
                    steps.add(new Step(null, Step.Kind.ALL_ELEMENTS));
                    selectors = selectors.substring(2);
                } else if (selectors.startsWith("[0]")) {
                    steps.add(new Step(null, Step.Kind.FIRST_ELEMENT));
                    selectors = selectors.substring(3);
                } else {
                    return null;
                }
            }
        }
        return steps.isEmpty() ? null : new ClaimPath(steps);
    }

    /**
     * Selects the claims this path points to. An all elements step fans out into every array
     * element, so the result may hold several nodes. Null nodes and dead ends select nothing.
     */
    public List<JsonNode> select(JsonNode claims) {
        List<JsonNode> current = new ArrayList<>();
        if (claims != null) {
            current.add(claims);
        }
        for (Step step : steps) {
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode node : current) {
                switch (step.kind()) {
                    case FIELD -> {
                        if (node.isObject() && node.get(step.field()) != null) {
                            next.add(node.get(step.field()));
                        }
                    }
                    case ALL_ELEMENTS -> {
                        if (node.isArray()) {
                            node.forEach(next::add);
                        }
                    }
                    case FIRST_ELEMENT -> {
                        if (node.isArray() && !node.isEmpty()) {
                            next.add(node.get(0));
                        }
                    }
                }
            }
            next.removeIf(JsonNode::isNull);
            current = next;
        }
        return current;
    }
}
