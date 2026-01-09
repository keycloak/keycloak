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
package org.keycloak.operator.controllers;

import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClient;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClientRepresentation;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClientRepresentation.AuthWithSecretRef;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClientRepresentationBuilder;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.client.ResourceNotFoundException;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;

@ControllerConfiguration
public class KeycloakOIDCClientController extends KeycloakClientBaseController<KeycloakOIDCClient, KeycloakOIDCClientRepresentation> {

    @Override
    Representation<KeycloakOIDCClientRepresentation> prepareRepresentation(
            KeycloakOIDCClientRepresentation representation, Context<?> context) {
        boolean poll = false;
        // create the payload via inlining of the secret
        KeycloakOIDCClientRepresentation rep = new KeycloakOIDCClientRepresentationBuilder(representation).build();
        AuthWithSecretRef auth = representation.getAuth();
        if (auth != null) {
            rep.getAuth().setSecretRef(null); // remove operator specific fields
            SecretKeySelector secretSelector = auth.getSecretRef();
            if (secretSelector != null) {
                poll = true;

                boolean optional = Boolean.TRUE.equals(secretSelector.getOptional());

                String namespace = context.getPrimaryResource().getMetadata().getNamespace();
                Secret secret = context.getClient().resources(Secret.class)
                        .inNamespace(namespace).withName(secretSelector.getName()).get();

                if (secret == null) {
                    if (!optional) {
                        throw new ResourceNotFoundException(String.format("Secret %s/%s not found", namespace, secretSelector.getName()));
                    }
                } else {
                    String value = secret.getData().get(secretSelector.getKey());

                    if (value == null) {
                        if (!optional) {
                            throw new ResourceNotFoundException(String.format("Secret key %s in %s/%s not found", secretSelector.getKey(), namespace, secretSelector.getName()));
                        }
                    } else {
                        rep.getAuth().setSecret(value);
                    }
                }
            }
        }
        return new Representation<KeycloakOIDCClientRepresentation>(rep, poll);
    }

}
