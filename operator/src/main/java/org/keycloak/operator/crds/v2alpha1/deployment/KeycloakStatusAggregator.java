/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.crds.v2alpha1.deployment;

import org.keycloak.operator.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KeycloakStatusAggregator {
    private final KeycloakStatusCondition readyCondition = new KeycloakStatusCondition();
    private final KeycloakStatusCondition hasErrorsCondition = new KeycloakStatusCondition();
    private final KeycloakStatusCondition rollingUpdate = new KeycloakStatusCondition();
    private final KeycloakStatusCondition updateType = new KeycloakStatusCondition();

    private final List<String> notReadyMessages = new ArrayList<>();
    private final List<String> errorMessages = new ArrayList<>();
    private final List<String> rollingUpdateMessages = new ArrayList<>();

    private final KeycloakStatusBuilder statusBuilder;
    private final Map<String, KeycloakStatusCondition> existingConditions;
    private final Long observedGeneration;

    /**
     * @param generation the observedGeneration for conditions
     */
    public KeycloakStatusAggregator(Long generation) {
        this(null, generation);
    }

    /**
     * @param current status, which is used as a base for the next conditions
     * @param generation the observedGeneration for conditions
     */
    public KeycloakStatusAggregator(KeycloakStatus current, Long generation) {
        if (current != null) { // 6.7 fabric8 no longer requires this null check
            statusBuilder = new KeycloakStatusBuilder(current);
            existingConditions = Optional.ofNullable(current.getConditions()).orElse(List.of()).stream().collect(Collectors.toMap(KeycloakStatusCondition::getType, Function.identity()));
        } else {
            statusBuilder = new KeycloakStatusBuilder();
            existingConditions = Map.of();
        }

        observedGeneration = generation;

        readyCondition.setType(KeycloakStatusCondition.READY);

        hasErrorsCondition.setType(KeycloakStatusCondition.HAS_ERRORS);

        rollingUpdate.setType(KeycloakStatusCondition.ROLLING_UPDATE);

        updateType.setType(KeycloakStatusCondition.UPDATE_TYPE);
    }

    public KeycloakStatusAggregator addNotReadyMessage(String message) {
        readyCondition.setStatus(false);
        readyCondition.setObservedGeneration(observedGeneration);
        notReadyMessages.add(message);
        return this;
    }

    public KeycloakStatusAggregator addErrorMessage(String message) {
        hasErrorsCondition.setStatus(true);
        hasErrorsCondition.setObservedGeneration(observedGeneration);
        errorMessages.add(message);
        return this;
    }

    public KeycloakStatusAggregator addWarningMessage(String message) {
        errorMessages.add("warning: " + message);
        hasErrorsCondition.setObservedGeneration(observedGeneration);
        return this;
    }

    public KeycloakStatusAggregator addRollingUpdateMessage(String message) {
        rollingUpdate.setStatus(true);
        rollingUpdate.setObservedGeneration(observedGeneration);
        rollingUpdateMessages.add(message);
        return this;
    }

    public void addUpdateType(boolean recreate, String message) {
        updateType.setStatus(recreate);
        updateType.setObservedGeneration(observedGeneration);
        updateType.setMessage(message);
    }

    public void resetUpdateType() {
        updateType.setStatus(null);
        updateType.setObservedGeneration(observedGeneration);
        updateType.setMessage(null);
    }

    /**
     * Apply non-condition changes to the status
     */
    public KeycloakStatusAggregator apply(Consumer<KeycloakStatusBuilder> toApply) {
        statusBuilder.withConditions(List.of());
        toApply.accept(statusBuilder);
        if (!statusBuilder.getConditions().isEmpty()) {
            throw new AssertionError("use addXXXMessage methods to modify conditions");
        }
        return this;
    }

    public KeycloakStatus build() {
        // conditions are only updated in one direction - the following determines if it's appropriate to observe the default / other direction
        if (readyCondition.getStatus() == null && !Boolean.TRUE.equals(hasErrorsCondition.getStatus())) {
            readyCondition.setStatus(true);
            readyCondition.setObservedGeneration(observedGeneration);
        }
        if (readyCondition.getObservedGeneration() != null) {
            readyCondition.setMessage(String.join("\n", notReadyMessages));
        }

        if (hasErrorsCondition.getStatus() == null && readyCondition.getObservedGeneration() != null) {
            hasErrorsCondition.setStatus(false);
            hasErrorsCondition.setObservedGeneration(observedGeneration);
        }
        if (hasErrorsCondition.getObservedGeneration() != null) {
            hasErrorsCondition.setMessage(String.join("\n", errorMessages));
        }

        if (rollingUpdate.getStatus() == null && readyCondition.getObservedGeneration() != null) {
            rollingUpdate.setStatus(false);
            rollingUpdate.setObservedGeneration(observedGeneration);
        }
        if (rollingUpdate.getObservedGeneration() != null) {
            rollingUpdate.setMessage(String.join("\n", rollingUpdateMessages));
        }

        String now = Utils.iso8601Now();
        updateConditionFromExisting(readyCondition, existingConditions, now);
        updateConditionFromExisting(hasErrorsCondition, existingConditions, now);
        updateConditionFromExisting(rollingUpdate, existingConditions, now);
        updateConditionFromExisting(updateType, existingConditions, now);

        return statusBuilder
                .withObservedGeneration(observedGeneration)
                .withConditions(List.of(readyCondition, hasErrorsCondition, rollingUpdate, updateType))
                .build();
    }

    static void updateConditionFromExisting(KeycloakStatusCondition condition, Map<String, KeycloakStatusCondition> existingConditions, String now) {
        var existing = existingConditions.get(condition.getType());
        if (existing == null) {
            if (condition.getObservedGeneration() != null) {
                condition.setLastTransitionTime(now);
            }
        } else if (condition.getObservedGeneration() == null) {
            // carry the existing forward
            condition.setLastTransitionTime(existing.getLastTransitionTime());
            condition.setObservedGeneration(existing.getObservedGeneration());
            condition.setStatus(existing.getStatus());
            if (condition.getMessage() == null) {
                condition.setMessage(existing.getMessage());
            }
        } else if (Objects.equals(existing.getStatus(), condition.getStatus())
                && Objects.equals(existing.getMessage(), condition.getMessage())) {
           condition.setLastTransitionTime(existing.getLastTransitionTime());
        } else {
           condition.setLastTransitionTime(now);
        }
    }
}
