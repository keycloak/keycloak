package org.keycloak.v1alpha1;

@io.fabric8.kubernetes.model.annotation.Version(value = "v1alpha1", storage = false, served = false)
@io.fabric8.kubernetes.model.annotation.Group("keycloak.org")
public class KeycloakRealm extends io.fabric8.kubernetes.client.CustomResource<KeycloakRealmSpec, KeycloakRealmStatus> implements io.fabric8.kubernetes.api.model.Namespaced {
}
