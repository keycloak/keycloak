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
import io.sundr.builder.annotations.Buildable;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class IngressSpec {

    @JsonProperty("enabled")
    private boolean ingressEnabled = true;

    @JsonProperty("className")
    private String ingressClassName;

    @JsonProperty("annotations")
    @JsonPropertyDescription("Additional annotations to be appended to the Ingress object")
    Map<String, String> annotations;

    @JsonPropertyDescription("A secret containing the TLS configuration for re-encrypt or TLS termination scenarios. Reference: https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets.")
    private String tlsSecret;

    @JsonProperty("labels")
    @JsonPropertyDescription("Additional labels to be appended to the Ingress object")
    Map<String, String> labels = new LinkedHashMap<String, String>();

    public boolean isIngressEnabled() {
        return ingressEnabled;
    }

    public void setIngressEnabled(boolean enabled) {
        this.ingressEnabled = enabled;
    }

    public String getIngressClassName() {
        return ingressClassName;
    }

    public void setIngressClassName(String className) {
        this.ingressClassName = className;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

    public String getTlsSecret() {
        return tlsSecret;
    }

    public void setTlsSecret(String tlsSecret) {
        this.tlsSecret = tlsSecret;
    }
    public Map<String, String> getLabels() {
        return labels;
    }
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }
}
