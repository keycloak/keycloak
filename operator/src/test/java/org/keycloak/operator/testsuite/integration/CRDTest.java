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

package org.keycloak.operator.testsuite.integration;

import io.fabric8.junit.jupiter.api.KubernetesTest;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;

import java.io.FileNotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@KubernetesTest
public class CRDTest {

    static KubernetesClient client;

    static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    public static void before() throws FileNotFoundException {
        BaseOperatorTest.createCRDs(client);
    }

    @Test
    public void testRealmImport() {
        roundTrip("/test-serialization-realmimport-cr.yml", KeycloakRealmImport.class);
    }

    @Test
    public void testKeycloak() {
        roundTrip("/test-serialization-keycloak-cr.yml", Keycloak.class);
    }

    private <T extends HasMetadata> void roundTrip(String resourceFile, Class<T> type) {
        // could also test the status, but that is not part of the expected files
        // also to test the status we may need the operator to not be running, which
        // means don't run these tests if remote
        Resource<T> resource = client.resources(type).load(this.getClass().getResourceAsStream(resourceFile));
        T parsed = resource.item();
        T fromServer = resource.create();
        //T fromServer = resource.updateStatus();

        var parsedTree = mapper.valueToTree(parsed);
        var actualTree = mapper.valueToTree(fromServer);

        assertThat(parsedTree.get("spec")).isEqualTo(actualTree.get("spec"));
        //assertThat(parsedTree.get("status")).isEqualTo(actualTree.get("status"));
    }

}
