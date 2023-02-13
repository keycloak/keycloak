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

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KeycloakStatusBuilder {
    private final KeycloakStatusCondition readyCondition;
    private final KeycloakStatusCondition hasErrorsCondition;
    private final KeycloakStatusCondition rollingUpdate;

    private final List<String> notReadyMessages = new ArrayList<>();
    private final List<String> errorMessages = new ArrayList<>();
    private final List<String> rollingUpdateMessages = new ArrayList<>();

    public KeycloakStatusBuilder() {
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

    public KeycloakStatusBuilder addNotReadyMessage(String message) {
        readyCondition.setStatus(false);
        notReadyMessages.add(message);
        return this;
    }

    public KeycloakStatusBuilder addErrorMessage(String message) {
        hasErrorsCondition.setStatus(true);
        errorMessages.add(message);
        return this;
    }

    public KeycloakStatusBuilder addWarningMessage(String message) {
        errorMessages.add("warning: " + message);
        return this;
    }

    public KeycloakStatusBuilder addRollingUpdateMessage(String message) {
        rollingUpdate.setStatus(true);
        rollingUpdateMessages.add(message);
        return this;
    }

    public KeycloakStatus build() {
        readyCondition.setMessage(String.join("\n", notReadyMessages));
        hasErrorsCondition.setMessage(String.join("\n", errorMessages));
        rollingUpdate.setMessage(String.join("\n", rollingUpdateMessages));

        KeycloakStatus status = new KeycloakStatus();
        status.setConditions(List.of(readyCondition, hasErrorsCondition, rollingUpdate));
        return status;
    }
}
