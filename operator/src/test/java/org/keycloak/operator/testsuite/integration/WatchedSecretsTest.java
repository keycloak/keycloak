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
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.unit.WatchedResourcesTest;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@QuarkusTest
public class WatchedSecretsTest extends BaseOperatorTest {
    @Test
    public void testSecretsAreWatched() {
        var kc = getTestKeycloakDeployment(true);
        deployKeycloak(k8sclient, kc, true);

        Secret dbSecret = getDbSecret();
        Secret tlsSecret = getTlsSecret();

        Log.info("Updating DB Secret, expecting restart");
        testDeploymentRestarted(Set.of(kc), Set.of(), () -> {
            dbSecret.getData().put(UUID.randomUUID().toString(), "YmxhaGJsYWg=");
            k8sclient.resource(dbSecret).update();
        });

        Log.info("Updating TLS Secret, expecting restart");
        testDeploymentRestarted(Set.of(kc), Set.of(), () -> {
            tlsSecret.getData().put(UUID.randomUUID().toString(), "YmxhaGJsYWg=");
            k8sclient.resource(tlsSecret).update();
        });

        Log.info("Updating DB Secret metadata, NOT expecting restart");
        testDeploymentRestarted(Set.of(), Set.of(kc), () -> {
            dbSecret.getMetadata().getLabels().put(UUID.randomUUID().toString(), "YmxhaGJsYWg");
            k8sclient.resource(dbSecret).update();
        });
    }

    @DisabledIfApiServerTest
    @Test
    public void testSecretChangesArePropagated() {
        final String username = "HomerSimpson";

        var kc = getTestKeycloakDeployment(false);
        deployKeycloak(k8sclient, kc, true);

        var dbSecret = getDbSecret();

        dbSecret.getData().put("username",
                Base64.getEncoder().encodeToString(username.getBytes()));
        k8sclient.resource(dbSecret).update();

        // dynamically check pod 0 to avoid race conditions
        Awaitility.await().atMost(2, TimeUnit.MINUTES).ignoreExceptions().until(() ->
                k8sclient.pods().withName(kc.getMetadata().getName() + "-0").getLog().contains("password authentication failed for user \"" + username + "\""));
    }

    private StatefulSet getStatefulSet(Keycloak kc) {
        return k8sclient.apps().statefulSets().withName(kc.getMetadata().getName()).require();
    }

    @Test
    public void testSecretsCanBeUnWatched() {
        var kc = getTestKeycloakDeployment(false);
        deployKeycloak(k8sclient, kc, true);

        Secret dbSecret = getDbSecret();

        Log.info("Updating KC to not to rely on DB Secret");
        hardcodeDBCredsInCR(kc);
        testDeploymentRestarted(Set.of(kc), Set.of(), () -> {
            deployKeycloak(k8sclient, kc, false, false);
        });

        Awaitility.await().untilAsserted(() -> {
            Log.info("Checking StatefulSet annotations");
            assertFalse(k8sclient.resources(StatefulSet.class).withName(kc.getMetadata().getName()).get().getMetadata()
                    .getAnnotations().get(WatchedResourcesTest.KEYCLOAK_WATCHING_ANNOTATION)
                    .contains(dbSecret.getMetadata().getName()));
        });
    }

    @Test
    public void testSingleSecretMultipleKeycloaks() {
        var kc1 = getTestKeycloakDeployment(true);
        var kc1Hostname = new HostnameSpecBuilder().withHostname("kc1.local").build();
        kc1.getMetadata().setName(kc1.getMetadata().getName() + "-1");
        kc1.getSpec().setHostnameSpec(kc1Hostname);

        var kc2 = getTestKeycloakDeployment(true);
        var kc2Hostname = new HostnameSpecBuilder().withHostname("kc2.local").build();
        kc2.getMetadata().setName(kc2.getMetadata().getName() + "-2");
        kc2.getSpec().setHostnameSpec(kc2Hostname); // to prevent Ingress conflicts

        deployKeycloak(k8sclient, kc1, true);
        deployKeycloak(k8sclient, kc2, true);

        var dbSecret = getDbSecret();

        Log.info("Updating DB Secret, expecting restart of both KCs");
        testDeploymentRestarted(Set.of(kc1, kc2), Set.of(), () -> {
            dbSecret.getData().put(UUID.randomUUID().toString(), "YmxhaGJsYWg=");
            k8sclient.resource(dbSecret).update();
        });

        Log.info("Updating KC1 to not to rely on DB Secret");
        hardcodeDBCredsInCR(kc1);
        testDeploymentRestarted(Set.of(kc1), Set.of(kc2), () -> {
            deployKeycloak(k8sclient, kc1, false, false);
        });

        Log.info("Updating DB Secret, expecting restart of just KC2");
        testDeploymentRestarted(Set.of(kc2), Set.of(kc1), () -> {
            dbSecret.getData().put(UUID.randomUUID().toString(), "YmxhaGJsYWg=");
            k8sclient.resource(dbSecret).update();
        });
    }

    private void testDeploymentRestarted(Set<Keycloak> crsToBeRestarted, Set<Keycloak> crsNotToBeRestarted, Runnable action) {
        boolean restartExpected = !crsToBeRestarted.isEmpty();

        var toBeRestarted = crsToBeRestarted.stream().collect(Collectors.toMap(Function.identity(), k -> getStatefulSet(k).getStatus().getUpdateRevision()));
        var notToBeRestarted = crsNotToBeRestarted.stream().collect(Collectors.toMap(Function.identity(), k -> getStatefulSet(k).getStatus().getUpdateRevision()));

        action.run();

        Set<Keycloak> allCrs = new HashSet<>(crsToBeRestarted);
        allCrs.addAll(crsNotToBeRestarted);

        if (restartExpected) {
            Awaitility.await()
                    .untilAsserted(() -> {
                        toBeRestarted.forEach((k, version) -> {
                            // make sure a new version was fully rolled in
                            var status = getStatefulSet(k).getStatus();
                            assertThat(status.getUpdateRevision()).isEqualTo(status.getCurrentRevision());
                            assertThat(status.getUpdateRevision()).isNotEqualTo(version);
                        });
                    });
        }
        if (!notToBeRestarted.isEmpty()) {
            Awaitility.await()
                    .during(10, TimeUnit.SECONDS) // to ensure no pods were created
                    .untilAsserted(() -> {
                        notToBeRestarted.forEach((k, version) -> {
                            // make sure the version has stayed the same
                            var status = getStatefulSet(k).getStatus();
                            assertThat(status.getUpdateRevision()).isEqualTo(status.getCurrentRevision());
                            assertThat(status.getUpdateRevision()).isEqualTo(version);
                        });
                    });
        }
    }

    private Secret getDbSecret() {
		return new SecretBuilder(k8sclient.secrets().inNamespace(namespace).withName("keycloak-db-secret").get())
				.editMetadata().withResourceVersion(null).endMetadata().build();
    }

    private Secret getTlsSecret() {
		return new SecretBuilder(k8sclient.secrets().inNamespace(namespace).withName("example-tls-secret").get())
				.editMetadata().withResourceVersion(null).endMetadata().build();
    }

    private void hardcodeDBCredsInCR(Keycloak kc) {
        kc.getSpec().getDatabaseSpec().setUsernameSecret(null);
        kc.getSpec().getDatabaseSpec().setPasswordSecret(null);

        var username = new ValueOrSecret("db-username", "kc-user");
        var password = new ValueOrSecret("db-password", "testpassword");

        kc.getSpec().getAdditionalOptions().remove(username);
        kc.getSpec().getAdditionalOptions().add(username);
        kc.getSpec().getAdditionalOptions().remove(password);
        kc.getSpec().getAdditionalOptions().add(password);
    }

    @AfterEach
    public void restoreDBSecret() {
        deployDBSecret();
    }
}