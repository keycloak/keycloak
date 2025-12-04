/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IDTokenRequest {

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("redirect_uri")
    private String redirectUri;

    @JsonProperty("response_type")
    private String responseType;

    @JsonProperty("request")
    private String request;

    @JsonProperty("scope")
    private String scope;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    // =====================================================================================
    // Serialization support
    // =====================================================================================

    public Map<String, List<String>> toRequestParameters() {
        Map<String, List<String>> out = new LinkedHashMap<>();

        add(out, "client_id", clientId);
        add(out, "redirect_uri", redirectUri);
        add(out, "response_type", responseType);
        add(out, "request", request);
        add(out, "scope", scope);

        return out;
    }

    public String toRequestUrl(String endpointUri) {
        Map<String, List<String>> params = toRequestParameters();
        String query = params.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(v ->
                        e.getKey() + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        return endpointUri + "?" + query;
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void add(Map<String, List<String>> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, List.of(value));
        }
    }
}
