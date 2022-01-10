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
package org.keycloak.operator.v2alpha1;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import org.jboss.logging.Logger;
import org.keycloak.operator.v2alpha1.crds.realm.*;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE, finalizerName = Constants.NO_FINALIZER)
public class RealmImporterController implements Reconciler<RealmImporter> {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient client;

    @Override
    public UpdateControl<RealmImporter> reconcile(RealmImporter realm, Context context) {
        logger.trace("Reconcile loop started - Realm Importer!");
        return UpdateControl.noUpdate();
    }
}
