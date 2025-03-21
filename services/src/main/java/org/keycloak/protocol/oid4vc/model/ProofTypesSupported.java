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
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Objects;

/**
 * See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-proof-types
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProofTypesSupported {
    @JsonProperty("jwt")
    private ProofTypeJWT jwt;
    @JsonProperty("cwt")
    private ProofTypeCWT cwt;

    @JsonProperty("ldp_vp")
    private ProofTypeLdpVp ldpVp;

    public ProofTypeJWT getJwt() {
        return jwt;
    }

    public ProofTypesSupported setJwt(ProofTypeJWT jwt) {
        this.jwt = jwt;
        return this;
    }

    public ProofTypeCWT getCwt() {
        return cwt;
    }

    public ProofTypesSupported setCwt(ProofTypeCWT cwt) {
        this.cwt = cwt;
        return this;
    }

    public ProofTypeLdpVp getLdpVp() {
        return ldpVp;
    }

    public ProofTypesSupported setLdpVp(ProofTypeLdpVp ldpVp) {
        this.ldpVp = ldpVp;
        return this;
    }

    public String toJsonString(){
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ProofTypesSupported fromJsonString(String jsonString){
        try {
            return JsonSerialization.readValue(jsonString, ProofTypesSupported.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProofTypesSupported that = (ProofTypesSupported) o;
        return Objects.equals(jwt, that.jwt) && Objects.equals(cwt, that.cwt) && Objects.equals(ldpVp, that.ldpVp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jwt, cwt, ldpVp);
    }
}
