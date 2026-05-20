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

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.testsuite.integration.BaseOperatorTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubeapitest.junit.EnableKubeAPIServer;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableKubeAPIServer
public class CRDUpgradeTest {

    static KubernetesClient client;

    static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testKeycloak() throws FileNotFoundException {
        //start with the v2alpha1 crd
        K8sUtils.set(client, this.getClass().getResourceAsStream("/v2alpha1-keycloak-crd.yaml"));

        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS).ignoreExceptions().until(() -> {
            return !client.getApiResources("k8s.keycloak.org/v2alpha1").getResources().isEmpty();
        });

        var resource = client.resource(getClass().getResourceAsStream("/example-keycloak.yaml"));
        var result = resource.create();
        assertEquals("k8s.keycloak.org/v2alpha1", result.getApiVersion());

        // update the CRD
        BaseOperatorTest.createCRDs(client);

        // ensure that v2beta1 works
        var kc = client.resources(Keycloak.class).withName("example-kc").get();
        assertEquals("k8s.keycloak.org/v2beta1", kc.getApiVersion());
        assertEquals("xforwarded", kc.getSpec().getProxySpec().getHeaders());
    }

}
