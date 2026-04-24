/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vp.model;

import jakarta.ws.rs.FormParam;

import org.keycloak.protocol.oid4vp.OID4VPConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OID4VPDirectPostRequest {

    @FormParam(OID4VPConstants.VP_TOKEN)
    @JsonProperty(OID4VPConstants.VP_TOKEN)
    private String vpToken;

    @FormParam(OID4VPConstants.ID_TOKEN)
    @JsonProperty(OID4VPConstants.ID_TOKEN)
    private String idToken;

    @FormParam(OID4VPConstants.STATE)
    @JsonProperty(OID4VPConstants.STATE)
    private String state;

    @FormParam(OID4VPConstants.ERROR)
    @JsonProperty(OID4VPConstants.ERROR)
    private String error;

    @FormParam(OID4VPConstants.ERROR_DESCRIPTION)
    @JsonProperty(OID4VPConstants.ERROR_DESCRIPTION)
    private String errorDescription;

    public String getVpToken() {
        return vpToken;
    }

    public OID4VPDirectPostRequest setVpToken(String vpToken) {
        this.vpToken = vpToken;
        return this;
    }

    public String getIdToken() {
        return idToken;
    }

    public OID4VPDirectPostRequest setIdToken(String idToken) {
        this.idToken = idToken;
        return this;
    }

    public String getState() {
        return state;
    }

    public OID4VPDirectPostRequest setState(String state) {
        this.state = state;
        return this;
    }

    public String getError() {
        return error;
    }

    public OID4VPDirectPostRequest setError(String error) {
        this.error = error;
        return this;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public OID4VPDirectPostRequest setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
        return this;
    }

    @JsonIgnore
    public boolean isErrorResponse() {
        return error != null;
    }

    @JsonIgnore
    public boolean hasVpToken() {
        return vpToken != null;
    }

    @JsonIgnore
    public boolean isValid() {
        return state != null && (isErrorResponse() || hasVpToken());
    }
}
