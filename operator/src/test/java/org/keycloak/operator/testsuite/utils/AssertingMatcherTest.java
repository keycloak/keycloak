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

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

/**
 * @author Alexander Schwartz
 */
class AssertingMatcherTest {

    @Test
    public void shouldPassOnTheInstance() {
        //
        String message = "mymessage";

        // when
        Awaitility.await()
                .pollInterval(1, TimeUnit.MILLISECONDS)
                .timeout(100, TimeUnit.MILLISECONDS)
                .pollInSameThread()
                .ignoreExceptions()
                .until(() -> buildKeycloakWithStatusMessage(message),
                        new AssertingMatcher<>(value -> {
                            assertThat(value.getStatus().getConditions().get(0).getMessage()).isEqualTo(message);
                        }));
    }

    /**
     * Use this test interactively in the IDE to try out different exceptions to be thrown.
     */
    @Test
    @Disabled
    public void justForTestingInTheIDE() {
        String message = "mymessage";

        Awaitility.await()
                .pollInterval(1, TimeUnit.MILLISECONDS)
                .timeout(100, TimeUnit.MILLISECONDS)
                .pollInSameThread()
                .ignoreExceptions()
                .until(() -> buildKeycloakWithStatusMessage(message),
                        new AssertingMatcher<>(keycloak ->
                                assertThat(keycloak.getStatus().getConditions().get(0).getMessage()).isNotEqualTo(message)
                            ));
    }

    @Test
    public void shouldShowTheStateInTheException() {
        String message = "mymessage";

        // when
        Throwable thrown = catchThrowable(() -> {
            Awaitility.await()
                    .pollInterval(1, TimeUnit.MILLISECONDS)
                    .timeout(2, TimeUnit.MILLISECONDS)
                    .pollInSameThread()
                    .ignoreExceptions()
                    .until(() -> buildKeycloakWithStatusMessage(message),
                            new AssertingMatcher<>(keycloak ->
                                    assertThat(keycloak.getStatus().getConditions().get(0).getMessage()).isNotEqualTo(message)));
        });

        // then

        /* Lambda expression in org.keycloak.operator.testsuite.utils.AssertingMatcherTest: expected to fulfill the condition but with value <CustomResource{kind='Keycloak',
         * apiVersion='k8s.keycloak.org/v2alpha1', metadata=ObjectMeta(annotations=null, clusterName=null, creationTimestamp=null, deletionGracePeriodSeconds=null,
         * deletionTimestamp=null, finalizers=[], generateName=null, generation=null, labels=null, managedFields=[], name=null, namespace=null, ownerReferences=[],
         * resourceVersion=null, selfLink=null, uid=null, additionalProperties={}), spec=null, status=KeycloakStatus{conditions=[KeycloakStatusCondition{type='null',
         * status=false, message='mymessage'}]}}> it failed with
         * Expecting actual:
         *   "mymessage"
         * not to be equal to:
         *   "mymessage"
         * within 2 milliseconds.
         */
        assertThat(thrown)
                .isInstanceOf(ConditionTimeoutException.class)
                .hasMessageContaining(message);
    }

    private Keycloak buildKeycloakWithStatusMessage(String message) {
        Keycloak keycloak = new Keycloak();
        KeycloakStatus status = new KeycloakStatus();
        KeycloakStatusCondition condition = new KeycloakStatusCondition();
        condition.setStatus(false);
        condition.setMessage(message);
        List<KeycloakStatusCondition> conditions = new LinkedList<>();
        conditions.add(condition);
        status.setConditions(conditions);
        keycloak.setStatus(status);
        return keycloak;
    }

}