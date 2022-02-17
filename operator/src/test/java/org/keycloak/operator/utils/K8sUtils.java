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

package org.keycloak.operator.utils;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import org.awaitility.Awaitility;
import org.keycloak.operator.v2alpha1.crds.Keycloak;
import org.keycloak.operator.v2alpha1.crds.KeycloakStatusCondition;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class K8sUtils {
    public static <T> T getResourceFromFile(String fileName) {
        return Serialization.unmarshal(Objects.requireNonNull(K8sUtils.class.getResourceAsStream("/" + fileName)), Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getResourceFromMultiResourceFile(String fileName, int index) {
        return ((List<T>) getResourceFromFile(fileName)).get(index);
    }

    public static Keycloak getDefaultKeycloakDeployment() {
        return getResourceFromMultiResourceFile("example-keycloak.yml", 0);
    }

    public static void deployKeycloak(KubernetesClient client, Keycloak kc, boolean waitUntilReady) {
        client.resources(Keycloak.class).createOrReplace(kc);

        if (waitUntilReady) {
            waitForKeycloakToBeReady(client, kc);
        }
    }

    public static void deployDefaultKeycloak(KubernetesClient client) {
        deployKeycloak(client, getDefaultKeycloakDeployment(), true);
    }

    public static void waitForKeycloakToBeReady(KubernetesClient client, Keycloak kc) {
        Log.infof("Waiting for Keycloak \"%s\"", kc.getMetadata().getName());
        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var currentKc = client.resources(Keycloak.class).withName(kc.getMetadata().getName()).get();
                    CRAssert.assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.READY, true);
                    CRAssert.assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.HAS_ERRORS, false);
                });
    }
}
