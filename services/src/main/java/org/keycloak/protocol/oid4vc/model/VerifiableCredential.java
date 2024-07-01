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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pojo to represent a VerifiableCredential for internal handling
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifiableCredential {

    @JsonProperty("@context")
    private List<String> context;
    private List<String> type = new ArrayList<>();

    /**
     * The value of the issuer property MUST be either a URL, or an object containing an id property whose value
     * is a URL; in either case, the issuer selects this URL to identify itself in a globally unambiguous way. It is
     * RECOMMENDED that the URL be one which, if dereferenced, results in a controller document, as defined in
     * [VC-DATA-INTEGRITY] or [VC-JOSE-COSE], about the issuer that can be used to verify the information expressed in
     * the credential.
     */
    private Object issuer;
    private Date issuanceDate;
    private URI id;
    private Date expirationDate;
    private CredentialSubject credentialSubject = new CredentialSubject();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public VerifiableCredential setAdditionalProperties(String name, Object property) {
        additionalProperties.put(name, property);
        return this;
    }

    public List<String> getContext() {
        return context;
    }

    public VerifiableCredential setContext(List<String> context) {
        this.context = context;
        return this;
    }

    public List<String> getType() {
        return type;
    }

    public VerifiableCredential setType(List<String> type) {
        this.type = type;
        return this;
    }

    public Object getIssuer() {
        return issuer;
    }

    public VerifiableCredential setIssuer(URI issuer) {
        this.issuer = issuer;
        return this;
    }

    public VerifiableCredential setIssuer(Map<String, String> issuer) {
        this.issuer = issuer;
        Optional.ofNullable(issuer).ifPresent(map -> {
            String id = Optional.ofNullable(map.get("id"))
                                .orElseThrow(() -> new IllegalArgumentException("id is a required field for issuer"));
            try
            {
                // id must be a URL: https://www.w3.org/TR/vc-data-model-2.0/#issuer
                new URI(id);
            }
            catch (URISyntaxException e)
            {
                throw new IllegalStateException("id must be a valid URI", e);
            }
        });
        return this;
    }

    public Date getIssuanceDate() {
        return issuanceDate;
    }

    public VerifiableCredential setIssuanceDate(Date issuanceDate) {
        this.issuanceDate = issuanceDate;
        return this;
    }

    public URI getId() {
        return id;
    }

    public VerifiableCredential setId(URI id) {
        this.id = id;
        return this;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public VerifiableCredential setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public CredentialSubject getCredentialSubject() {
        return credentialSubject;
    }

    public VerifiableCredential setCredentialSubject(CredentialSubject credentialSubject) {
        this.credentialSubject = credentialSubject;
        return this;
    }

    public VerifiableCredential setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }
}

