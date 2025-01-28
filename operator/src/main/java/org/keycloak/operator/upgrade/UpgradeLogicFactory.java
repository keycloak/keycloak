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

import java.util.Collection;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import jakarta.enterprise.context.ApplicationScoped;
import org.keycloak.common.Profile;
import org.keycloak.operator.controllers.KeycloakDeploymentDependentResource;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UpdateSpec;
import org.keycloak.operator.upgrade.impl.AlwaysRecreateUpgradeLogic;
import org.keycloak.operator.upgrade.impl.RecreateOnImageChangeUpgradeLogic;

/**
 * The {@link UpgradeLogic} factory. It returns an implementation based on the {@link Keycloak} configuration.
 */
@ApplicationScoped
public class UpgradeLogicFactory {

    @SuppressWarnings("removal")
    public UpgradeLogic create(Keycloak keycloak, Context<Keycloak> context, KeycloakDeploymentDependentResource dependentResource) {
        var strategy = UpdateSpec.findUpdateStrategy(keycloak);
        if (strategy.isEmpty()) {
            return new RecreateOnImageChangeUpgradeLogic(context, keycloak, dependentResource);
        }
        assertFeatureEnabled(keycloak, strategy.get());
        return switch (strategy.get()) {
            case RECREATE -> new AlwaysRecreateUpgradeLogic(context, keycloak, dependentResource);
        };
    }

    private static void assertFeatureEnabled(Keycloak keycloak, UpdateStrategy updateStrategy) {
        var rollingUpdatesEnabled = CRDUtils.keycloakSpecOf(keycloak)
                .map(KeycloakSpec::getFeatureSpec)
                .map(FeatureSpec::getEnabledFeatures)
                .stream()
                .flatMap(Collection::stream)
                .anyMatch(Profile.Feature.ROLLING_UPDATES.getKey()::equals);
        if (rollingUpdatesEnabled) {
            return;
        }
        // Is it safe to throw an exception? Or should return RecreateOnImageChangeUpgradeLogic?
        throw new IllegalArgumentException("Unable to use update strategy %s. The preview feature '%s' is not enabled.".formatted(updateStrategy, Profile.Feature.ROLLING_UPDATES.getKey()));
    }

}
