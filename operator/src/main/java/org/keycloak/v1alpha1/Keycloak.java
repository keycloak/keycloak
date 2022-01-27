package org.keycloak.v1alpha1;

@io.fabric8.kubernetes.model.annotation.Version(value = "v1alpha1", storage = false, served = false)
@io.fabric8.kubernetes.model.annotation.Group("keycloak.org")
public class Keycloak extends io.fabric8.kubernetes.client.CustomResource<KeycloakSpec, KeycloakStatus> implements io.fabric8.kubernetes.api.model.Namespaced {
}
