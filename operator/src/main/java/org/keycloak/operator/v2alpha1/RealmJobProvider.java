package org.keycloak.operator.v2alpha1;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.jboss.logging.Logger;
import org.keycloak.operator.v2alpha1.crds.realm.RealmStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.keycloak.operator.Constants.*;

public class RealmJobProvider {

    private final static Logger logger = Logger.getLogger(RealmJobProvider.class);

    private KubernetesClient client;

    private Consumer<Supplier<Job>> createJobFn = (jb) -> {
        var job = jb.get();
        client
                .batch()
                .v1()
                .jobs()
                .create(job);
    };


    public RealmJobProvider(KubernetesClient client) {
        this.client = client;
    }

    private Job buildJob(String name, String namespace, String realmCRName, Container keycloakContainer, Volume secretVolume, List<OwnerReference> ownerReferences) {
        return new JobBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(PART_OF_LABEL, realmCRName)
                .addToLabels(MANAGED_BY_LABEL, MANAGED_BY_VALUE)
                .withOwnerReferences(ownerReferences)
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .withContainers(keycloakContainer)
                .addToVolumes(secretVolume)
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private Volume buildSecretVolume(String volumeName, String secretName) {
        return new VolumeBuilder()
                .withName(volumeName)
                .withSecret(new SecretVolumeSourceBuilder()
                        .withSecretName(secretName)
                        .build())
                .build();
    }

    private Container buildKeycloakJobContainer(PodTemplateSpec spec, String realmName, String volumeName) {
        var keycloakContainer = spec.getSpec().getContainers().get(0);

        var importMntPath = "/mnt/realm-import/";

        var dbOptions = keycloakContainer
                .getArgs()
                .stream()
                .filter((p) -> p.startsWith("--db"))
                .collect(Collectors.toList());

        var command = new ArrayList<String>();
        command.add("/bin/bash");

        var commandArgs = new ArrayList<String>();
        var dbOptsString = dbOptions.stream().reduce("", (o, n) -> o + " " + n);
        commandArgs.add("-c");
        commandArgs.add("/opt/keycloak/bin/kc.sh build" + dbOptsString + " && " +
                "/opt/keycloak/bin/kc.sh import --file='" + importMntPath + realmName + "-realm.json' --override=true");

        keycloakContainer
                .setCommand(command);
        keycloakContainer
                .setArgs(commandArgs);
        var volumeMounts = new ArrayList<VolumeMount>();
        volumeMounts.add(
                new VolumeMountBuilder()
                        .withName(volumeName)
                        .withReadOnly(true)
                        .withMountPath(importMntPath)
                        .build()
        );
        keycloakContainer.setVolumeMounts(volumeMounts);

        return keycloakContainer;
    }

    private RealmStatus handleStatus(String name, String namespace, Supplier<Job> jobBuilder) {
        var prevJob = client
                .batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName(name)
                .get();

        if (prevJob != null) {
            logger.info("Job already executed - not recreating");
            var oldStatus = prevJob.getStatus();
            var newStatus = new RealmStatus();

            if (oldStatus != null) {
                if (oldStatus.getSucceeded() != null && oldStatus.getSucceeded() >= 0) {
                    newStatus.setState(RealmStatus.State.DONE);
                } else if (oldStatus.getFailed() != null && oldStatus.getFailed() >= 0) {
                    newStatus.setError(true);
                    newStatus.setState(RealmStatus.State.ERROR);
                } else {
                    newStatus.setState(RealmStatus.State.STARTED);
                }
            } else {
                newStatus.setState(RealmStatus.State.UNKNOWN);
            }
            return newStatus;
        } else {
            logger.info("Creating a new Job");
            createJobFn.accept(jobBuilder);

            var status = new RealmStatus();
            status.setState(RealmStatus.State.STARTED);
            return status;
        }
    }

    public RealmStatus handleImportJob(String secretName, String realmName, String realmCRName, Deployment deployment, List<OwnerReference> ownerReferences) {
        var name = KubernetesResourceUtil.sanitizeName("realm-" + realmName + "-importer");
        var namespace = deployment.getMetadata().getNamespace();
        var volumeName = KubernetesResourceUtil.sanitizeName(secretName + "-volume");
        var keycloakTemplate = deployment.getSpec().getTemplate();

        var keycloakContainer =
                buildKeycloakJobContainer(keycloakTemplate, realmName, volumeName);

        Supplier<Job> jobBuilder = () -> buildJob(
                name,
                namespace,
                realmCRName,
                keycloakContainer,
                buildSecretVolume(volumeName, secretName),
                ownerReferences
        );

        return handleStatus(name, namespace, jobBuilder);
    }

}
