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

import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.WatchedSecretsStore;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.testsuite.utils.CRAssert.assertKeycloakStatusCondition;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getDefaultKeycloakDeployment;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@QuarkusTest
public class WatchedSecretsTest extends BaseOperatorTest {
    @Test
    public void testSecretsAreWatched() {
        try {
            var kc = getDefaultKeycloakDeployment();
            deployKeycloak(k8sclient, kc, true);

            Secret dbSecret = getDbSecret();
            Secret tlsSecret = getTlsSecret();

            assertThat(dbSecret.getMetadata().getLabels()).containsEntry(Constants.KEYCLOAK_COMPONENT_LABEL, WatchedSecretsStore.WATCHED_SECRETS_LABEL_VALUE);
            assertThat(tlsSecret.getMetadata().getLabels()).containsEntry(Constants.KEYCLOAK_COMPONENT_LABEL, WatchedSecretsStore.WATCHED_SECRETS_LABEL_VALUE);

            Log.info("Updating DB Secret, expecting restart");
            testDeploymentRestarted(Set.of(kc), Set.of(), () -> {
                dbSecret.getData().put(UUID.randomUUID().toString(), "YmxhaGJsYWg=");
                k8sclient.secrets().createOrReplace(dbSecret);
            });

            Log.info("Updating TLS Secret, expecting restart");
            testDeploymentRestarted(Set.of(kc), Set.of(), () -> {
                tlsSecret.getData().put(UUID.randomUUID().toString(), "YmxhaGJsYWg=");
                k8sclient.secrets().createOrReplace(tlsSecret);
            });

            Log.info("Updating DB Secret metadata, NOT expecting restart");
            testDeploymentRestarted(Set.of(), Set.of(kc), () -> {
                dbSecret.getMetadata().getLabels().put(UUID.randomUUID().toString(), "YmxhaGJsYWg");
                k8sclient.secrets().createOrReplace(dbSecret);
            });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testSecretChangesArePropagated() {
        try {
            final String username = "HomerSimpson";

            var kc = getDefaultKeycloakDeployment();
            deployKeycloak(k8sclient, kc, true);

            var prevPodNames = getPodNamesForCrs(Set.of(kc));

            var dbSecret = getDbSecret();
            dbSecret.getData().put("username", Base64.toBase64String(username.getBytes()));
            k8sclient.secrets().createOrReplace(dbSecret);

            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        Log.info("Checking pod logs for DB auth failures");
                        var podlogs = getPodNamesForCrs(Set.of(kc)).stream()
                                .filter(n -> !prevPodNames.contains(n)) // checking just new pods
                                .map(n -> {
                                        var name = k8sclient
                                                .pods()
                                                .inNamespace(namespace)
                                                .list()
                                                .getItems()
                                                .stream()
                                                .filter(p -> (p.getMetadata().getName() + p.getMetadata().getCreationTimestamp()).equals(n))
                                                .findAny()
                                                .get()
                                                .getMetadata()
                                                .getName();

                                        return k8sclient.pods().inNamespace(namespace).withName(name).getLog();
                                })
                                .collect(Collectors.toList());
                        assertThat(podlogs).anyMatch(l -> l.contains("password authentication failed for user \"" + username + "\""));
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testSecretsCanBeUnWatched() {
        try {
            var kc = getDefaultKeycloakDeployment();
            deployKeycloak(k8sclient, kc, true);

            Log.info("Updating KC to not to rely on DB Secret");
            hardcodeDBCredsInCR(kc);
            testDeploymentRestarted(Set.of(kc), Set.of(), () -> {
                deployKeycloak(k8sclient, kc, false, false);
            });

            Log.info("Updating DB Secret to trigger clean-up process");
            testDeploymentRestarted(Set.of(), Set.of(kc), () -> {
                var dbSecret = getDbSecret();
                dbSecret.getMetadata().getLabels().put(UUID.randomUUID().toString(), "YmxhaGJsYWg");
                k8sclient.secrets().createOrReplace(dbSecret);
            });

            Awaitility.await().untilAsserted(() -> {
                Log.info("Checking labels on DB Secret");
                assertThat(getDbSecret().getMetadata().getLabels()).doesNotContainKey(Constants.KEYCLOAK_COMPONENT_LABEL);
            });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testSingleSecretMultipleKeycloaks() {
        try {
            var kc1 = getDefaultKeycloakDeployment();
            kc1.getMetadata().setName(kc1.getMetadata().getName() + "-1");
            kc1.getSpec().setHostname("kc1.local");

            var kc2 = getDefaultKeycloakDeployment();
            kc2.getMetadata().setName(kc2.getMetadata().getName() + "-2");
            kc2.getSpec().setHostname("kc2.local"); // to prevent Ingress conflicts

            deployKeycloak(k8sclient, kc1, true);
            deployKeycloak(k8sclient, kc2, true);

            var dbSecret = getDbSecret();

            Log.info("Updating DB Secret, expecting restart of both KCs");
            testDeploymentRestarted(Set.of(kc1, kc2), Set.of(), () -> {
                dbSecret.getData().put(UUID.randomUUID().toString(), "YmxhaGJsYWg=");
                k8sclient.secrets().createOrReplace(dbSecret);
            });

            Log.info("Updating KC1 to not to rely on DB Secret");
            hardcodeDBCredsInCR(kc1);
            testDeploymentRestarted(Set.of(kc1), Set.of(kc2), () -> {
                deployKeycloak(k8sclient, kc1, false, false);
            });

            Log.info("Updating DB Secret, expecting restart of just KC2");
            testDeploymentRestarted(Set.of(kc2), Set.of(kc1), () -> {
                dbSecret.getData().put(UUID.randomUUID().toString(), "YmxhaGJsYWg=");
                k8sclient.secrets().createOrReplace(dbSecret);
            });
        }
        catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    private void testDeploymentRestarted(Set<Keycloak> crsToBeRestarted, Set<Keycloak> crsNotToBeRestarted, Runnable action) {
        boolean restartExpected = !crsToBeRestarted.isEmpty();

        List<String> podsToBeRestarted = getPodNamesForCrs(crsToBeRestarted);
        List<String> podsNotToBeRestarted = getPodNamesForCrs(crsNotToBeRestarted);

        action.run();

        if (restartExpected) {
            assertRollingUpdate(crsToBeRestarted, true);
        }

        Set<Keycloak> allCrs = new HashSet<>(crsToBeRestarted);
        allCrs.addAll(crsNotToBeRestarted);
        assertRollingUpdate(allCrs, false);

        if (restartExpected) {
            Awaitility.await()
                    .untilAsserted(() -> {
                        List<String> newPods = getPodNamesForCrs(allCrs);
                        Log.infof("Pods to be restarted: %s\nPods NOT to be restarted: %s\nCurrent Pods: %s",
                                podsToBeRestarted, podsNotToBeRestarted, newPods);
                        assertThat(newPods).noneMatch(podsToBeRestarted::contains);
                        assertThat(newPods).containsAll(podsNotToBeRestarted);
                    });
        }
        else {
            Awaitility.await()
                    .during(10, TimeUnit.SECONDS) // to ensure no pods were created
                    .untilAsserted(() -> {
                        List<String> newPods = getPodNamesForCrs(allCrs);
                        Log.infof("Pods NOT to be restarted: %s, expected pods: %s\nAsserting current pods are unchanged: %s",
                                podsNotToBeRestarted, newPods);
                        assertThat(newPods).isEqualTo(podsNotToBeRestarted);
                    });
        }
    }

    private List<String> getPodNamesForCrs(Set<Keycloak> crs) {
        return k8sclient
                .pods()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(pod -> pod.getMetadata().getName() + pod.getMetadata().getCreationTimestamp())
                .filter(pod -> crs.stream().map(c -> c.getMetadata().getName()).anyMatch(pod::startsWith))
                .collect(Collectors.toList());
    }

    private void assertRollingUpdate(Set<Keycloak> crs, boolean expectedStatus) {
        Awaitility.await()
                .untilAsserted(() -> {
                    for (var cr : crs) {
                        Keycloak kc = k8sclient.resources(Keycloak.class)
                                .inNamespace(namespace)
                                .withName(cr.getMetadata().getName())
                                .get();
                        assertKeycloakStatusCondition(kc, KeycloakStatusCondition.ROLLING_UPDATE, expectedStatus);
                    }
                });
    }

    private Secret getDbSecret() {
        return k8sclient.secrets().inNamespace(namespace).withName("keycloak-db-secret").get();
    }

    private Secret getTlsSecret() {
        return k8sclient.secrets().inNamespace(namespace).withName("example-tls-secret").get();
    }

    private void hardcodeDBCredsInCR(Keycloak kc) {
        var username = new ValueOrSecret("db-username", "postgres");
        var password = new ValueOrSecret("db-password", "testpassword");

        kc.getSpec().getServerConfiguration().remove(username);
        kc.getSpec().getServerConfiguration().add(username);
        kc.getSpec().getServerConfiguration().remove(password);
        kc.getSpec().getServerConfiguration().add(password);
    }

    @AfterEach
    public void restoreDBSecret() {
        deployDBSecret();
    }
}
