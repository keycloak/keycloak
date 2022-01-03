/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.operator;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import org.jboss.logging.Logger;
import org.keycloak.operator.crds.Keycloak;
import org.keycloak.operator.crds.KeycloakStatus;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE, finalizerName = Constants.NO_FINALIZER)
public class KeycloakController implements Reconciler<Keycloak> {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient client;

    @Override
    public UpdateControl<Keycloak> reconcile(Keycloak kc, Context context) {
        logger.trace("Reconcile loop started");
        final var spec = kc.getSpec();

        logger.info("Reconciling Keycloak: " + kc.getMetadata().getName() + " in namespace: " + kc.getMetadata().getNamespace());

        KeycloakStatus status = kc.getStatus();
        var deployment = new KeycloakDeployment(client);

        try {
            var kcDeployment = deployment.getKeycloakDeployment(kc);

            if (kcDeployment == null) {
                // Need to create the deployment
                deployment.createKeycloakDeployment(kc);
            }

            var nextStatus = deployment.getNextStatus(spec, status, kcDeployment);

            if (!nextStatus.equals(status)) {
                logger.trace("Updating the status");
                kc.setStatus(nextStatus);
                return UpdateControl.updateStatus(kc);
            } else {
                logger.trace("Nothing to do");
                return UpdateControl.noUpdate();
            }
        } catch (Exception e) {
            logger.error("Error reconciling", e);
            status = new KeycloakStatus();
            status.setMessage("Error performing operations:\n" + e.getMessage());
            status.setState(KeycloakStatus.State.ERROR);
            status.setError(true);

            kc.setStatus(status);
            return UpdateControl.updateStatus(kc);
        }
    }
}
