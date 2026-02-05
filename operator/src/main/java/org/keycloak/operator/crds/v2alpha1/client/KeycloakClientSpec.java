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

import org.keycloak.representations.admin.v2.BaseClientRepresentation;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.generator.annotation.Required;
import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", lazyCollectionInitEnabled = false)
public class KeycloakClientSpec<T extends BaseClientRepresentation> {

    @Required
    @JsonPropertyDescription("The name of the Keycloak CR to reference, in the same namespace.")
    @ValidationRule(value = "self == oldSelf", message = "keycloakCrName is immutable")
    private String keycloakCRName;

    @Required
    @JsonPropertyDescription("The realm of the Client")
    @ValidationRule(value = "self == oldSelf", message = "realm is immutable")
    private String realm;

    @Required
    private T client;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public T getClient() {
        return client;
    }

    public void setClient(T client) {
        this.client = client;
    }

    public String getKeycloakCRName() {
        return keycloakCRName;
    }

    public void setKeycloakCRName(String keycloakCRName) {
        this.keycloakCRName = keycloakCRName;
    }

}
