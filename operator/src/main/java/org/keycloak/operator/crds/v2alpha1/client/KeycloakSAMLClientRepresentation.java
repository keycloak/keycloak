package org.keycloak.operator.crds.v2alpha1.client;

import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", lazyCollectionInitEnabled = false)
public class KeycloakSAMLClientRepresentation extends SAMLClientRepresentation {

}
