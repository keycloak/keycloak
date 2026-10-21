package org.keycloak.broker.kubernetes;

public interface KubernetesConstants {

    String DEFAULT_KUBERNETES_ISSUER_URL = "https://kubernetes.default.svc.cluster.local";
    String DEFAULT_KUBERNETES_API_SERVER_URL = "https://kubernetes.default.svc";
    String SERVICE_ACCOUNT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";

}
