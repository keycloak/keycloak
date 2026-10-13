package org.keycloak.operator.testsuite.apiserver;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.operator.Utils;

import io.fabric8.kubeapitest.KubeAPIServer;
import io.fabric8.kubeapitest.KubeAPIServerConfigBuilder;
import io.fabric8.kubeapitest.KubeAPITestException;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.logging.Log;

public class ApiServerHelper {

    static final int MAX_START_RETRIES = 2;

    private KubeAPIServer kubeApi;

    public ApiServerHelper() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < MAX_START_RETRIES; i++) {
            kubeApi = new KubeAPIServer(
                    KubeAPIServerConfigBuilder.anAPIServerConfig().withStartupTimeout(90_000).build());
            try {
                kubeApi.start();
            } catch (KubeAPITestException e) {
                if (i == MAX_START_RETRIES - 1) {
                    throw e;
                }
                kubeApi.stop();
                Log.warnf("api server failed to become ready %s", e.getMessage());
            }
        }
        Log.infof("api server started in %s ms", System.currentTimeMillis() - start);
    }

    public void stop() {
        kubeApi.stop();
    }

    public KubernetesClient createClient(String namespace) {
        Config config = Config.fromKubeconfig(kubeApi.getKubeConfigYaml());
        config.setNamespace(namespace);
        var result = new KubernetesClientBuilder().withConfig(config).build();

        // fake statefulset controller - we don't have to worry about closing this
        // that will happen automatically when the client is closed
        result.apps().statefulSets().inAnyNamespace().inform(new ResourceEventHandler<StatefulSet>() {

            @Override
            public void onAdd(StatefulSet obj) {
                updateStatefulSet(obj);
            }

            private void updateStatefulSet(StatefulSet obj) {
                int specReplicas = obj.getSpec().getReplicas();
                int statusReplicas = Optional.ofNullable(obj.getStatus().getReplicas()).orElse(0);
                String revision = Utils.hash(List.of(obj.getSpec())).substring(1);
                String updateRevision = obj.getStatus().getUpdateRevision();

                if (statusReplicas != specReplicas
                        || !Objects.equals(revision, updateRevision)
                        || !Objects.equals(obj.getStatus().getCurrentRevision(), updateRevision)) {
                    // generate intermediate rolling events
                    int actualReplicas;
                    if (!Objects.equals(revision, updateRevision)) { // detected spec change, mimic rolling update
                        // this is not fully accurate as it's rather recreate than rolling update,
                        // but thanks to gradual scaling up it emits more events which is closer to the real behavior
                        actualReplicas = 0;
                    } else if (specReplicas == 0) { // probably recreate update requested, scaling down the deployment
                        actualReplicas = Math.max(0, statusReplicas - 1);
                    } else {
                        actualReplicas = Math.min(specReplicas, statusReplicas + 1); // otherwise, just scale up
                    }
                    obj = result.getKubernetesSerialization().clone(obj);
                    obj.getStatus().setReplicas(actualReplicas);
                    obj.getStatus().setReadyReplicas(actualReplicas);
                    obj.getStatus().setUpdateRevision(revision);
                    if (actualReplicas == specReplicas) {
                        obj.getStatus().setCurrentRevision(revision);
                    }
                    obj.getMetadata().setResourceVersion(null);
                    result.resource(obj).updateStatus();
                }
            }

            @Override
            public void onUpdate(StatefulSet oldObj, StatefulSet newObj) {
                updateStatefulSet(newObj);
            }

            @Override
            public void onDelete(StatefulSet obj, boolean deletedFinalStateUnknown) {

            }

        });

        return result;
    }

}
