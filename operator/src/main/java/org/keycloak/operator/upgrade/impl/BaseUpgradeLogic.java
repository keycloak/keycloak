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

package org.keycloak.operator.upgrade.impl;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.logging.Log;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.controllers.KeycloakDeploymentDependentResource;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.upgrade.UpgradeLogic;
import org.keycloak.operator.upgrade.UpgradeType;

/**
 * Common {@link UpgradeLogic} implementation that checks if the upgrade logic needs to be run.
 * <p>
 * The upgrade logic can be skipped if it is the first deployment or if the change is not relevance (like, updating the
 * number of replicas or annotations).
 */
abstract class BaseUpgradeLogic implements UpgradeLogic {

    protected final Context<Keycloak> context;
    protected final Keycloak keycloak;
    private Consumer<KeycloakStatusAggregator> statusConsumer = unused -> {};

    BaseUpgradeLogic(Context<Keycloak> context, Keycloak keycloak) {
        this.context = context;
        this.keycloak = keycloak;
    }

    @Override
    public final Optional<UpdateControl<Keycloak>> decideUpgrade() {
        var existing = ContextUtils.getCurrentStatefulSet(context);
        if (existing.isEmpty()) {
            // new deployment, no upgrade needed
            Log.debug("New deployment - skipping upgrade logic");
            return Optional.empty();
        }
        var desiredStatefulSet = ContextUtils.getDesiredStatefulSet(context);
        var desiredContainer = CRDUtils.firstContainerOf(desiredStatefulSet).orElseThrow(BaseUpgradeLogic::containerNotFound);
        var actualContainer = CRDUtils.firstContainerOf(existing.get()).orElseThrow(BaseUpgradeLogic::containerNotFound);

        if (isContainerEquals(actualContainer, desiredContainer)) {
            // container is equals, no upgrade required
            Log.debug("No changes detected in the container - skipping upgrade logic");
            return Optional.empty();
        }

        return onUpgrade();
    }

    @Override
    public final void updateStatus(KeycloakStatusAggregator statusAggregator) {
        statusConsumer.accept(statusAggregator);
    }

    /**
     * Concrete upgrade logic should be implemented here.
     * <p>
     * Use {@link ContextUtils#getCurrentStatefulSet(Context)} and/or
     * {@link ContextUtils#getDesiredStatefulSet(Context)} to get the current and the desired {@link StatefulSet},
     * respectively.
     * <p>
     * Use the methods {@link #decideRecreateUpgrade(String)} or {@link #decideRollingUpgrade(String)} to use one of the available
     * upgrade logics.
     *
     * @return An {@link UpdateControl} if the reconciliation must be interrupted before updating the
     * {@link StatefulSet}.
     */
    abstract Optional<UpdateControl<Keycloak>> onUpgrade();

    void decideRollingUpgrade(String reason) {
        Log.debugf("Decided rolling upgrade type. Reason: %s", reason);
        statusConsumer = status -> status.addUpgradeType(false, reason);
        ContextUtils.storeUpgradeType(context, UpgradeType.ROLLING);
    }

    void decideRecreateUpgrade(String reason) {
        Log.debugf("Decided recreate upgrade type. Reason: %s", reason);
        statusConsumer = status -> status.addUpgradeType(true, reason);
        ContextUtils.storeUpgradeType(context, UpgradeType.RECREATE);
    }

    static IllegalStateException containerNotFound() {
        return new IllegalStateException("Container not found in stateful set.");
    }

    private static boolean isContainerEquals(Container actual, Container desired) {
        return isImageEquals(actual, desired) &&
                isArgsEquals(actual, desired) &&
                isEnvEquals(actual, desired);
    }

    private static boolean isImageEquals(Container actual, Container desired) {
        return isEquals("image", actual.getImage(), desired.getImage());
    }

    private static boolean isArgsEquals(Container actual, Container desired) {
        return isEquals("args", actual.getArgs(), desired.getArgs());
    }

    private static boolean isEnvEquals(Container actual, Container desired) {
        var actualEnv = envVars(actual);
        var desiredEnv = envVars(desired);
        return isEquals("env", actualEnv, desiredEnv);
    }

    private static Map<String, EnvVar> envVars(Container container) {
        // The operator only sets value or secrets. Any other combination is from unsupported pod template.
        return container.getEnv().stream()
                .filter(envVar -> !envVar.getName().equals(KeycloakDeploymentDependentResource.POD_IP))
                .collect(Collectors.toMap(EnvVar::getName, Function.identity()));
    }

    private static <T> boolean isEquals(String key, T actual, T desired) {
        var isEquals = Objects.equals(actual, desired);
        if (!isEquals) {
            Log.debugf("Found difference in container's %s:%nactual:%s%ndesired:%s", key, actual, desired);
        }
        return isEquals;
    }
}
