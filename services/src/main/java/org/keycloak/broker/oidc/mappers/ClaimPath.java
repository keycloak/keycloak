/*
 * Copyright 2026 Andrzej Świętek
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

package org.keycloak.broker.oidc.mappers;

import java.util.List;
import java.util.Optional;

import org.keycloak.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A claim path read as {@link JsonUtils#getJsonValue(JsonNode, String)} reads it: components separated by {@code .}, a
 * literal dot escaped as {@code \.}, and a component ending with an array index such as {@code [0]}; the walk stops at
 * the first array or scalar value on the path. Unlike that method, the value found is returned as sent, without
 * trimming or converting it.
 */
final class ClaimPath {

    /**
     * @param index the array element to select when the named value is an array
     */
    private record Step(String name, Optional<Integer> index) {

        /**
         * The step for {@code component}; empty when it ends with {@code ]} without a valid index.
         */
        static Optional<Step> parse(String component) {
            return component.endsWith("]") ? indexedStep(component) : Optional.of(new Step(component, Optional.empty()));
        }

        private static Optional<Step> indexedStep(String component) {
            int bracket = component.indexOf('[');
            return bracket < 0
                    ? Optional.empty()
                    : arrayIndex(component.substring(bracket + 1, component.length() - 1).trim())
                            .map(index -> new Step(component.substring(0, bracket).trim(), Optional.of(index)));
        }

        private static Optional<Integer> arrayIndex(String text) {
            try {
                return Optional.of(Integer.parseInt(text)).filter(index -> index >= 0);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        Optional<JsonNode> select(JsonNode node) {
            return present(node.get(name)).flatMap(this::element);
        }

        private Optional<JsonNode> element(JsonNode named) {
            return named.isArray() && index.isPresent() ? present(named.get(index.orElseThrow())) : Optional.of(named);
        }
    }

    private ClaimPath() {
    }

    /**
     * The node at {@code claim} under {@code root}; empty when the path is invalid or leads to nothing or to JSON null.
     */
    static Optional<JsonNode> find(JsonNode root, String claim) {
        List<String> components = JsonUtils.splitClaimPath(claim);
        return components.isEmpty() || claim.endsWith(".") ? Optional.empty() : walk(root, components);
    }

    /**
     * Each component is parsed when the walk reaches it, so an invalid component after the end of the walk is ignored.
     */
    private static Optional<JsonNode> walk(JsonNode node, List<String> components) {
        return components.isEmpty()
                ? Optional.of(node)
                : Step.parse(components.get(0))
                        .flatMap(step -> step.select(node))
                        .flatMap(selected -> continueFrom(selected, components.subList(1, components.size())));
    }

    /**
     * Continues the walk below an object; an array or a scalar value ends it.
     */
    private static Optional<JsonNode> continueFrom(JsonNode node, List<String> remainingComponents) {
        return node.isObject() ? walk(node, remainingComponents) : Optional.of(node);
    }

    private static Optional<JsonNode> present(JsonNode node) {
        return Optional.ofNullable(node).filter(candidate -> !candidate.isNull() && !candidate.isMissingNode());
    }
}
