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

package org.keycloak.operator.testsuite.unit;

import io.fabric8.kubernetes.client.utils.Serialization;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.testsuite.utils.CRAssert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class KeycloakStatusTest {

    @Test
    public void testEqualityWithScale() {
        KeycloakStatus status1 = new KeycloakStatusAggregator(0L).apply(b -> b.withInstances(1)).build();

        KeycloakStatus status2 = new KeycloakStatusAggregator(0L).apply(b -> b.withInstances(2)).build();

        assertNotEquals(status1, status2);
    }

    @Test
    public void testDefaults() {
        KeycloakStatus status = new KeycloakStatusAggregator(1L).build();
        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.READY, true, "", 1L);

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.ROLLING_UPDATE, false, "", 1L);

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.HAS_ERRORS, false, "", 1L);
    }

    @Test
    public void testReadyWithWarning() {
        KeycloakStatus status = new KeycloakStatusAggregator(1L).addWarningMessage("something's not right").build();
        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.READY, true, "", 1L);

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.HAS_ERRORS, false, "warning: something's not right", 1L); // could also be unknown
    }

    @Test
    public void testNotReady() {
        KeycloakStatus status = new KeycloakStatusAggregator(1L).addNotReadyMessage("waiting").build();
        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.READY, false, "waiting", 1L);

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.HAS_ERRORS, false, "", 1L);
    }

    @Test
    public void testReadyRolling() {
        KeycloakStatus status = new KeycloakStatusAggregator(1L).addRollingUpdateMessage("rolling").build();
        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.READY, true, "", 1L);

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.ROLLING_UPDATE, true, "rolling", 1L);
    }

    @Test
    public void testError() {
        // without prior status, ready and rolling are unknown
        KeycloakStatus status = new KeycloakStatusAggregator(1L).addErrorMessage("this is bad").build();
        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.READY, null, null, null);

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.HAS_ERRORS, true, "this is bad", 1L);

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.ROLLING_UPDATE, null, null, null);
    }

    @Test
    public void testErrorWithPriorStatus() {
        // with prior status, ready and rolling are preserved
        KeycloakStatus prior = new KeycloakStatusAggregator(1L).build();
        prior.getConditions().stream().forEach(c -> c.setLastTransitionTime("prior"));

        KeycloakStatus status = new KeycloakStatusAggregator(prior, 2L).addErrorMessage("this is bad").build();
        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.READY, true, "", 1L)
            .extracting(KeycloakStatusCondition::getLastTransitionTime).isEqualTo("prior");

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.HAS_ERRORS, true, "this is bad", 2L)
            .extracting(KeycloakStatusCondition::getLastTransitionTime).isNotEqualTo("prior");

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.ROLLING_UPDATE, false, "", 1L);
    }

    @Test
    public void testReadyWithPriorStatus() {
        // without prior status, ready and rolling are known and keep the transition time
        KeycloakStatus prior = new KeycloakStatusAggregator(1L).build();
        prior.getConditions().stream().forEach(c -> c.setLastTransitionTime("prior"));

        KeycloakStatus status = new KeycloakStatusAggregator(prior, 2L).build();
        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.READY, true, "", 2L)
            .extracting(KeycloakStatusCondition::getLastTransitionTime).isEqualTo("prior");

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.HAS_ERRORS, false, "", 2L);

        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.ROLLING_UPDATE, false, "", 2L);
    }

    @Test
    public void testMessagesChangesLastTransitionTime() {
        KeycloakStatus prior = new KeycloakStatusAggregator(1L).build();
        prior.getConditions().stream().forEach(c -> {
            c.setLastTransitionTime("prior");
            c.setMessage("old");
        });

        KeycloakStatus status = new KeycloakStatusAggregator(prior, 2L).build();
        CRAssert.assertKeycloakStatusCondition(status, KeycloakStatusCondition.READY, true, "", 2L).has(new Condition<>(
                c -> !c.getLastTransitionTime().equals("prior") && !c.getMessage().equals("old"), "transitioned"));
    }

    @Test
    public void testPreservesScale() {
        KeycloakStatus prior = new KeycloakStatusAggregator(1L).apply(b -> b.withObservedGeneration(1L).withInstances(3)).build();
        prior.getConditions().stream().forEach(c -> c.setLastTransitionTime("prior"));

        KeycloakStatus status = new KeycloakStatusAggregator(prior, 2L).apply(b -> b.withObservedGeneration(2L)).build();
        assertEquals(2, status.getObservedGeneration());
        assertEquals(3, status.getInstances());
    }

    @Test
    public void testStatusSerializtion() {
        KeycloakStatusCondition condition = new KeycloakStatusCondition();
        condition.setStatus(false);

        String yaml = Serialization.asYaml(condition);
        assertEquals("---\nstatus: \"False\"\n", yaml);

        var deserialized = Serialization.unmarshal(yaml, KeycloakStatusCondition.class);
        assertFalse(deserialized.getStatus());
    }

}
