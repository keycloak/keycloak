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

package org.keycloak.operator.update.impl;

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
import org.keycloak.operator.update.UpdateLogic;
import org.keycloak.operator.update.UpdateType;

/**
 * Common {@link UpdateLogic} implementation that checks if the update logic needs to be run.
 * <p>
 * The update logic can be skipped if it is the first deployment or if the change is not relevance (like, updating the
 * number of replicas or annotations).
 */
abstract class BaseUpdateLogic implements UpdateLogic {

    protected final Context<Keycloak> context;
    protected final Keycloak keycloak;
    private Consumer<KeycloakStatusAggregator> statusConsumer = KeycloakStatusAggregator::resetUpdateType;

    BaseUpdateLogic(Context<Keycloak> context, Keycloak keycloak) {
        this.context = context;
        this.keycloak = keycloak;
    }

    @Override
    public final Optional<UpdateControl<Keycloak>> decideUpdate() {
        var existing = ContextUtils.getCurrentStatefulSet(context);
        if (existing.isEmpty()) {
            // new deployment, no update needed
            Log.debug("New deployment - skipping update logic");
            return Optional.empty();
        }
        copyStatusFromExistStatefulSet(existing.get());

        Optional<String> storedHash = CRDUtils.getUpdateHash(existing.get());
        var desiredStatefulSet = ContextUtils.getDesiredStatefulSet(context);
        var desiredContainer = CRDUtils.firstContainerOf(desiredStatefulSet).orElseThrow(BaseUpdateLogic::containerNotFound);

        if (Objects.equals(CRDUtils.getUpdateHash(desiredStatefulSet).orElseThrow(), storedHash.orElse(null))) {
            Log.debug("Hash is equals - skipping update logic");
            return Optional.empty();
        }

        var actualContainer = CRDUtils.firstContainerOf(existing.get()).orElseThrow(BaseUpdateLogic::containerNotFound);

        if (isContainerEquals(actualContainer, desiredContainer)) {
            // container is equals, no update required
            Log.debug("No changes detected in the container - skipping update logic");
            return Optional.empty();
        }

        return onUpdate();
    }

    @Override
    public final void updateStatus(KeycloakStatusAggregator statusAggregator) {
        statusConsumer.accept(statusAggregator);
    }

    /**
     * Concrete update logic should be implemented here.
     * <p>
     * Use {@link ContextUtils#getCurrentStatefulSet(Context)} and/or
     * {@link ContextUtils#getDesiredStatefulSet(Context)} to get the current and the desired {@link StatefulSet},
     * respectively.
     * <p>
     * Use the methods {@link #decideRecreateUpdate(String)} or {@link #decideRollingUpdate(String)} to use one of the available
     * update logics.
     *
     * @return An {@link UpdateControl} if the reconciliation must be interrupted before updating the
     * {@link StatefulSet}.
     */
    abstract Optional<UpdateControl<Keycloak>> onUpdate();

    private void copyStatusFromExistStatefulSet(StatefulSet current) {
        var maybeRecreate = CRDUtils.fetchIsRecreateUpdate(current);
        if (maybeRecreate.isEmpty()) {
            return;
        }
        var reason = CRDUtils.findUpdateReason(current).orElseThrow();
        var recreate = maybeRecreate.get();
        statusConsumer = statusAggregator -> statusAggregator.addUpdateType(recreate, reason);
    }

    void decideRollingUpdate(String reason) {
        Log.debugf("Decided rolling update type. Reason: %s", reason);
        statusConsumer = status -> status.addUpdateType(false, reason);
        ContextUtils.storeUpdateType(context, UpdateType.ROLLING, reason);
    }

    void decideRecreateUpdate(String reason) {
        Log.debugf("Decided recreate update type. Reason: %s", reason);
        statusConsumer = status -> status.addUpdateType(true, reason);
        ContextUtils.storeUpdateType(context, UpdateType.RECREATE, reason);
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
                .filter(envVar -> !envVar.getName().equals(KeycloakDeploymentDependentResource.HOST_IP_SPI_OPTION))
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
