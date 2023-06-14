/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonPropertyOrder({"enabled", "disabled"})
public class FeatureSpec implements Serializable {

    @JsonProperty("enabled")
    @JsonPropertyDescription("Enabled Keycloak features")
    private List<String> enabledFeatures;

    @JsonProperty("disabled")
    @JsonPropertyDescription("Disabled Keycloak features")
    private List<String> disabledFeatures;

    public List<String> getEnabledFeatures() {
        return enabledFeatures;
    }

    public void setEnabledFeatures(List<String> enabledFeatures) {
        this.enabledFeatures = enabledFeatures;
    }

    public List<String> getDisabledFeatures() {
        return disabledFeatures;
    }

    public void setDisabledFeatures(List<String> disabledFeatures) {
        this.disabledFeatures = disabledFeatures;
    }
}