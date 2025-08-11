/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class Truststore {

    @JsonPropertyDescription("Not used. To be removed in later versions.")
    private String name;

    @JsonPropertyDescription("The Secret containing the trust material - only set one of the other secret or configMap")
    private TruststoreSource secret;
    @JsonPropertyDescription("The ConfigMap containing the trust material - only set one of the other secret or configMap")
    private TruststoreSource configMap;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TruststoreSource getSecret() {
        return secret;
    }

    public void setSecret(TruststoreSource secret) {
        this.secret = secret;
    }

    public TruststoreSource getConfigMap() {
        return configMap;
    }

    public void setConfigMap(TruststoreSource configMap) {
        this.configMap = configMap;
    }

}
