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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.WatchedResources;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class WatchedResourcesTest {

    public static final String KEYCLOAK_WATCHING_ANNOTATION = "operator.keycloak.org/watching-secrets";
    public static final String KEYCLOAK_WATCHING_CONFIGMAPS_ANNOTATION = "operator.keycloak.org/watching-configmaps";
    public static final String KEYCLOAK_MISSING_SECRETS_ANNOTATION = "operator.keycloak.org/missing-secrets";
    public static final String KEYCLOAK_MISSING_CONFIGMAPS_ANNOTATION = "operator.keycloak.org/missing-configmaps";


    @Inject
    WatchedResources watchedResources;

    @Test
    public void testHashing() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Utils.hash(List.of()));
        assertEquals("b5655bfe4d4e130f5023a76a5de0906cf84eb5895bda5d44642673f9eb4024bf", Utils.hash(List.of(newSecret(Map.of("a", "b")), newSecret(Map.of("c", "d")))));
        assertEquals("d526224334e65c71095be909b2d14c52f1589abb84a3c76fbe79dd75d7132fbb",
                Utils.hash(List.of(new ConfigMapBuilder().withNewMetadata().withName("x")
                        .withAnnotations(Map.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                        .endMetadata().withData(Map.of("a", "b")).build())));
    }

    private Secret newSecret(Map<String, String> data) {
        return new SecretBuilder().withNewMetadata().withName(UUID.randomUUID().toString())
                .withLabels(Map.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())).endMetadata()
                .withData(data).build();
    }

}
