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

import java.util.Optional;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.keycloak.operator.controllers.KeycloakDeploymentDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.upgrade.UpgradeLogic;
import org.keycloak.operator.upgrade.UpgradeType;

/**
 * An {@link UpgradeLogic} implementation that forces a {@link UpgradeType#RECREATE} on every configuration or image
 * change.
 */
public class AlwaysRecreateUpgradeLogic extends BaseUpgradeLogic {

    public AlwaysRecreateUpgradeLogic(Context<Keycloak> context, Keycloak keycloak, KeycloakDeploymentDependentResource statefulSetResource) {
        super(context, keycloak, statefulSetResource);
    }

    @Override
    Optional<UpdateControl<Keycloak>> onUpgrade() {
        decideRecreateUpgrade();
        return Optional.empty();
    }
}
