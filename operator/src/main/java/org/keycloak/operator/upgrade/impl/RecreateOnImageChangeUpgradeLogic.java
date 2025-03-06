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

import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.upgrade.UpgradeType;

/**
 * Implements Keycloak 26.0 logic.
 * <p>
 * It uses a {@link UpgradeType#RECREATE} if the image changes; otherwise uses {@link UpgradeType#ROLLING}.
 */
public class RecreateOnImageChangeUpgradeLogic extends BaseUpgradeLogic {

    public RecreateOnImageChangeUpgradeLogic(Context<Keycloak> context, Keycloak keycloak) {
        super(context, keycloak);
    }

    @Override
    Optional<UpdateControl<Keycloak>> onUpgrade() {
        var currentImage = extractImage(ContextUtils.getCurrentStatefulSet(context).orElseThrow());
        var desiredImage = extractImage(ContextUtils.getDesiredStatefulSet(context));

        if (Objects.equals(currentImage, desiredImage)) {
            decideRollingUpgrade("Image unchanged.");
        } else {
            decideRecreateUpgrade("Image changed %s -> %s".formatted(currentImage, desiredImage));
        }
        return Optional.empty();
    }

    private static String extractImage(StatefulSet statefulSet) {
        return CRDUtils.firstContainerOf(statefulSet)
                .map(Container::getImage)
                .orElseThrow(BaseUpgradeLogic::containerNotFound);
    }

}
