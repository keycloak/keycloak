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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialsOffer {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    //Either the id of a credential, offered in the issuer metadata or a supported credential object
    private List<Object> credentials;

    private PreAuthorizedGrant grants;

    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    public CredentialsOffer setCredentialIssuer(String credentialIssuer) {
        this.credentialIssuer = credentialIssuer;
        return this;
    }

    public List<Object> getCredentials() {
        return credentials;
    }

    public CredentialsOffer setCredentials(List<Object> credentials) {
        this.credentials = credentials;
        return this;
    }

    public PreAuthorizedGrant getGrants() {
        return grants;
    }

    public CredentialsOffer setGrants(PreAuthorizedGrant grants) {
        this.grants = grants;
        return this;
    }
}