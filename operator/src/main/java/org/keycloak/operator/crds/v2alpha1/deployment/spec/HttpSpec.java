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

import java.util.Optional;
import java.util.Map;

import org.keycloak.operator.Constants;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class HttpSpec {
    @JsonPropertyDescription("A secret containing the TLS configuration for HTTPS. Reference: https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets.")
    private String tlsSecret;

    @JsonPropertyDescription("Enables the HTTP listener.")
    private Boolean httpEnabled;

    @JsonPropertyDescription("The used HTTP port.")
    private Integer httpPort = Constants.KEYCLOAK_HTTP_PORT;

    @JsonPropertyDescription("The used HTTPS port.")
    private Integer httpsPort = Constants.KEYCLOAK_HTTPS_PORT;

    @JsonPropertyDescription("Annotations to be appended to the Service object")
    Map<String, String> annotations;

    @JsonPropertyDescription("Labels to be appended to the Service object")
    Map<String, String> labels;

    public String getTlsSecret() {
        return tlsSecret;
    }

    public void setTlsSecret(String tlsSecret) {
        this.tlsSecret = tlsSecret;
    }

    public Boolean getHttpEnabled() {
        return httpEnabled;
    }

    public void setHttpEnabled(Boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }


    public static int httpPort(Keycloak keycloak) {
        return httpSpec(keycloak)
                .map(HttpSpec::getHttpPort)
                .orElse(Constants.KEYCLOAK_HTTP_PORT);
    }

    public static int httpsPort(Keycloak keycloak) {
        return httpSpec(keycloak)
                .map(HttpSpec::getHttpsPort)
                .orElse(Constants.KEYCLOAK_HTTPS_PORT);
    }

    private static Optional<HttpSpec> httpSpec(Keycloak keycloak) {
        return CRDUtils.keycloakSpecOf(keycloak)
                .map(KeycloakSpec::getHttpSpec);
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }
    
}
