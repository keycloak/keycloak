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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a claims description object as used in authorization details.
 * A claims description object defines the requirements for the claims that the Wallet
 * requests to be included in the Credential.
 * 
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class ClaimsDescription {

    @JsonProperty("path")
    private List<Object> path;

    @JsonProperty("mandatory")
    private Boolean mandatory;

    public ClaimsDescription() {
    }

    public ClaimsDescription(List<Object> path, Boolean mandatory) {
        this.path = path;
        this.mandatory = mandatory;
    }

    public List<Object> getPath() {
        return path;
    }

    public void setPath(List<Object> path) {
        this.path = path;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Returns the mandatory flag, defaulting to false if not set.
     */
    public boolean isMandatory() {
        return mandatory != null ? mandatory : false;
    }
}
