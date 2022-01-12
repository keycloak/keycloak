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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import org.jboss.logging.Logger;
import org.keycloak.operator.v2alpha1.crds.realm.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.operator.Constants.*;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE, finalizerName = Constants.NO_FINALIZER)
public class RealmController implements Reconciler<Realm>, ErrorStatusHandler<Realm>, EventSourceInitializer<Realm> {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient client;

    @Inject
    ObjectMapper jsonMapper;

    @Override
    public UpdateControl<Realm> reconcile(Realm realm, Context context) {
        logger.trace("Realm Importer - Reconcile loop started");
        var kcDeployment = new KeycloakDeployment(client);

        // Get the deployment from the cluster
        var kc = kcDeployment.getKeycloakDeployment(
                realm.getSpec().getKeycloakCRName(),
                realm.getMetadata().getNamespace()
        );

        // If the deployment doesn't exists set error and exit
        if (kc == null) {
            realm.setStatus(newErrorStatus("Keycloak Deployment doesn't exists"));
            return UpdateControl.updateStatus(realm);
        }

        // Compute the CR OwnerReferences
        var ownerReferences = getOwnerReferences(realm);

        // Fixed Secret Name
        var secretName = RealmSecret.getSecretName(kc, realm);

        // Create or update the relevant Secret
        var realmSecret = new RealmSecret(client, jsonMapper);
        realmSecret.handleRealmSecret(secretName, realm, ownerReferences);

        // Run the import job and get the result
        var realmJob = new RealmJob(client);
        var nextStatus = realmJob
                .handleImportJob(
                        secretName,
                        realm.getSpec().getRealm().getRealm(),
                        realm.getMetadata().getName(),
                        kc,
                        ownerReferences
                );

        if (realm.getStatus() != nextStatus) {
            // Transition to DONE
            if (nextStatus.getState() == RealmStatus.State.DONE) {
                // Perform a rolling restart of the Deployment to invalidate caches
                client
                    .apps()
                    .deployments()
                    .inNamespace(kc.getMetadata().getNamespace())
                    .withName(kc.getMetadata().getName())
                    .rolling()
                    .restart();
            }

            realm.setStatus(nextStatus);
            return UpdateControl.updateStatus(realm);
        } else {
            return UpdateControl.noUpdate();
        }
    }

    private List<OwnerReference> getOwnerReferences(Realm realm) {
        return List.of(
            new OwnerReferenceBuilder()
                .withController(true)
                .withBlockOwnerDeletion(true)
                .withApiVersion(realm.getApiVersion())
                .withKind(realm.getKind())
                .withName(realm.getMetadata().getName())
                .withUid(realm.getMetadata().getUid())
                .build());
    }

    private RealmStatus newErrorStatus(String message) {
        var status = new RealmStatus();
        status.setError(true);
        status.setState(RealmStatus.State.ERROR);
        status.setMessage(message);
        return status;
    }

    @Override
    public DeleteControl cleanup(Realm realm, Context context) {
        // We are using OwnerReferences and all the resources are reclaimed when the CR is deleted
        return DeleteControl.defaultDelete();
    }

    @Override
    public Optional<Realm> updateErrorStatus(Realm realm, RetryInfo retryInfo, RuntimeException e) {
        var status = realm.getStatus();
        if (status == null) {
            status = new RealmStatus();
        }
        status.setState(RealmStatus.State.ERROR);
        status.setError(true);
        status.setMessage("Error: " + e.getMessage());
        return Optional.of(realm);
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<Realm> context) {
        return List.of(new InformerEventSource<>(
                client, Job.class, job -> {
                    if (job.getMetadata().getLabels() != null &&
                            job.getMetadata().getLabels().containsKey(PART_OF_LABEL)) {
                        return context.getPrimaryCache()
                                .list(realm -> realm.getMetadata().getName().equals(job.getMetadata().getLabels().get(PART_OF_LABEL)))
                                .map(ResourceID::fromResource)
                                .collect(Collectors.toSet());
                    } else {
                        return Set.of();
                    }
        },
                (Realm realm) -> new ResourceID(realm.getCRDName(),
                        realm.getMetadata().getNamespace()),
                true));
    }
}
