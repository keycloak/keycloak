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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KeycloakStatusAggregator {
    private final KeycloakStatusCondition readyCondition;
    private final KeycloakStatusCondition hasErrorsCondition;
    private final KeycloakStatusCondition rollingUpdate;

    private final List<String> notReadyMessages = new ArrayList<>();
    private final List<String> errorMessages = new ArrayList<>();
    private final List<String> rollingUpdateMessages = new ArrayList<>();
    
    private final KeycloakStatusBuilder statusBuilder = new KeycloakStatusBuilder();

    public KeycloakStatusAggregator() {
        readyCondition = new KeycloakStatusCondition();
        readyCondition.setType(KeycloakStatusCondition.READY);
        readyCondition.setStatus(true);

        hasErrorsCondition = new KeycloakStatusCondition();
        hasErrorsCondition.setType(KeycloakStatusCondition.HAS_ERRORS);
        hasErrorsCondition.setStatus(false);

        rollingUpdate = new KeycloakStatusCondition();
        rollingUpdate.setType(KeycloakStatusCondition.ROLLING_UPDATE);
        rollingUpdate.setStatus(false);
    }

    public KeycloakStatusAggregator addNotReadyMessage(String message) {
        readyCondition.setStatus(false);
        notReadyMessages.add(message);
        return this;
    }

    public KeycloakStatusAggregator addErrorMessage(String message) {
        hasErrorsCondition.setStatus(true);
        errorMessages.add(message);
        return this;
    }

    public KeycloakStatusAggregator addWarningMessage(String message) {
        errorMessages.add("warning: " + message);
        return this;
    }

    public KeycloakStatusAggregator addRollingUpdateMessage(String message) {
        rollingUpdate.setStatus(true);
        rollingUpdateMessages.add(message);
        return this;
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
        readyCondition.setMessage(String.join("\n", notReadyMessages));
        hasErrorsCondition.setMessage(String.join("\n", errorMessages));
        rollingUpdate.setMessage(String.join("\n", rollingUpdateMessages));
        
        return statusBuilder.withConditions(List.of(readyCondition, hasErrorsCondition, rollingUpdate)).build();
    }
}
