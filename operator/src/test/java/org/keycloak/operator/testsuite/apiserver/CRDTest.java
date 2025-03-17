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

package org.keycloak.operator.testsuite.apiserver;

import io.fabric8.kubeapitest.junit.EnableKubeAPIServer;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;

import java.io.FileNotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportBuilder;
import org.keycloak.operator.testsuite.integration.BaseOperatorTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;
import org.keycloak.operator.update.UpdateStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnableKubeAPIServer
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
    public void testRealmImportWithoutRequiredFields() {
        KeycloakRealmImport cr = new KeycloakRealmImportBuilder()
                .withNewMetadata()
                    .withName("invalid-realm")
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        var eMsg = assertThrows(KubernetesClientException.class, () -> client.resource(cr).create()).getMessage();
        assertThat(eMsg).contains("spec.keycloakCRName: Required value", "spec.realm: Required value");
    }

    @Test
    public void testKeycloak() {
        roundTrip("/test-serialization-keycloak-cr.yml", Keycloak.class);

        // ensure that server side apply works
        var kc = client.resources(Keycloak.class).withName("test-serialization-kc").get();
        kc.setStatus(new KeycloakStatusAggregator(1L).build());
        kc = client.resource(kc).updateStatus();
        kc.getMetadata().setManagedFields(null);
        kc.getMetadata().getAnnotations().put("x", "y");
        kc = client.resource(kc).serverSideApply();
        assertThat(kc.getMetadata().getAnnotations()).containsEntry("x", "y");
    }

    @Test
    public void testKeycloakWithDefaultReplicas() {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        kc.getSpec().setInstances(null);

        assertThat(client.resource(kc).create().getSpec().getInstances()).isNull();
    }

    @Test
    public void testUpdateSpecValidation() {
        var cr = new KeycloakBuilder()
                .withNewMetadata()
                .withName("invalid-keycloak")
                .endMetadata()
                .withNewSpec()
                .withNewUpdateSpec()
                .withStrategy(UpdateStrategy.EXPLICIT)
                .endUpdateSpec()
                .endSpec()
                .build();

        var eMsg = assertThrows(KubernetesClientException.class, () -> client.resource(cr).create()).getMessage();
        assertThat(eMsg).contains("spec.update: Invalid value: \"object\": The 'revision' field is required when 'Explicit' strategy is used.");
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
