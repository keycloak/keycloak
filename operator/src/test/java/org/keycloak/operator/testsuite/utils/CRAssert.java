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

package org.keycloak.operator.testsuite.utils;

import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;

import org.assertj.core.api.ObjectAssert;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class CRAssert {

    public static void assertKeycloakStatusCondition(Keycloak kc, String condition, boolean status) {
        assertKeycloakStatusCondition(kc, condition, status, null);
    }
    public static void assertKeycloakStatusCondition(Keycloak kc, String condition, boolean status, String containedMessage) {
        Log.debugf("Asserting CR: %s, condition: %s, status: %s, message: %s", kc.getMetadata().getName(), condition, status, containedMessage);
        try {
            assertKeycloakStatusCondition(kc.getStatus(), condition, status, containedMessage, null);
        } catch (Exception e) {
            Log.infof("Asserting CR: %s with status:\n%s", kc.getMetadata().getName(), Serialization.asYaml(kc.getStatus()));
            throw e;
        }
    }

    public static void assertKeycloakStatusCondition(KeycloakStatus kcStatus, String condition, Boolean status, String containedMessage) {
        assertKeycloakStatusCondition(kcStatus, condition, status, containedMessage, null);
    }

    public static ObjectAssert<KeycloakStatusCondition> assertKeycloakStatusCondition(KeycloakStatus kcStatus, String condition, Boolean status, String containedMessage, Long observedGeneration) {
        KeycloakStatusCondition statusCondition = kcStatus.findCondition(condition).orElseThrow();
        assertThat(statusCondition.getStatus()).isEqualTo(status);
        if (containedMessage != null) {
            assertThat(statusCondition.getMessage()).contains(containedMessage);
        }
        if (observedGeneration != null) {
            assertThat(statusCondition.getObservedGeneration()).isEqualTo(observedGeneration);
        }
        if (status != null) {
            assertThat(statusCondition.getLastTransitionTime()).isNotNull();
        }
        return assertThat(statusCondition);
    }

    public static void assertKeycloakStatusDoesNotContainMessage(KeycloakStatus kcStatus, String message) {
        assertThat(kcStatus.getConditions())
                .noneMatch(c -> c.getMessage().contains(message));
    }

    public static void assertKeycloakRealmImportStatusCondition(KeycloakRealmImport kri, String condition, Boolean status) {
        assertThat(kri.getStatus().getConditions())
                .anyMatch(c -> c.getType().equals(condition) && Objects.equals(c.getStatus(), status));
    }
}
