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

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.WatchedSecretsController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class WatchedSecretsControllerTest {

    @Inject
    WatchedSecretsController watchedSecretsController;

    @Test
    public void testSecretHashing() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", watchedSecretsController.getSecretHash(List.of()));
        assertEquals("b5655bfe4d4e130f5023a76a5de0906cf84eb5895bda5d44642673f9eb4024bf", watchedSecretsController.getSecretHash(List.of(newSecret(Map.of("a", "b")), newSecret(Map.of("c", "d")))));
    }

    @Test
    public void testGetSecretNames() {
        assertEquals(List.of(), watchedSecretsController.getSecretNames(new StatefulSetBuilder().withNewMetadata().addToAnnotations(Constants.KEYCLOAK_WATCHING_ANNOTATION, "").endMetadata().build()));
        assertEquals(Arrays.asList("something"), watchedSecretsController.getSecretNames(new StatefulSetBuilder().withNewMetadata().addToAnnotations(Constants.KEYCLOAK_WATCHING_ANNOTATION, "something").endMetadata().build()));
        assertEquals(Arrays.asList("x", "y"), watchedSecretsController.getSecretNames(new StatefulSetBuilder().withNewMetadata().addToAnnotations(Constants.KEYCLOAK_WATCHING_ANNOTATION, "x;y").endMetadata().build()));
    }

    private Secret newSecret(Map<String, String> data) {
        return new SecretBuilder().withNewMetadata().withName(UUID.randomUUID().toString())
                .withLabels(Map.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())).endMetadata()
                .withData(data).build();
    }

}
