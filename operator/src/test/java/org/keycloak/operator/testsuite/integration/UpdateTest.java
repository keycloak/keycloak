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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.operator.testsuite.utils.CRAssert.eventuallyRecreateUpdateStatus;
import static org.keycloak.operator.testsuite.utils.CRAssert.eventuallyRollingUpdateStatus;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.Gettable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.KeycloakUpdateJobDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UpdateSpec;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.update.UpdateStrategy;
import org.keycloak.operator.update.impl.AutoUpdateLogic;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.quarkus.test.junit.QuarkusTest;

@Tag(BaseOperatorTest.SLOW)
@QuarkusTest
public class UpdateTest extends BaseOperatorTest {

    @ParameterizedTest(name = "testImageChange-{0}")
    @EnumSource(UpdateStrategy.class)
    public void testImageChange(UpdateStrategy updateStrategy) throws InterruptedException {
        Assumptions.assumeTrue(operatorDeployment != OperatorDeployment.local_apiserver || updateStrategy != UpdateStrategy.AUTO);
        var kc = createInitialDeployment(updateStrategy);
        var updateCondition = assertUnknownUpdateTypeStatus(kc);
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        // changing the image to non-existing will always use the recreate update type.
        kc.getSpec().setImage("quay.io/keycloak/non-existing-keycloak");
        disableProbes(kc);
        updateCondition = switch (updateStrategy) {
            case AUTO -> eventuallyRecreateUpdateStatus(k8sclient, kc, "Unexpected update-compatibility command");
            case RECREATE_ON_IMAGE_CHANGE -> eventuallyRecreateUpdateStatus(k8sclient, kc, "Image changed");
            case EXPLICIT -> eventuallyRollingUpdateStatus(k8sclient, kc, "Explicit strategy configured. Revision matches.");
        };

        deployKeycloak(k8sclient, kc, false);
        await(updateCondition);

        if (updateStrategy == UpdateStrategy.AUTO) {
            assertUpdateJobExists(kc);
        }
    }

    @ParameterizedTest(name = "testCacheMaxCount-{0}")
    @EnumSource(UpdateStrategy.class)
    public void testCacheMaxCount(UpdateStrategy updateStrategy) throws InterruptedException {
        Assumptions.assumeTrue(operatorDeployment != OperatorDeployment.local_apiserver || updateStrategy != UpdateStrategy.AUTO);
        var kc = createInitialDeployment(updateStrategy);
        var updateCondition = assertUnknownUpdateTypeStatus(kc);
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        // changing the local cache max-count should never use the recreate update type
        kc.getSpec().getAdditionalOptions().add(new ValueOrSecret("cache-embedded-authorization-max-count", "10"));

        updateCondition = switch (updateStrategy) {
            case AUTO -> eventuallyRollingUpdateStatus(k8sclient, kc, "Compatible changes detected.");
            case RECREATE_ON_IMAGE_CHANGE -> eventuallyRollingUpdateStatus(k8sclient, kc, "Image unchanged");
            case EXPLICIT -> eventuallyRollingUpdateStatus(k8sclient, kc, "Explicit strategy configured. Revision matches.");
        };

        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        if (updateStrategy == UpdateStrategy.AUTO) {
            assertUpdateJobExists(kc);
        }
    }

    @ParameterizedTest(name = "testOptimizedImage-{0}")
    @EnumSource(UpdateStrategy.class)
    @EnabledIfSystemProperty(named = OPERATOR_CUSTOM_IMAGE, matches = ".+")
    public void testOptimizedImage(UpdateStrategy updateStrategy) throws InterruptedException {
        Assumptions.assumeTrue(operatorDeployment != OperatorDeployment.local_apiserver || updateStrategy != UpdateStrategy.AUTO);
        // In GHA, the custom image is an optimized image of the base image.
        // We should be able to do a zero-downtime update with Auto strategy.
        var kc = createInitialDeployment(updateStrategy);
        // use the base image
        kc.getSpec().setImage(null);
        var updateCondition = assertUnknownUpdateTypeStatus(kc);
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        // use the optimized image, auto strategy should use a rolling update
        kc.getSpec().setImage(getTestCustomImage());
        updateCondition = switch (updateStrategy) {
            case AUTO -> eventuallyRollingUpdateStatus(k8sclient, kc, "Compatible changes detected.");
            case RECREATE_ON_IMAGE_CHANGE -> eventuallyRecreateUpdateStatus(k8sclient, kc, "Image changed");
            case EXPLICIT -> eventuallyRollingUpdateStatus(k8sclient, kc, "Explicit strategy configured. Revision matches.");
        };

        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        if (updateStrategy == UpdateStrategy.AUTO) {
            assertUpdateJobExists(kc);
        }
    }

    @DisabledIfApiServerTest
    @EnabledIfSystemProperty(named = OPERATOR_CUSTOM_IMAGE, matches = ".+")
    @Test
    public void testNoJobReuse() throws InterruptedException {
        // In GHA, the custom image is an optimized image of the base image.
        // We should be able to do a zero-downtime update with Auto strategy.
        var kc = createInitialDeployment(UpdateStrategy.AUTO);
        // use the base image
        kc.getSpec().setImage(null);
        var updateCondition = assertUnknownUpdateTypeStatus(kc);
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        // let's trigger a rolling update
        updateCondition = eventuallyRollingUpdateStatus(k8sclient, kc, "Compatible changes detected.");
        kc.getSpec().setImage(getTestCustomImage());

        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        var job = assertUpdateJobExists(kc);
        var hash = job.getMetadata().getAnnotations().get(KeycloakUpdateJobDependentResource.KEYCLOAK_CR_HASH_ANNOTATION);
        assertEquals(0, containerExitCode(job));

        //let's trigger a recreate
        updateCondition = eventuallyRecreateUpdateStatus(k8sclient, kc, "Unexpected update-compatibility command");
        // enough to crash the Pod and return exit code != 0
        kc.getSpec().setImage("quay.io/keycloak/non-existing-keycloak");
        disableProbes(kc);

        deployKeycloak(k8sclient, kc, false);
        await(updateCondition);

        job = assertUpdateJobExists(kc);
        var newHash = job.getMetadata().getAnnotations().get(KeycloakUpdateJobDependentResource.KEYCLOAK_CR_HASH_ANNOTATION);
        assertNotEquals(hash, newHash);
        assertNotEquals(0, containerExitCode(job));
    }

    @Test
    public void testExplicitStrategy() throws InterruptedException {
        var kc = createInitialDeployment(UpdateStrategy.EXPLICIT);
        disableProbes(kc);

        var updateCondition = assertUnknownUpdateTypeStatus(kc);
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        // update configuration, revision is updated
        updateCondition = eventuallyRecreateUpdateStatus(k8sclient, kc, "does not match");
        kc.getSpec().setAdditionalOptions(List.of(new ValueOrSecret("cache-embedded-authorization-max-count", "10")));
        kc.getSpec().getUpdateSpec().setRevision("1");
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        // update configuration, revision is updated
        updateCondition = eventuallyRecreateUpdateStatus(k8sclient, kc, "Explicit strategy configured. Revision (1) does not match (2).");
        kc.getSpec().setAdditionalOptions(List.of(new ValueOrSecret("cache-embedded-authorization-max-count", "11")));
        kc.getSpec().getUpdateSpec().setRevision("2");
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);

        // update configuration, revision is unchanged
        updateCondition = eventuallyRollingUpdateStatus(k8sclient, kc, "Explicit strategy configured. Revision matches.");
        kc.getSpec().setAdditionalOptions(List.of(new ValueOrSecret("cache-embedded-authorization-max-count", "12")));
        kc.getSpec().getUpdateSpec().setRevision("2");
        deployKeycloak(k8sclient, kc, true);
        await(updateCondition);
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

        // check label selector
        var jobPodName = AutoUpdateLogic.findPodForJob(k8sclient, job)
                .map(Pod::getMetadata)
                .map(ObjectMeta::getName)
                .orElseThrow();
        var servicePods = k8sclient.pods().inNamespace(namespaceOf(keycloak))
                .withLabels(Utils.allInstanceLabels(keycloak))
                .resources()
                .map(Gettable::get)
                .map(Pod::getMetadata)
                .map(ObjectMeta::getName)
                .toList();
        assertFalse(servicePods.contains(jobPodName), "pods: " + servicePods + " / job pod: " + jobPodName);
        assertEquals("test", keycloak.getSpec().getUpdateSpec().getLabels().get("example"));
        return job;
    }

    private int containerExitCode(Job job) {
        var maybeExitCode = AutoUpdateLogic.findPodForJob(k8sclient, job)
                .flatMap(AutoUpdateLogic::container)
                .map(AutoUpdateLogic::exitCode);
        assertTrue(maybeExitCode.isPresent());
        return maybeExitCode.get();
    }

    private CompletableFuture<Void> assertUnknownUpdateTypeStatus(Keycloak keycloak) {
        return k8sclient.resource(keycloak).informOnCondition(kcs -> {
            if (kcs.isEmpty() || kcs.get(0).getStatus() == null) {
                return false;
            }
            try {
                CRAssert.assertKeycloakStatusCondition(kcs.get(0), KeycloakStatusCondition.UPDATE_TYPE, null);
                return true;
            } catch (AssertionError e) {
                return false;
            }
        }).thenAccept(unused -> {});
    }

    private static Keycloak createInitialDeployment(UpdateStrategy updateStrategy) {
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().setInstances(2);
        var updateSpec = new UpdateSpec();
        updateSpec.setStrategy(updateStrategy);
        Map<String, String> labels = new java.util.HashMap<>(Map.of());
        labels.put("example", "test");
        updateSpec.setLabels(labels);
        if (updateStrategy == UpdateStrategy.EXPLICIT) {
            updateSpec.setRevision("0");
        }
        kc.getSpec().setUpdateSpec(updateSpec);
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
