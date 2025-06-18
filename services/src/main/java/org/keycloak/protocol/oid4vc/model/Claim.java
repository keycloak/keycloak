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
package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Holding metadata on a claim of verifiable credential.
 * <p>
 * See: <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.2.2">openid-4-verifiable-credential-issuance-1_0.html#appendix-A.2.2</a>
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Claim {
    @JsonProperty("mandatory")
    private Boolean mandatory;
    @JsonProperty("value_type")
    private String valueType;
    @JsonProperty("display")
    private List<ClaimDisplay> display;

    public Boolean getMandatory() {
        return mandatory;
    }

    public Claim setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
        return this;
    }

    public String getValueType() {
        return valueType;
    }

    public Claim setValueType(String valueType) {
        this.valueType = valueType;
        return this;
    }

    public List<ClaimDisplay> getDisplay() {
        return display;
    }

    public Claim setDisplay(List<ClaimDisplay> display) {
        this.display = display;
        return this;
    }
}
