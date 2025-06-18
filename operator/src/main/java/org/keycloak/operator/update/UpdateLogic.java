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

package org.keycloak.operator.update;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;

/**
 * An API to implement to handle Keycloak CR updates.
 * <p>
 * This interface is invoked all the time before creating the {@link StatefulSet} and it can manipulate the
 * reconciliation to perform or check other tasks required before the {@link StatefulSet} is created or updated.
 */
public interface UpdateLogic {

    /**
     * It must check is an existing {@link StatefulSet} exists and decided on the {@link UpdateType} to update the
     * {@link StatefulSet}.
     * <p>
     * The method should use {@link org.keycloak.operator.ContextUtils#storeUpdateType(Context, UpdateType, String)} to store
     * its decision. If no prior {@link StatefulSet} is present, no decision is required and
     * {@link org.keycloak.operator.ContextUtils#storeUpdateType(Context, UpdateType, String)} must not be invoked.
     * <p>
     * Return a non-empty {@link Optional} to interrupt the reconciliation until the next event. The interrupted
     * prevents the {@link StatefulSet} from being updated.
     *
     * @return The {@link UpdateControl} if the reconciliation needs to be interrupted or an empty {@link Optional} if
     * it can proceed.
     */
    Optional<UpdateControl<Keycloak>> decideUpdate();

    /**
     * Updates the Keycloak CR status.
     *
     * @param statusAggregator The {@link KeycloakStatusAggregator} to update.
     */
    void updateStatus(KeycloakStatusAggregator statusAggregator);

}
