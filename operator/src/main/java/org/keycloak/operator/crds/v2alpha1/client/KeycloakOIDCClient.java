/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.crds.v2alpha1.client;

import org.keycloak.operator.Constants;

import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@CSVMetadata(
        description="Represents a Keycloak OIDC Client",
        displayName="KeycloakOIDCClient"
    )
    @Group(Constants.CRDS_GROUP)
    @Version(Constants.CRDS_VERSION)
    @Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder",
            lazyCollectionInitEnabled = false, refs = {
            @BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),
            @BuildableReference(io.fabric8.kubernetes.client.CustomResource.class),
    })
public class KeycloakOIDCClient extends CustomResource<KeycloakOIDCClientSpec, KeycloakClientStatus> implements Namespaced {

    @Required
    @Override
    public KeycloakOIDCClientSpec getSpec() {
        return super.getSpec();
    }

}
