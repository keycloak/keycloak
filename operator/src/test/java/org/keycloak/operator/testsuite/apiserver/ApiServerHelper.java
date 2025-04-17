package org.keycloak.operator.testsuite.apiserver;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.operator.Utils;

import io.fabric8.kubeapitest.KubeAPIServer;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;

public class ApiServerHelper {

    private KubeAPIServer kubeApi;

    public ApiServerHelper() {
        kubeApi = new KubeAPIServer();
        kubeApi.start();
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
                int replicas = obj.getSpec().getReplicas();
                String revision = Utils.hash(List.of(obj.getSpec())).substring(1);
                if (!Objects.equals(Optional.ofNullable(obj.getStatus().getReplicas()).orElse(0), replicas)
                        || !Objects.equals(revision, obj.getStatus().getUpdateRevision())
                        || !Objects.equals(obj.getStatus().getCurrentRevision(), obj.getStatus().getUpdateRevision())) {
                    // generate intermediate rolling events
                    int actualReplicas;
                    if (!Objects.equals(revision, obj.getStatus().getUpdateRevision())) {
                        actualReplicas = 0;
                    } else if (replicas == 0) {
                        actualReplicas = Math.max(replicas, Optional.ofNullable(obj.getStatus().getReplicas()).orElse(0) - 1);
                    } else {
                        actualReplicas = Math.min(replicas, Optional.ofNullable(obj.getStatus().getReplicas()).orElse(0) + 1);
                    }
                    obj = result.getKubernetesSerialization().clone(obj);
                    obj.getStatus().setReplicas(actualReplicas);
                    obj.getStatus().setReadyReplicas(actualReplicas);
                    obj.getStatus().setUpdateRevision(revision);
                    if (actualReplicas == replicas) {
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
