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

package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class TracingSpec {

    @JsonPropertyDescription("Enables the OpenTelemetry tracing.")
    private Boolean enabled;

    @JsonPropertyDescription("OpenTelemetry endpoint to connect to.")
    private String endpoint;

    @JsonPropertyDescription("OpenTelemetry service name. Takes precedence over 'service.name' defined in the 'resourceAttributes' map.")
    private String serviceName;

    @JsonPropertyDescription("OpenTelemetry protocol used for the telemetry data (default 'grpc'). For more information, check the Tracing guide.")
    private String protocol;

    @JsonPropertyDescription("OpenTelemetry sampler to use for tracing (default 'traceidratio'). For more information, check the Tracing guide.")
    private String samplerType;

    @JsonPropertyDescription("OpenTelemetry sampler ratio. Probability that a span will be sampled. Expected double value in interval [0,1].")
    private Double samplerRatio;

    @JsonPropertyDescription("OpenTelemetry compression method used to compress payloads. If unset, compression is disabled. Possible values are: gzip, none.")
    private String compression;

    @JsonPropertyDescription("OpenTelemetry resource attributes present in the exported trace to characterize the telemetry producer.")
    private Map<String, String> resourceAttributes;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSamplerType() {
        return samplerType;
    }

    public void setSamplerType(String samplerType) {
        this.samplerType = samplerType;
    }

    public Double getSamplerRatio() {
        return samplerRatio;
    }

    public void setSamplerRatio(Double samplerRatio) {
        this.samplerRatio = samplerRatio;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public Map<String, String> getResourceAttributes() {
        if (resourceAttributes == null) {
            resourceAttributes = new LinkedHashMap<>();
        }
        return resourceAttributes;
    }

    // resource attributes in format key=val delimited by comma
    @JsonIgnore
    public String getResourceAttributesString() {
        return convertTracingAttributesToString(getResourceAttributes());
    }

    public void setResourceAttributes(Map<String, String> resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
    }

    /**
     * Convert resource attributes in format key=val delimited by comma to string
     */
    public static String convertTracingAttributesToString(Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .map(attr -> String.format("%s=%s", attr.getKey(), attr.getValue()))
                .collect(Collectors.joining(","));
    }
}
