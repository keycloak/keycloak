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

import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpecUnsupported;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.utils.OperatorConditions.checkCondition;
import static org.keycloak.utils.OperatorConditions.isNull;
import static org.keycloak.utils.OperatorConditions.notNull;

public class OperatorConditionsTest {

    @Test
    public void nullObjects() {
        final var nullSpec = (KeycloakSpec) null;
        checkNullObjects(() -> nullSpec);

        final Optional<KeycloakSpec> optionalNullSpec = Optional.ofNullable(null);
        checkNullObjects(() -> optionalNullSpec.get());
    }

    @Test
    public void notNullObjects() {
        final var spec = new KeycloakSpec();
        final var unsupported = new KeycloakSpecUnsupported();
        final var podTemplateSet = new PodTemplateSpec();

        spec.setHostname("hostname");
        podTemplateSet.setAdditionalProperty("asdf", "asdf");
        unsupported.setPodTeplate(podTemplateSet);
        spec.setUnsupported(unsupported);

        assertThat(isNull(() -> spec.getHostname().isEmpty())).isFalse();
        assertThat(notNull(() -> spec.getHostname().isEmpty())).isTrue();
        assertThat(checkCondition(() -> spec.getHostname().equals("hostname"))).isTrue();
        assertThat(checkCondition(() -> spec.getHostname().equals(null))).isFalse();

        assertThat(isNull(() -> spec.getUnsupported().getPodTemplate().getAdditionalProperties())).isFalse();
        assertThat(notNull(() -> spec.getUnsupported().getPodTemplate().getAdditionalProperties())).isTrue();
        assertThat(checkCondition(() -> spec.getUnsupported().getPodTemplate().getAdditionalProperties().size() == 1)).isTrue();
    }

    private void checkNullObjects(Supplier<KeycloakSpec> spec) {
        assertThat(isNull(() -> spec.get().getServerConfiguration().isEmpty())).isTrue();
        assertThat(isNull(() -> spec.get().getUnsupported().getPodTemplate().getMetadata().getName())).isTrue();

        assertThat(notNull(() -> spec.get().getServerConfiguration().isEmpty())).isFalse();
        assertThat(notNull(() -> spec.get().getUnsupported().getPodTemplate().getMetadata().getName())).isFalse();

        assertThat(checkCondition(() -> spec.get().getServerConfiguration() != null)).isFalse();
        assertThat(checkCondition(() -> spec.get().getServerConfiguration().isEmpty() == true)).isFalse();
    }
}