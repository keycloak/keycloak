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

package org.keycloak.operator.upgrade;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.keycloak.operator.controllers.KeycloakUpdateJobDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UpdateSpec;
import org.keycloak.operator.upgrade.impl.ForceRecreateUpgradeLogic;
import org.keycloak.operator.upgrade.impl.AutoUpgradeLogic;
import org.keycloak.operator.upgrade.impl.RecreateOnImageChangeUpgradeLogic;

/**
 * The {@link UpgradeLogic} factory. It returns an implementation based on the {@link Keycloak} configuration.
 */
@ApplicationScoped
public class UpgradeLogicFactory {
    @Inject
    KeycloakUpdateJobDependentResource updateJobDependentResource;

    public UpgradeLogic create(Keycloak keycloak, Context<Keycloak> context) {
        var strategy = UpdateSpec.getUpdateStrategy(keycloak);
        return switch (strategy) {
            case RECREATE_ON_IMAGE_CHANGE -> new RecreateOnImageChangeUpgradeLogic(context, keycloak);
            case FORCE_RECREATE -> new ForceRecreateUpgradeLogic(context, keycloak);
            case AUTO -> new AutoUpgradeLogic(context, keycloak, updateJobDependentResource);
        };
    }

}
