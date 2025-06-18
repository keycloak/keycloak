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
package org.keycloak.operator.crds.v2alpha1.deployment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.fabric8.kubernetes.model.annotation.LabelSelector;
import io.fabric8.kubernetes.model.annotation.StatusReplicas;
import io.sundr.builder.annotations.Buildable;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", lazyCollectionInitEnabled = false)
public class KeycloakStatus {

    @LabelSelector
    private String selector;
    @StatusReplicas
    private Integer instances;
    private Long observedGeneration;

    private List<KeycloakStatusCondition> conditions;

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public Integer getInstances() {
        return instances;
    }

    public void setInstances(Integer instances) {
        this.instances = instances;
    }

    public List<KeycloakStatusCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<KeycloakStatusCondition> conditions) {
        this.conditions = conditions;
    }

    public Optional<KeycloakStatusCondition> findCondition(String type) {
        if (conditions == null || conditions.isEmpty()) {
            return Optional.empty();
        }
        return conditions.stream().filter(c -> type.equals(c.getType())).findFirst();
    }

    @JsonIgnore
    public boolean isReady() {
        return findCondition(KeycloakStatusCondition.READY).map(KeycloakStatusCondition::getStatus).orElse(false);
    }

    public Long getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(Long generation) {
        this.observedGeneration = generation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeycloakStatus status = (KeycloakStatus) o;
        return Objects.equals(getConditions(), status.getConditions())
                && Objects.equals(getInstances(), status.getInstances())
                && Objects.equals(getSelector(), status.getSelector())
                && Objects.equals(getObservedGeneration(), status.getObservedGeneration());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConditions(), getInstances(), getSelector(), getObservedGeneration());
    }
}
