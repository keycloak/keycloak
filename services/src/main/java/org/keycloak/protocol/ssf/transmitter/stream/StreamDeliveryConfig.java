package org.keycloak.protocol.ssf.transmitter.stream;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the delivery configuration for a stream.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamDeliveryConfig {

    @JsonProperty("method")
    private String method;

    @JsonProperty("endpoint_url")
    private String endpointUrl;

    @JsonProperty("authorization_header")
    private String authorizationHeader;

    @JsonProperty("additional_parameters")
    private Map<String, Object> additionalParameters;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(Map<String, Object> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }
}
