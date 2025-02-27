/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.keycloak.common.Profile;
import org.keycloak.operator.controllers.KeycloakUpdateJobDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UpdateSpec;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.upgrade.UpdateStrategy;
import org.keycloak.operator.upgrade.impl.AutoUpgradeLogic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.operator.testsuite.utils.CRAssert.assertKeycloakStatusCondition;
import static org.keycloak.operator.testsuite.utils.CRAssert.eventuallyRecreateUpgradeStatus;
import static org.keycloak.operator.testsuite.utils.CRAssert.eventuallyRollingUpgradeStatus;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

@QuarkusTest
@Disabled(value = "unstable")
public class UpgradeTest extends BaseOperatorTest {

    @ParameterizedTest(name = "testImageChange-{0}")
    @EnumSource(UpdateStrategy.class)
    public void testImageChange(UpdateStrategy updateStrategy) throws InterruptedException {
        var kc = createInitialDeployment(updateStrategy);
        deployKeycloak(k8sclient, kc, true);
        assertUnknownUpdateTypeStatus(kc);

        var stsGetter = k8sclient.apps().statefulSets().inNamespace(namespace).withName(kc.getMetadata().getName());
        final String newImage = "quay.io/keycloak/non-existing-keycloak";

        // changing the image to non-existing will always use the recreate upgrade type.
        kc.getSpec().setImage(newImage);
        var upgradeCondition = eventuallyRecreateUpgradeStatus(k8sclient, kc);

        deployKeycloak(k8sclient, kc, false);
        await(upgradeCondition);
        switch (updateStrategy) {
            case AUTO:
                // Sometimes the pod disappears, and the status could be:
                // - Unexpected update-compatibility command error.
                // - Unexpected update-compatibility command exit code.
                assertRecreateUpdateTypeStatus(kc, "Unexpected update-compatibility command");
                break;
            case FORCE_RECREATE:
                assertRecreateUpdateTypeStatus(kc, "Strategy ForceRecreate configured.");
                break;
            case RECREATE_ON_IMAGE_CHANGE:
                assertRecreateUpdateTypeStatus(kc, "Image changed");
                break;
        }

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var sts = stsGetter.get();
                    assertEquals(kc.getSpec().getInstances(), sts.getSpec().getReplicas()); // just checking specs as we're using a non-existing image
                    assertEquals(newImage, sts.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());

                    var currentKc = k8sclient.resources(Keycloak.class)
                            .inNamespace(namespace).withName(kc.getMetadata().getName()).get();
                    assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.READY, false, "Waiting for more replicas");
                });

        if (updateStrategy == UpdateStrategy.AUTO) {
            assertUpdateJobExists(kc);
        }
    }

    @ParameterizedTest(name = "testCacheMaxCount-{0}")
    @EnumSource(UpdateStrategy.class)
    public void testCacheMaxCount(UpdateStrategy updateStrategy) throws InterruptedException {
        var kc = createInitialDeployment(updateStrategy);
        deployKeycloak(k8sclient, kc, true);
        assertUnknownUpdateTypeStatus(kc);

        // changing the local cache max-count should never use the recreate upgrade type
        // except if forced by the Keycloak CR.
        kc.getSpec().getAdditionalOptions().add(new ValueOrSecret("cache-embedded-authorization-max-count", "10"));
        var upgradeCondition = updateStrategy == UpdateStrategy.FORCE_RECREATE ?
                eventuallyRecreateUpgradeStatus(k8sclient, kc) :
                eventuallyRollingUpgradeStatus(k8sclient, kc);

        deployKeycloak(k8sclient, kc, true);
        await(upgradeCondition);
        switch (updateStrategy) {
            case AUTO:
                assertRollingUpdateTypeStatus(kc, "Compatible changes detected.");
                break;
            case FORCE_RECREATE:
                assertRecreateUpdateTypeStatus(kc, "Strategy ForceRecreate configured.");
                break;
            case RECREATE_ON_IMAGE_CHANGE:
                assertRollingUpdateTypeStatus(kc, "Image unchanged");
                break;
        }

        if (updateStrategy == UpdateStrategy.AUTO) {
            assertUpdateJobExists(kc);
        }
    }

    @ParameterizedTest(name = "testOptimizedImage-{0}")
    @EnumSource(UpdateStrategy.class)
    @EnabledIfSystemProperty(named = OPERATOR_CUSTOM_IMAGE, matches = ".+")
    public void testOptimizedImage(UpdateStrategy updateStrategy) throws InterruptedException {
        // In GHA, the custom image is an optimized image of the base image.
        // We should be able to do a zero-downtime upgrade with Auto strategy.
        var kc = createInitialDeployment(updateStrategy);
        // use the base image
        kc.getSpec().setImage(null);
        deployKeycloak(k8sclient, kc, true);
        assertUnknownUpdateTypeStatus(kc);

        // use the optimized image, auto strategy should use a rolling upgrade
        kc.getSpec().setImage(getTestCustomImage());
        var upgradeCondition = updateStrategy == UpdateStrategy.AUTO ?
                eventuallyRollingUpgradeStatus(k8sclient, kc) :
                eventuallyRecreateUpgradeStatus(k8sclient, kc);

        deployKeycloak(k8sclient, kc, true);
        await(upgradeCondition);
        switch (updateStrategy) {
            case AUTO:
                assertRollingUpdateTypeStatus(kc, "Compatible changes detected.");
                break;
            case FORCE_RECREATE:
                assertRecreateUpdateTypeStatus(kc, "Strategy ForceRecreate configured.");
                break;
            case RECREATE_ON_IMAGE_CHANGE:
                assertRecreateUpdateTypeStatus(kc, "Image changed");
                break;
        }

        if (updateStrategy == UpdateStrategy.AUTO) {
            assertUpdateJobExists(kc);
        }
    }

    @EnabledIfSystemProperty(named = OPERATOR_CUSTOM_IMAGE, matches = ".+")
    @Test
    public void testNoJobReuse() throws InterruptedException {
        // In GHA, the custom image is an optimized image of the base image.
        // We should be able to do a zero-downtime upgrade with Auto strategy.
        var kc = createInitialDeployment(UpdateStrategy.AUTO);
        // use the base image
        kc.getSpec().setImage(null);
        deployKeycloak(k8sclient, kc, true);
        assertUnknownUpdateTypeStatus(kc);

        // let's trigger a rolling upgrade
        var upgradeCondition = eventuallyRollingUpgradeStatus(k8sclient, kc);
        kc.getSpec().setImage(getTestCustomImage());

        deployKeycloak(k8sclient, kc, true);
        await(upgradeCondition);
        assertRollingUpdateTypeStatus(kc, "Compatible changes detected.");

        var job = assertUpdateJobExists(kc);
        var hash = job.getMetadata().getAnnotations().get(KeycloakUpdateJobDependentResource.KEYCLOAK_CR_HASH_ANNOTATION);
        assertEquals(0, containerExitCode(job));

        //let's trigger a recreate
        upgradeCondition = eventuallyRecreateUpgradeStatus(k8sclient, kc);
        // enough to crash the Pod and return exit code != 0
        kc.getSpec().setImage("quay.io/keycloak/non-existing-keycloak");

        deployKeycloak(k8sclient, kc, false);
        await(upgradeCondition);
        assertRecreateUpdateTypeStatus(kc, "Unexpected update-compatibility command");

        job = assertUpdateJobExists(kc);
        var newHash = job.getMetadata().getAnnotations().get(KeycloakUpdateJobDependentResource.KEYCLOAK_CR_HASH_ANNOTATION);
        assertNotEquals(hash, newHash);
        assertNotEquals(0, containerExitCode(job));
    }

    private Job assertUpdateJobExists(Keycloak keycloak) {
        var job = k8sclient.batch().v1().jobs()
                .inNamespace(keycloak.getMetadata().getNamespace())
                .withName(KeycloakUpdateJobDependentResource.jobName(keycloak))
                .get();
        assertNotNull(job);
        var maybeStatus = Optional.ofNullable(job.getStatus());
        var finished = maybeStatus.map(JobStatus::getSucceeded).orElse(0) +
                maybeStatus.map(JobStatus::getFailed).orElse(0);
        assertEquals(1, finished);
        return job;
    }

    private int containerExitCode(Job job) {
        var maybeExitCode = AutoUpgradeLogic.findPodForJob(k8sclient, job)
                .flatMap(AutoUpgradeLogic::container)
                .map(AutoUpgradeLogic::exitCode);
        assertTrue(maybeExitCode.isPresent());
        return maybeExitCode.get();
    }

    private void assertUnknownUpdateTypeStatus(Keycloak keycloak) {
        var current = k8sclient.resource(keycloak).get();
        CRAssert.assertKeycloakStatusCondition(current, KeycloakStatusCondition.UPDATE_TYPE, null);
    }

    private void assertRecreateUpdateTypeStatus(Keycloak keycloak, String reason) {
        var current = k8sclient.resource(keycloak).get();
        CRAssert.assertKeycloakStatusCondition(current, KeycloakStatusCondition.UPDATE_TYPE, true, reason);
    }

    private void assertRollingUpdateTypeStatus(Keycloak keycloak, String reason) {
        var current = k8sclient.resource(keycloak).get();
        CRAssert.assertKeycloakStatusCondition(current, KeycloakStatusCondition.UPDATE_TYPE, false, reason);
    }

    private static Keycloak createInitialDeployment(UpdateStrategy updateStrategy) {
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().setInstances(3);
        if (updateStrategy == null) {
            return kc;
        }
        var updateSpec = new UpdateSpec();
        updateSpec.setStrategy(updateStrategy);
        kc.getSpec().setUpdateSpec(updateSpec);

        if (kc.getSpec().getFeatureSpec() == null) {
            kc.getSpec().setFeatureSpec(new FeatureSpec());
        }
        kc.getSpec().getFeatureSpec().setEnabledFeatures(List.of(Profile.Feature.ROLLING_UPDATES.getKey()));
        return kc;
    }

    private static <T> void await(CompletableFuture<T> future) throws InterruptedException {
        try {
            future.get(2, TimeUnit.MINUTES);
        } catch (ExecutionException | TimeoutException e) {
            throw new AssertionError(e);
        }
    }
}
