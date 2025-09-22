package org.keycloak.broker.kubernetes;

public interface KubernetesConstants {

    String KUBERNETES_SERVICE_HOST_KEY = "KUBERNETES_SERVICE_HOST";
    String KUBERNETES_SERVICE_PORT_HTTPS_KEY = "KUBERNETES_SERVICE_PORT_HTTPS";
    String SERVICE_ACCOUNT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";

}
