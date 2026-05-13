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
package org.keycloak.protocol.oid4vc.model.presentation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DcqlCredentialQuery {

    @JsonProperty("id")
    private String id;

    @JsonProperty("format")
    private String format;

    @JsonProperty("meta")
    private DcqlCredentialMeta meta;

    @JsonProperty("claims")
    private List<DcqlClaimQuery> claims;

    public String getId() {
        return id;
    }

    public DcqlCredentialQuery setId(String id) {
        this.id = id;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public DcqlCredentialQuery setFormat(String format) {
        this.format = format;
        return this;
    }

    public DcqlCredentialMeta getMeta() {
        return meta;
    }

    public DcqlCredentialQuery setMeta(DcqlCredentialMeta meta) {
        this.meta = meta;
        return this;
    }

    public List<DcqlClaimQuery> getClaims() {
        return claims;
    }

    public DcqlCredentialQuery setClaims(List<DcqlClaimQuery> claims) {
        this.claims = claims;
        return this;
    }
}
