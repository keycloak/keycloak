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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", lazyCollectionInitEnabled = false)
public class KeycloakClientStatus {
    
    private Long observedGeneration;
    
    private String hash;

    private List<KeycloakClientStatusCondition> conditions = new ArrayList<KeycloakClientStatusCondition>();
    
    // TODO: will the id or anything else be generated such that it needs to be in the status
    
    public Long getObservedGeneration() {
        return observedGeneration;
    }
    
    public void setObservedGeneration(Long observedGeneration) {
        this.observedGeneration = observedGeneration;
    }
    
    public List<KeycloakClientStatusCondition> getConditions() {
        return conditions;
    }
    
    public void setConditions(List<KeycloakClientStatusCondition> conditions) {
        this.conditions = conditions;
    }
    
    public String getHash() {
        return hash;
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeycloakClientStatus status = (KeycloakClientStatus) o;
        return Objects.equals(getConditions(), status.getConditions())
                && Objects.equals(getHash(), status.getHash())
                && Objects.equals(getObservedGeneration(), status.getObservedGeneration());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConditions(), getHash(), getObservedGeneration());
    }
}
