package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class TelemetrySpec {

    @JsonPropertyDescription("OpenTelemetry endpoint to connect to.")
    private String endpoint;

    @JsonPropertyDescription("OpenTelemetry service name. Takes precedence over 'service.name' defined in the 'resourceAttributes' map.")
    private String serviceName;

    @JsonPropertyDescription("OpenTelemetry protocol used for the telemetry data (default 'grpc'). For more information, check the OpenTelemetry guide.")
    private String protocol;

    @JsonPropertyDescription("OpenTelemetry resource attributes present in the exported telemetry data to characterize the telemetry producer.")
    private Map<String, String> resourceAttributes;

    public Map<String, String> getResourceAttributes() {
        if (resourceAttributes == null) {
            resourceAttributes = new LinkedHashMap<>();
        }
        return resourceAttributes;
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

    @JsonIgnore
    public String getResourceAttributesString() {
        return convertResourceAttributesToString(getResourceAttributes());
    }

    public void setResourceAttributes(Map<String, String> resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
    }

    /**
     * Convert resource attributes in format key=val delimited by comma to string
     */
    public static String convertResourceAttributesToString(Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .map(attr -> String.format("%s=%s", attr.getKey(), attr.getValue()))
                .collect(Collectors.joining(","));
    }
}
